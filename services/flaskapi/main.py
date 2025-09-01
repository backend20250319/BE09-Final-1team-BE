# services/flaskapi/main.py — launcher only
from pathlib import Path
import os, sys, argparse

# .env가 있으면 로드(없어도 무시)
try:
    from dotenv import load_dotenv  # type: ignore
    load_dotenv()
except Exception:
    pass

# 정식 앱 팩토리(app:create_app) 사용
from app import create_app as factory

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / ".data"
DATA_DIR.mkdir(parents=True, exist_ok=True)  # sqlite 저장 폴더 보장

def _env(name: str, default: str | None = None) -> str | None:
    v = os.getenv(name)
    if v is None:
        return default
    v = v.strip().strip('"').strip("'")  # 따옴표/공백 방어
    return v if v else default

def _coerce_jdbc(url: str) -> str:
    # jdbc:mysql://...  →  mysql+pymysql://...
    # jdbc:postgresql://... → postgresql+psycopg2://...
    if not url.startswith("jdbc:"):
        return url
    rest = url.split("jdbc:", 1)[1]
    if rest.startswith("mysql://"):
        return "mysql+pymysql://" + rest.split("mysql://", 1)[1]
    if rest.startswith("postgresql://"):
        return "postgresql+psycopg2://" + rest.split("postgresql://", 1)[1]
    return url  # 알 수 없는 스킴은 그대로

def _ensure_default_db_url():
    # 우선순위: FLASK_DATABASE_URL > DATABASE_URL > (기본) .data/flaskapi.sqlite3
    db = _env("FLASK_DATABASE_URL") or _env("DATABASE_URL")
    if db:
        os.environ["FLASK_DATABASE_URL"] = _coerce_jdbc(db)
        return
    default_sqlite = f"sqlite:///{(DATA_DIR / 'flaskapi.sqlite3').as_posix()}"
    os.environ["FLASK_DATABASE_URL"] = default_sqlite

def _port_from_env() -> int:
    p = _env("PORT", "5000")
    try:
        return int(p) if p else 5000
    except ValueError:
        return 5000

def _hint_requirements():
    try:
        import flask  # noqa: F401
    except Exception:
        print(
            "[hint] Flask가 설치되어 있지 않습니다. 다음 명령으로 설치하세요:\n"
            "       pip install -r requirements.txt",
            file=sys.stderr,
        )

if __name__ == "__main__":
    _hint_requirements()
    _ensure_default_db_url()

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=_port_from_env())
    args = parser.parse_args()

    app = factory()  # 블루프린트/DB 설정은 팩토리 안에서 처리
    env = _env("APP_ENV", "local")
    db_url = _env("FLASK_DATABASE_URL")

    print(f"[flaskapi] ENV={env} PORT={args.port}")
    print(f"[flaskapi] DATABASE_URL={'(set)' if db_url else '(default)'}")
    print(f"[flaskapi] base={BASE_DIR}")

    app.run(host="0.0.0.0", port=args.port, debug=_env("FLASK_ENV") == "development")
