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
    """
    if not uri.startswith("sqlite"):
        return uri  # MySQL/Postgres 등은 그대로

    if uri.startswith("sqlite:///:memory:"):
        return uri

    if uri.startswith("sqlite:////"):
        raw = uri.replace("sqlite:////", "", 1)
        p = Path("/" + raw)
        return f"sqlite:////{p.as_posix().lstrip('/')}"

    if uri.startswith("sqlite:///"):
        rel = uri.replace("sqlite:///", "", 1)
        inst_dir = Path(app.instance_path)
        inst_dir.mkdir(parents=True, exist_ok=True)
        abs_path = (inst_dir / rel).resolve()
        return f"sqlite:///{abs_path.as_posix()}"

    db_file = Path(app.instance_path) / "summary.db"
    return f"sqlite:///{db_file.as_posix()}"


def create_app() -> Flask:
    app = Flask(__name__, instance_relative_config=True)

    Path(app.instance_path).mkdir(parents=True, exist_ok=True)

    env = os.getenv("FLASK_ENV", "").lower()
    try:
        if env == "production":
            from .config import ProdConfig as ActiveConfig
        else:
            from .config import DevConfig as ActiveConfig
        app.config.from_object(ActiveConfig)
    except Exception:
        app.config.setdefault("SQLALCHEMY_DATABASE_URI", "sqlite:///flaskapi.db")
        app.config.setdefault("SQLALCHEMY_TRACK_MODIFICATIONS", False)

    if env != "production":
        app.config.setdefault("SQLALCHEMY_ECHO", True)

    db_uri = (
        os.getenv("FLASK_DATABASE_URL")
        or os.getenv("DATABASE_URL")
        or app.config.get("SQLALCHEMY_DATABASE_URI")
    )
    if not db_uri:
        db_uri = "sqlite:///flaskapi.db"

    db_uri = _normalize_sqlite_uri(app, db_uri)
    app.config["SQLALCHEMY_DATABASE_URI"] = db_uri
    app.config.setdefault("SQLALCHEMY_TRACK_MODIFICATIONS", False)

    # ✅ 풀 옵션 세팅
    engine_opts = dict(app.config.get("SQLALCHEMY_ENGINE_OPTIONS", {}) or {})

    if db_uri.startswith("mysql") or db_uri.startswith("postgresql"):
        engine_opts.setdefault("pool_size", 2)        # 기본 커넥션 2개
        engine_opts.setdefault("max_overflow", 0)     # 추가 연결 막기
        engine_opts.setdefault("pool_timeout", 30)    # 대기 시간
        engine_opts.setdefault("pool_recycle", 1800)  # 30분마다 재연결

    if db_uri.startswith("sqlite"):
        connect_args = dict(engine_opts.get("connect_args", {}) or {})
        connect_args.setdefault("check_same_thread", False)
        engine_opts["connect_args"] = connect_args

    engine_opts.setdefault("pool_pre_ping", True)
    app.config["SQLALCHEMY_ENGINE_OPTIONS"] = engine_opts

    # ✅ DB init & create_all (dev 전용)
    db.init_app(app)
    with app.app_context():
        try:
            from . import models
            if env != "production":   # 운영에서는 create_all 안 함
                db.create_all()
        except OperationalError as e:
            print("[DB][OperationalError]", e)
            print("[DB][Hint] 상위 폴더/권한/URI 형식을 다시 확인하세요.")
            raise

    try:
        from .routes.summary_route import summary_bp
        app.register_blueprint(summary_bp)
    except Exception:
        pass

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

    print("[BOOT] CWD:", os.getcwd())
    print("[BOOT] instance_path:", app.instance_path)
    print("[BOOT] SQLALCHEMY_DATABASE_URI:", app.config["SQLALCHEMY_DATABASE_URI"])

    return app
