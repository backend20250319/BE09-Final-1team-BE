# ORM 모델 정의
from sqlalchemy import text, Index
from sqlalchemy.dialects import mysql
from .extensions import db
from datetime import datetime


# ENUM 정의 (DDL과 동일)
CATEGORY_ENUM = (
    "POLITICS", "ECONOMY", "SOCIETY", "LIFE",
    "INTERNATIONAL", "IT_SCIENCE", "VEHICLE", "TRAVEL_FOOD", "ART"
)
DEDUP_ENUM = ("REPRESENTATIVE", "RELATED", "KEPT")


class News(db.Model):
    __tablename__ = "news"
    __table_args__ = (
        Index("idx_dedup_state", "dedup_state"),
        Index("idx_published_at", "published_at"),
        {"sqlite_autoincrement": True},
    )

    # bigint auto_increment primary key (SQLite에서는 Integer로 대체)
    news_id = db.Column(
        db.BigInteger().with_variant(db.Integer, "sqlite"),
        primary_key=True,
        autoincrement=True,
        nullable=False,
    )

    title = db.Column(db.Text, nullable=False)
    content = db.Column(
        db.Text().with_variant(mysql.MEDIUMTEXT(), "mysql"),
        nullable=False,
    )
    press = db.Column(db.Text, nullable=False)

    # DDL: varchar(100) NULL (datetime 아님)
    published_at = db.Column(db.String(100), nullable=True)

    reporter = db.Column(db.Text, nullable=False)

    dedup_state = db.Column(
        db.Enum(*DEDUP_ENUM, name="dedup_state", native_enum=True),
        nullable=False,
    )

    # datetime(6) NULL
    created_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=True,
    )
    # DB에서 삭제
    # updated_at = db.Column(
    #     db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
    #     nullable=True,
    # )

    # DDL: bit NOT NULL
    trusted = db.Column(
        db.Boolean().with_variant(mysql.BIT(1), "mysql"),
        nullable=False,
        server_default=text("0"),   # MySQL BIT(1) 기본값으로 0
    )

    image_url = db.Column(db.Text, nullable=True)

    # varchar(255) UNIQUE NULL
    oid_aid = db.Column(db.String(255), unique=True, nullable=True)

    # ENUM + 기본값 'POLITICS' NOT NULL
    category_name = db.Column(
        db.Enum(*CATEGORY_ENUM, name="category_name", native_enum=True),
        nullable=False,
        server_default=text("'POLITICS'"),
    )

class NewsSummary(db.Model):
    __tablename__ = "news_summary"

    # MySQL: BIGINT AUTO_INCREMENT
    id = db.Column(db.Integer, primary_key=True)
    news_id = db.Column(db.String(255), nullable=False, index=True)
    summary_type = db.Column(db.String(50), nullable=False)
    lines = db.Column(db.Integer, nullable=False, default=3)
    summary_text = db.Column(db.Text, nullable=False)
    created_at = db.Column(db.DateTime, default=datetime.utcnow, nullable=False)

    __table_args__ = (
        db.UniqueConstraint('news_id', 'summary_type', 'lines',
                            name='uq_news_summary_key'),
    )

    news_id = db.Column(
        db.BigInteger().with_variant(db.Integer, "sqlite"),
        db.ForeignKey("news.news_id"),
        index=True,
        nullable=True
    )

    # DB 컬럼명과 동일 (AIBOT / NEWSLETTER)
    summary_type = db.Column(db.String(50), nullable=False)

    # MEDIUMTEXT on MySQL
    summary_text = db.Column(
        db.Text().with_variant(mysql.MEDIUMTEXT(), "mysql"),
        nullable=False
    )

    # MySQL: datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
    created_at = db.Column(
        db.DateTime().with_variant(mysql.DATETIME(fsp=6), "mysql"),
        nullable=False,
        server_default=text("CURRENT_TIMESTAMP")
    )

    news = db.relationship("News", backref=db.backref("summaries", lazy=True))



