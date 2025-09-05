# routes/summary_route.py
from flask import Blueprint, request, jsonify, current_app, g
from sqlalchemy.exc import SQLAlchemyError
from datetime import datetime
from ..extensions import db
from ..models import News, NewsSummary
from ..prompt_manager import PromptManager
from ..services.summarizer import summarize

# /summary로 시작하는 모든 요약 관련 엔드포인트
summary_bp = Blueprint("summary", __name__, url_prefix="/summary")

# 안전장치: 한 번에 돌릴 프롬프트 개수 상한
MAX_PROMPTS = 10

def _parse_bool(data: dict, key: str, default=False) -> bool:
    """data[key]를 유연하게 불리언으로 파싱."""
    val = data.get(key, default)
    if isinstance(val, bool):
        return val
    return str(val).strip().lower() in ("1", "true", "yes", "on")

def _parse_lines(data: dict, default=3, min_=1, max_=10) -> int:
    """lines 파라미터 정규화(최소/최대 범위 강제)."""
    try:
        v = int(data.get("lines", default))
        return max(min_, min(v, max_))
    except Exception:
        return default

def _aggregate_summaries(items, lines: int) -> str:
    """
    여러 프롬프트로 생성한 부분 요약들을 하나로 통합.
    items: [{"id": "TYPE:key", "summary": "..."}]
    """
    merged = "\n\n".join(
        f"[{it['id']}]\n{it['summary']}" for it in items if it.get("summary")
    )
    meta_prompt = (
        "다음 여러 요약본을 통합하라.\n"
        "- 중복/반복 제거\n"
        "- 서로 상충되면 보수적으로 기술(확정어 피함)\n"
        "- 숫자/고유명사 보존\n"
        f"- 최종 출력은 {lines}줄, 각 줄은 한 문장\n"
    )
    # summarize(text, prompt) 시그니처 가정
    return summarize(merged, meta_prompt)

def _resolve_prompt_and_type(data, news) -> tuple[str, str, int]:
    """
    요약 타입/프롬프트/줄수 확정.
    우선순위: (요청 type/summary_type) → DB news.category_name → DEFAULT
    단, 요청이 'DEFAULT'로 정규화되면 '미지정'으로 간주하여 DB 카테고리 사용.
    """
    # 1) 요청값 수집
    type_in = (data.get("type") or "").strip()
    stype_in = (data.get("summary_type") or "").strip()
    req_type_raw = type_in or stype_in

    # 2) 'DEFAULT'로 정규화되면 요청값은 무시(=미지정)
    req_type_norm = PromptManager._canon_type(req_type_raw) if req_type_raw else None
    req_type_effective = None if req_type_norm == PromptManager.DEFAULT_TYPE else req_type_raw

    # 3) DB 카테고리
    db_cat = getattr(news, "category_name", None) if news else None  # ← 컬럼명 정확히!
    type_candidate = req_type_effective or db_cat

    # 4) 줄수/프롬프트 확정
    lines = _parse_lines(data, default=3)
    resolved_type, prompt_text = PromptManager.get_effective(
        prompt_or_id=data.get("prompt"),
        type_candidate=type_candidate,
        lines=lines,
    )

    # 5) 로깅으로 추적 쉽게
    current_app.logger.info(
        f"[{g.rid}] type_in={type_in} summary_type_in={stype_in} db_cat={db_cat} -> resolved={resolved_type}"
    )
    return resolved_type, prompt_text, lines


@summary_bp.before_app_request
def _bind_request_id():
    """게이트웨이/서비스에서 주입한 X-Request-ID를 로깅 컨텍스트에 바인딩."""
    g.rid = request.headers.get("X-Request-ID", "-")

@summary_bp.route("/healthz", methods=["GET"])
def healthz():
    """헬스 체크용 심플 엔드포인트."""
    return jsonify({"status": "ok"}), 200

@summary_bp.route("/", methods=["POST"])
@summary_bp.route("", methods=["POST"])
def create_summary():
    """
    요청(JSON):
      {
        "news_id": 123,          # 또는 "text": "...", 둘 중 하나 필요
        "type": "DEFAULT",         # 선택(기본 DEFAULT)
        "lines": 3,              # 선택(기본 3)
        "prompt": "...",         # 선택
        "ensemble": false        # 선택(기본 false; true면 여러 프롬프트 통합)
      }

    응답(JSON, Spring DTO와 호환):
      {
        "newsId": 123,
        "type": "DEFAULT",
        "lines": 3,
        "summary": "...",
        "cached": true|false,
        "createdAt": "2025-08-20T01:23:45Z"
      }
    """
    # Content-Type 미지정 요청 방지: force 대신 silent 사용
    data = request.get_json(silent=True) or {}

    news_id = data.get("news_id") or data.get('newsId')
    text = data.get("text")

    # 원문 확보(우선순위: text 직접 제공 > DB 조회)
    news = None
    if not (isinstance(text, str) and text.strip()):
        if not news_id:
            return jsonify({"error": "text 또는 news_id가 필요합니다."}), 400
        news = News.query.get(news_id)
        if not news:
            return jsonify({"error": "뉴스가 없습니다.", "newsId": news_id}), 404
        text = getattr(news, "content", None) or getattr(news, "body", None)
        if not text or not str(text).strip():
            return jsonify({"error": "뉴스 본문이 비어 있습니다.", "newsId": news_id}), 400

    # 단일 vs 종합(ensemble) 모드 선택
    # 기본은 단일 모드: 상세페이지 클릭 시 성능/비용 절감
    ensemble = _parse_bool(data, "ensemble", default=False)

    # 타입/프롬프트/줄수 확정
    resolved_type, prompt_text, lines = _resolve_prompt_and_type(data, news)

    # 캐시 조회: 단일 모드에만 적용(ensemble은 매번 달라질 수 있음)
    # ⚠️ 캐시 키에는 반드시 lines 포함
    cached_row = None
    if not ensemble and news_id:
        cached_row = (
            NewsSummary.query
            .filter_by(news_id=news_id, summary_type=resolved_type, lines=lines)
            .first()
        )
        if cached_row:
            current_app.logger.info(f"[{g.rid}] cache hit news_id={news_id} type={resolved_type} lines={lines}")
            return jsonify({
                "newsId": cached_row.news_id,
                "type": cached_row.summary_type,
                "lines": cached_row.lines,
                "summary": cached_row.summary_text,
                "cached": True,
                "createdAt": (cached_row.created_at or datetime.utcnow()).replace(microsecond=0).isoformat() + "Z"
            }), 200

    # 요약 생성
    try:
        if ensemble:
            # 해당 타입의 모든 프롬프트를 실행 → 통합
            items = PromptManager.get_many_by_types(
                [resolved_type], include_default=True, lines=lines
            )[:MAX_PROMPTS]
            partials = []
            for it in items:
                try:
                    s = summarize(text, it["prompt"])
                except Exception as e:
                    # 한 개 실패해도 전체는 계속
                    current_app.logger.warning(f"[{g.rid}] summarize partial failed {it['id']}: {e}")
                    s = ""
                partials.append({"id": it["id"], "summary": s})
            summary_text = _aggregate_summaries(partials, lines)
        else:
            summary_text = summarize(text, prompt_text)

    except Exception as e:
        current_app.logger.exception(f"[{g.rid}] summarize failed (/summary)")
        return jsonify({"error": "SUMMARIZE_FAILED", "detail": str(e)}), 500

    # 저장: (news_id, summary_type, lines) 기준 upsert
    saved_row = None
    if news_id:
        try:
            saved_row = (
                NewsSummary.query
                .filter_by(news_id=news_id, summary_type=resolved_type, lines=lines)
                .first()
            )
            now = datetime.utcnow()
            if saved_row:
                # ensemble은 기본 덮어쓰기(필요 시 정책 분기 가능)
                saved_row.summary_text = summary_text
                saved_row.created_at = now
            else:
                saved_row = NewsSummary(
                    news_id=news_id,
                    summary_type=resolved_type,
                    lines=lines,
                    summary_text=summary_text,
                    created_at=now
                )
                db.session.add(saved_row)
            db.session.commit()
        except SQLAlchemyError as e:
            current_app.logger.exception(f"[{g.rid}] DB commit failed (/summary)")
            db.session.rollback()
            return jsonify({"error": "DB_ERROR", "detail": str(e)}), 500

    # 최종 응답(표준 스키마, 200 통일)
    created_at = (
            (saved_row.created_at if saved_row else datetime.utcnow())
            .replace(microsecond=0).isoformat() + "Z"
    )
    return jsonify({
        "newsId": news_id,
        "type": resolved_type,
        "lines": lines,
        "summary": summary_text,
        "cached": False,
        "createdAt": created_at
    }), 200
