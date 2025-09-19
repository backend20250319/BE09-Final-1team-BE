# app/__init__.py
from flask import Flask, request, jsonify, current_app
import os
from pathlib import Path
from datetime import datetime

from dotenv import load_dotenv
from sqlalchemy.exc import OperationalError
from sqlalchemy import text

# 핵심: 오직 extensions.db만 초기화에 사용 (models 내부도 동일 기준)
from .extensions import db

load_dotenv()  # .env 우선 적용


def _normalize_sqlite_uri(app: Flask, uri: str) -> str:
    """
    SQLite URI를 절대경로/정상형식으로 정규화.
    - sqlite:///:memory:        -> 그대로
    - sqlite:////C:/abs.db      -> 절대경로 형태는 그대로(슬래시만 POSIX 보정)
    - sqlite:///relative.db     -> app.instance_path 기준의 절대경로로 변환
    - 그 외 이상치               -> instance/summary.db 로 폴백
    """
    if not uri.startswith("sqlite"):
        return uri  # MySQL/Postgres 등은 그대로

    if uri.startswith("sqlite:///:memory:"):
        return uri

    # 이미 절대경로 형태 (리눅스 / 윈도우 드라이브 레터 포함)면 보존
    if uri.startswith("sqlite:////"):
        raw = uri.replace("sqlite:////", "", 1)
        # 앞에 슬래시 하나 붙여 Path가 루트부터 해석하도록
        p = Path("/" + raw)
        return f"sqlite:////{p.as_posix().lstrip('/')}"

    # sqlite:///something.db  → instance_path 기준으로 절대화
    if uri.startswith("sqlite:///"):
        rel = uri.replace("sqlite:///", "", 1)
        inst_dir = Path(app.instance_path)
        inst_dir.mkdir(parents=True, exist_ok=True)
        abs_path = (inst_dir / rel).resolve()
        return f"sqlite:///{abs_path.as_posix()}"

    # 그 외 변형(매우 드묾): instance/summary.db 로 폴백
    db_file = Path(app.instance_path) / "summary.db"
    return f"sqlite:///{db_file.as_posix()}"


def create_app() -> Flask:
    # instance_relative_config=True → ./instance 폴더 자동 기준
    app = Flask(__name__, instance_relative_config=True)

    # instance 폴더 보장
    Path(app.instance_path).mkdir(parents=True, exist_ok=True)

    # 1) 구성 로드 (없으면 DevConfig 기본)
    env = os.getenv("FLASK_ENV", "").lower()
    try:
        if env == "production":
            from .config import ProdConfig as ActiveConfig
        else:
            from .config import DevConfig as ActiveConfig
        app.config.from_object(ActiveConfig)
    except Exception:
        # config 모듈이 없더라도 최소 동작 보장
        app.config.setdefault("SQLALCHEMY_DATABASE_URI", "sqlite:///flaskapi.db")
        app.config.setdefault("SQLALCHEMY_TRACK_MODIFICATIONS", False)

    # 개발 편의: SQL 로그
    if env != "production":
        app.config.setdefault("SQLALCHEMY_ECHO", True)

    # 2) DB URI 확정: env > config > 기본값
    db_uri = (
                os.getenv("FLASK_DATABASE_URL")              # ✅ 먼저 이걸 봅니다
                or os.getenv("DATABASE_URL")
                or app.config.get("SQLALCHEMY_DATABASE_URI")
    )
    if not db_uri:
        db_uri = "sqlite:///flaskapi.db"

    # 3) SQLite 정규화 + 엔진 옵션
    db_uri = _normalize_sqlite_uri(app, db_uri)
    app.config["SQLALCHEMY_DATABASE_URI"] = db_uri
    app.config.setdefault("SQLALCHEMY_TRACK_MODIFICATIONS", False)

    engine_opts = dict(app.config.get("SQLALCHEMY_ENGINE_OPTIONS", {}) or {})
    if db_uri.startswith("sqlite"):
        connect_args = dict(engine_opts.get("connect_args", {}) or {})
        connect_args.setdefault("check_same_thread", False)  # dev 서버 편의
        engine_opts["connect_args"] = connect_args
    engine_opts.setdefault("pool_pre_ping", True)
    app.config["SQLALCHEMY_ENGINE_OPTIONS"] = engine_opts

    # 4) DB init & 테이블 생성
    db.init_app(app)
    with app.app_context():
        try:
            # 모델 정의만 import해서 메타데이터 로드
            from . import models  # (models 안에서도 from .extensions import db 사용)
            db.create_all()
        except OperationalError as e:
            print("[DB][OperationalError]", e)
            print("[DB][Hint] 상위 폴더/권한/URI 형식을 다시 확인하세요.")
            raise

    # 5) 블루프린트 등록 (존재할 경우에만)
    try:
        from .routes.summary_route import summary_bp
        app.register_blueprint(summary_bp)
    except Exception:
        pass  # routes가 아직 없어도 앱은 부팅되도록

    # 6) 헬스/디버그 라우트
    @app.get("/")
    def health():
        return {"ok": True, "ts": datetime.utcnow().isoformat() + "Z"}

    @app.get("/__routes")
    def __routes():
        return {
            "cwd": os.getcwd(),
            "instance_path": app.instance_path,
            "db_uri": app.config.get("SQLALCHEMY_DATABASE_URI"),
            "routes": [
                {"rule": r.rule, "methods": sorted(list(r.methods))}
                for r in app.url_map.iter_rules()
            ],
        }

    # 실제 SQLite 파일 경로 확인(혼선 방지)
    @app.get("/__db")
    def __db():
        try:
            uri = str(db.engine.url)
            info = {"engine_url": uri}
            if uri.startswith("sqlite"):
                rows = db.session.execute(text("PRAGMA database_list")).mappings().all()
                info["sqlite_database_list"] = [dict(r) for r in rows]
            return info
        except Exception as e:
            current_app.logger.exception("__db failed")
            return {"error": str(e)}, 500

    @app.before_request
    def _log_req():
        print(">>", request.method, request.path, request.headers.get("Content-Type"))

    @app.errorhandler(404)
    def _not_found(e):
        if request.path.startswith("/summary"):
            return jsonify({
                "error": "Not Found",
                "path": request.path,
                "hint": "아래 routes에서 /summary 엔드포인트가 있는지 확인하세요.",
                "routes": [r.rule for r in app.url_map.iter_rules()],
            }), 404
        return e, 404

    # 7) 부팅 로그
    print("[BOOT] CWD:", os.getcwd())
    print("[BOOT] instance_path:", app.instance_path)
    print("[BOOT] SQLALCHEMY_DATABASE_URI:", app.config["SQLALCHEMY_DATABASE_URI"])

    return app
