import os
from dotenv import load_dotenv
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent # config.py 위치 기준

load_dotenv() # env파일 불러오기

def strtobool(s: str) -> int:  # Py3.12 대응
    s = s.strip().lower()
    if s in ("y","yes","t","true","on","1"): return 1
    if s in ("n","no","f","false","off","0"): return 0
    raise ValueError(f"invalid truth value: {s}")

class BaseConfig:
    SECRET_KEY = os.getenv("SECRET_KEY", "dev-secret")
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-4.1-mini")

class DevConfig(BaseConfig):
    # instance/summary.db 절대경로로 고정
    _db_path = (BASE_DIR / "instance" / "summary.db")
    _db_uri = f"sqlite:///{_db_path.as_posix()}"  # 윈도우 백슬래시 이슈 회피
    SQLALCHEMY_DATABASE_URI = os.getenv("DATABASE_URL", _db_uri)
    DEBUG = bool(strtobool(os.getenv("DEBUG", "1")))

class ProdConfig(BaseConfig):
    DEBUG = False
    SQLALCHEMY_DATABASE_URI = os.getenv("DATABASE_URL")  # 여기서는 검증만 하지 않음

Config = DevConfig if os.getenv("FLASK_ENV") != "production" else ProdConfig
