# app/prompt_manager.py
from typing import List, Dict, Optional, Tuple

class PromptManager:
    """
    - 뉴스 카테고리 문자열을 프롬프트 타입으로 그대로 사용.
    - 등록되지 않은 타입(카테고리)이면 DEFAULT로 치환.
    - get() : 프롬프트 텍스트만 반환
    - get_effective(): (실제 사용된 타입, 프롬프트 텍스트) 튜플 반환 → DB 저장 시 resolved_type을 그대로 쓰면 됨.
    """

    # ── Summarization Config ─────────────────────────────────────────────────────────
    LINES = 3  # 기본 요약 줄 수 (※ 줄 수 표기는 default에만 적용)

    # ── Generation Settings ─────────────────────────────────────────────────────────
    TEMPERATURE = 0.2
    # temperature=0.2 의미(샘플링 온도)
    # - 낮을수록 분포가 뾰족 → 결정적/일관적 출력(요약·사실형 작업에 적합)
    # - 높을수록 분포가 평평 → 다양/창의(브레인스토밍·창작에 적합)
    # - 수식: p = softmax(logits / T). T를 줄이면 높은 확률 토큰이 더 자주 선택됩니다.
    # - T≈0이면 greedy에 가깝고, 샘플링을 안 쓰면(beam/greedy) temperature 영향이 거의 없습니다.
    # - top_p/top_k와의 차이: temperature는 남겨둔 후보 ‘사이’의 확률 기울기, top_p/top_k는 후보의 범위.
    # - 권장 범위: 요약/코드 0.1~0.3, 창작 0.7~1.0. 본 세팅(0.2)은 사실 위주 요약에 최적화.

    GEN_PARAMS = {
        "temperature": TEMPERATURE,  # ↓ 낮을수록 확률 분포가 뾰족 → 일관적 결과(요약에 적합)

        "top_p": 1.0,                # nucleus sampling 상한(0~1). 1.0이면 사실상 '끄기'(전체 분포 사용).
        # 0.8~0.95로 낮추면 상위 토큰만 샘플링 → 산만함 감소, 보수적 출력.
        # temperature와 top_p를 동시에 크게 조정하면 예측 불가성 ↑ → 보통 둘 중 하나만 조정.

        # "max_tokens": 400,         # 생성(출력) 가능한 최대 토큰 수 상한.
        # 요약 길이 관리용. 너무 낮으면 문장 도중 절단될 수 있음.
        # (대략 3줄 요약: 120~200 토큰 안팎으로 시작해 보며 조정)

        # "presence_penalty": 0.0,   # 이미 등장한 '어떤' 토큰이라도 다시 쓰는 경향을 전반적으로 억제(+이면 억제).
        # 새 주제/어휘로 확장 유도. 범위 보통 [-2.0, 2.0].
        # 사실 요약은 0.0~0.2 권장(높이면 원문 용어 유지가 흔들릴 수 있음).

        # "frequency_penalty": 0.0,  # 같은 토큰을 '반복해서' 쓸수록 더 강하게 패널티(+이면 억제).
        # 중복 문구/어휘 반복을 줄이는 데 유용. 보통 0.0~0.3로 미세 조정.
        # 너무 높으면 필요한 핵심 용어도 과하게 바뀔 수 있음.
    }

    # 1) 프롬프트 세트
    PROMPTS: Dict[str, Dict[str, str]] = {
        "DEFAULT": {
            "default": "이 기사를 {lines}줄로 요약해줘.",
            "p1": "시간 순서대로 사건 전개를 정리해 {lines}줄로 요약. 한 줄=핵심 사실 1개. 추측·과장 금지.",
            "p2": "원문 구조와 핵심 근거를 남기고 {lines}줄로 요약. 숫자·날짜·고유명사는 정확히 유지.",
            "p3": "쟁점/반론을 균형 있게 {lines}줄로 요약. 주장↔근거를 분리해 간결히.",
            "p4": "배경은 간단히, 새로 확인된 사실 중심으로 {lines}줄 요약. 과도한 해석 금지.",
            "p5": "이해관계자별 한 줄 정리: 정부·기업·시민/단체 관점에서 {lines}줄 요약.",
            "p6": "후속 일정/영향을 포함해 {lines}줄 요약. 일정·수치·기관명은 정확히.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "POLITICS": {
            "default": "정치 기사 {lines}줄 요약: 정책/법안/발언의 핵심 → 찬반 쟁점·이해관계자 → 향후 일정(표결·발표·협상) 순서로. 수치·기관명은 정확히 유지해.",
            "p1": "쟁점·입장 정리형 — 정치 기사를 {lines}줄로 요약. 한 줄=한 쟁점: 핵심, 입장, 일정(표결·발표 등) 순. 수치·날짜 유지.",
            "p2": "정책/법안 포맷 — ‘정책/법안’ 관련 보도를 {lines}줄로 요약. ① 목적/핵심 ② 대상/쟁점 ③ 일정·주체. 한 줄 한 문장, 수식어 금지.",
            "p3": "정책/법안 포맷(세부) — ‘정책/법안’ 기사를 {lines}줄로 요약. 1) 목적/핵심 2) 대상/쟁점 3) 일정/참여자(표결·발표 등).",
            "p4": "발언·논쟁 브리핑 — 발언/논쟁 중심 이슈를 {lines}줄로 요약. ① 발언/논쟁 내용 ② 반응 ③ 향후 후속조치.",
            "p5": "정책/기관별 포커스 — 주요 부처/기관 중심으로 {lines}줄 요약. 역할·권한·일정 순. 추측·단정 금지.",
            "p6": "시위/정치 이벤트 — 시위·행사·발표 등 일정을 {lines}줄로 요약. 누구/무엇/언제/어디/왜. 한 줄 한 문장, 과장 금지.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "ECONOMY": {
            "default": "경제 기사 {lines}줄 요약: 어떤 지표·정책·이벤트가 무엇에 영향을 줬는지(생활물가·환율·주택·금리 등)와 원인·파급효과를 명확히 써줘.",
            "p1": "지표·시장형 — ‘경제 기사’를 {lines}줄로 요약. 핵심 지표와 방향(↑/↓)·근거·영향을 간결히. 수치·날짜 유지.",
            "p2": "지표/지수 — 경제 기사를 {lines}줄로 요약. (지표/지수) 수치와 전월/전년 대비, 원인, 파급효과.",
            "p3": "투자자 관점 — ‘투자자 관점’에서 {lines}줄 요약. ① 가격/거래 ② 리스크·변동성 ③ 향후 포인트. 과장/단정 금지.",
            "p4": "기업/실적 브리핑 — 기업/실적 뉴스를 {lines}줄로 요약. 매출·영업이익·전망, 변수/리스크, 코멘트 포함.",
            "p5": "정책·제도 — 경제 정책/제도 기사를 {lines}줄로 요약. ① 취지/핵심 ② 대상/영향 ③ 일정/후속조치.",
            "p6": "소비/체감형 — 생활물가·대출·세금 등 체감 이슈를 {lines}줄로 요약. 금액·비율·기간 등 수치 정확히.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "SOCIETY": {
            "default": "사회 기사 {lines}줄 요약: 사건 경과(언제/어디서/무엇) → 피해 규모·당국 조치 → 향후 수사·제도 개선 등 후속 조치를 정리해줘.",
            "p1": "사건사고 — ‘사건/사고’ 기사를 {lines}줄로 요약. ① 사건 경과 ② 피해/원인 ③ 수사/조치.",
            "p2": "재난/안전 — 재난/안전 이슈를 {lines}줄로 요약. 발생 원인, 대응 현황, 추가 위험/예방.",
            "p3": "사회 이슈 — 쟁점/갈등형 이슈를 {lines}줄로 요약. 쟁점, 이해관계자 입장, 법/제도·후속 과제.",
            "p4": "지역/생활 — 지역사회·생활 밀착 주제를 {lines}줄로 요약. 영향 대상, 변화 포인트, 유의사항.",
            "p5": "사법/행정 — 판결/수사/행정처분을 {lines}줄로 요약. 결정 요지, 근거, 파장/후속.",
            "p6": "노동/복지 — 노동·복지 이슈를 {lines}줄로 요약. 제도 변화, 대상/조건, 유의점/사례.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "LIFE": {
            "default": "생활/문화 {lines}줄 요약: 트렌드·제도 변화의 핵심 → 이용자에게 미치는 영향과 유의점 → 관련 일정·가격·이용 방법을 정리해줘.",
            "p1": "트렌드/가이드 — 트렌드 기사를 {lines}줄로 요약. 유행 근거(지표/사례) → 소비자/이용자 영향 → 한 줄 한 문장 가이드.",
            "p2": "생활/지원 — 생활지원/복지/지침 뉴스를 {lines}줄로 요약. 대상/조건, 신청 방법, 일정/주의.",
            "p3": "소비/지출 — 소비 관련 소식을 {lines}줄로 요약. 가격/조건, 비교 포인트, 실용 정보.",
            "p4": "문화/교육 — 문화·교육 관련 기사를 {lines}줄로 요약. 내용/대상, 일정/접수, 혜택/유의점.",
            "p5": "건강/안전 — 건강/안전 이슈를 {lines}줄로 요약. 주요 내용, 권고/제한, 주의점.",
            "p6": "생활 팁 — 생활 팁을 {lines}줄로 요약. 상황/대상, 핵심 요령, 참고 링크/기관(있으면).",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "INTERNATIONAL": {
            "default": "세계 뉴스 {lines}줄 요약: 국가·기관·합의의 핵심 내용 → 배경과 이해관계 → 제재·협상·군사·경제적 파급효과를 간단히 정리해줘.",
            "p1": "정상회담/외교 — ‘국제/외교 뉴스’를 {lines}줄로 요약. 합의/발표 요지, 일정/참여자, 파장(지역/산업).",
            "p2": "분쟁/제재 — 분쟁/제재 이슈를 {lines}줄로 요약. 조치 내용, 당사자/대상, 파장·추가 변수.",
            "p3": "국제지표/경제 — 글로벌 지표/발표를 {lines}줄로 요약. 수치/변화, 해석/영향, 후속 일정.",
            "p4": "동맹/연합 — 동맹/연합/협력 뉴스를 {lines}줄로 요약. 목적·범위, 이해관계, 전망.",
            "p5": "인권/난민 — 인권/난민 이슈를 {lines}줄로 요약. 사건 요지, 대응/비판, 국제 규범/후속.",
            "p6": "국내 영향 — 해외 이슈의 국내 영향(산업/정책/시장)을 {lines}줄로 요약.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "IT_SCIENCE": {
            "default": "IT/과학 {lines}줄 요약: 기술/연구의 목적·원리·성과 → 성능(정확도/속도 등) → 실제 적용 분야·향후 로드맵을 포함해줘.",
            "p1": "AI/기술 브리핑 — ‘AI/기술’ 기사를 {lines}줄로 요약. 핵심 원리/개선점, 성능지표/비교, 적용 사례.",
            "p2": "제품/서비스 출시 — ‘신제품/서비스’ 뉴스를 {lines}줄로 요약. 핵심 기능, 가격/정책, 경쟁·차별점.",
            "p3": "연구 결과 — 연구 결과를 {lines}줄로 요약. 방법/데이터, 주요 수치, 한계/재현 가능성/다음 단계.",
            "p4": "보안/안전 — 보안/안전 이슈를 {lines}줄로 요약. 취약점/사고, 영향 범위, 대응/패치.",
            "p5": "정책/규제 — IT 규제/표준을 {lines}줄로 요약. 취지/대상, 의무/완화, 일정/유예.",
            "p6": "산업/시장 — 산업/시장 동향을 {lines}줄로 요약. 수요/공급, 주요 업체, 전망/리스크.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "VEHICLE": {
            "default": "자동차/교통 {lines}줄 요약: 모델/가격/인프라 핵심 → 성능·안전·주행거리 등 수치 → 보조금·정책·사고·교통정책 영향 순서로.",
            "p1": "차량/모델 — 신차/모델 뉴스를 {lines}줄로 요약. 파워트레인/성능, 가격/트림, 비교 포인트.",
            "p2": "충전 인프라/정책 — ‘충전/인프라/정책’ 기사를 {lines}줄로 요약. 변화 요지, 요금/지원, 이용자 영향.",
            "p3": "모빌리티/교통 — 교통·모빌리티 이슈를 {lines}줄로 요약. 정책/제도, 안전/사고, 이용자 유의점.",
            "p4": "리콜/안전 — 리콜/안전 관련 뉴스를 {lines}줄로 요약. 대상/원인, 조치 내용, 일정/방법.",
            "p5": "자율주행/SW — 자율주행/소프트웨어 이슈를 {lines}줄로 요약. 기능/제약, 규제/윤리, 로드맵.",
            "p6": "중고/정비 — 중고/정비 관련 소식을 {lines}줄로 요약. 가격/보증, 점검 포인트, 소비자 주의.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "TRAVEL_FOOD": {
            "default": "여행/음식 {lines}줄 요약: 장소/코스/메뉴의 특징 → 가격·운영시각·예약·접근 등 실용 정보 → 주의사항·성수기 팁을 정리해줘.",
            "p1": "여행 코스 — ‘여행 기사’를 {lines}줄로 요약. 목적지/기간/비용, 이동/숙박/추천 포인트.",
            "p2": "항공/호텔 — 항공/호텔 관련 소식을 {lines}줄로 요약. 가격/조건, 예약·취소 규정, 주의/추천.",
            "p3": "푸드/외식 — ‘푸드/외식 뉴스’를 {lines}줄로 요약. 메뉴/가격/정보, 행사/특가, 지역/가이드.",
            "p4": "축제/행사 — ‘페스티벌/행사’를 {lines}줄로 요약. 라인업/프로그램, 일정/장소/티켓, 관람 포인트.",
            "p5": "지역/관광 — 지역 관광 소식을 {lines}줄로 요약. 코스/교통, 비용/패스, 성수기 팁.",
            "p6": "주의/안전 — 여행/외식 유의사항을 {lines}줄로 요약. 예약/위생/안전 체크리스트.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
        "ART": {
            "default": "예술 {lines}줄 요약: 작품·전시·공연의 주제와 의의 → 작가/기관 정보와 평판·수상 → 관련 정보(기간·장소·요금)를 간결히 써줘.",
            "p1": "전시/공연 안내형 — ‘예술 기사’를 {lines}줄로 요약. 작품/전시/공연 핵심, 장소/기간/티켓, 특징/테마.",
            "p2": "평론/수상 브리핑 — ‘평론/수상 소식’을 {lines}줄로 요약. 작품/수상 내역, 의미/평가, 파장/논점 최소화.",
            "p3": "페스티벌/행사 가이드 — ‘페스티벌/행사’를 {lines}줄로 요약. 라인업/프로그램, 일정/장소/티켓, 관람 포인트.",
            "p4": "작가/기관 — 작가·기관 관련 뉴스를 {lines}줄로 요약. 이력/업적, 대표작, 전시/공연/신작.",
            "p5": "산업/시장 — 예술 산업/시장 소식을 {lines}줄로 요약. 거래/경매, 정책/지원, 트렌드.",
            "p6": "주의/안내 — 관람/참여 유의사항을 {lines}줄로 요약. 예약/이용/환불 규정, 접근성 정보.",
            "p7": "",
            "p8": "",
            "p9": "",
            "p10": "",
        },
    }

    # 기본 타입
    DEFAULT_TYPE = "DEFAULT"

    # 2) 별칭/정규화 테이블 (카테고리 → 타입)
    #   - 뉴스 카테고리의 다양한 표기를 보정
    #   - 레거시('AIBOT')가 오면 DEFAULT로 흡수
    TYPE_ALIASES: Dict[str, str] = {
        "": "DEFAULT",
        "AIBOT": "DEFAULT",

        # 카테고리 표기 보정(선택)
        "WORLD": "INTERNATIONAL",
        "GLOBAL": "INTERNATIONAL",
        "IT": "IT_SCIENCE",
        "SCIENCE": "IT_SCIENCE",
        "AUTO": "VEHICLE",
        "CAR": "VEHICLE",
        "TRAVEL": "TRAVEL_FOOD",
        "FOOD": "TRAVEL_FOOD",
        "CULTURE": "ART",
    }

    # ──────────────────────────────────────────────────────────────────────────
    # 내부 유틸
    # ──────────────────────────────────────────────────────────────────────────
    @staticmethod
    def _normalize_key(s: Optional[str]) -> str:
        u = (s or "").upper().strip()
        return u.replace("-", "_").replace(" ", "_").replace("/", "_")

    @classmethod
    def _canon_type(cls, t: Optional[str]) -> str:
        """
        미지정/미등록 타입을 DEFAULT로 정규화.
        """
        u = cls._normalize_key(t)
        u = cls.TYPE_ALIASES.get(u, u or cls.DEFAULT_TYPE)
        return u if u in cls.PROMPTS else cls.DEFAULT_TYPE

    @staticmethod
    def _fmt(s: str, base: Dict) -> str:
        try:
            return s.format(**base)
        except KeyError:
            # 알 수 없는 placeholder는 그대로 둠
            return s

    # ──────────────────────────────────────────────────────────────────────────
    # 공개 API
    # ──────────────────────────────────────────────────────────────────────────
    @classmethod
    def get_many_by_types(
            cls,
            types: List[str],
            include_default: bool = True,
            **params
    ) -> List[Dict[str, str]]:
        """
        여러 타입을 받아 각 타입에 등록된 모든 프롬프트를 반환.
        타입이 미지정/미등록이면 DEFAULT로 매핑.
        반환: [{"id": "TYPE:key", "prompt": "..."}, ...]
        """
        results: List[Dict[str, str]] = []
        seen = set()
        base = {"lines": 3, **params}

        for t in (types or [cls.DEFAULT_TYPE]):
            g = cls._canon_type(t)
            entries = cls.PROMPTS.get(g, {})
            for k, v in entries.items():
                if k == "default" and not include_default:
                    continue
                pid = f"{g}:{k}"
                if pid in seen:
                    continue
                seen.add(pid)
                results.append({"id": pid, "prompt": cls._fmt(v, base)})
        return results

    @classmethod
    def get(cls, prompt_or_id: Optional[str], type_: Optional[str], **params) -> str:
        """
        프롬프트 텍스트만 반환.
        - type_이 미지정/미등록이면 DEFAULT로 처리
        - prompt_or_id가 없으면 해당 타입의 default 사용
        - "TYPE:key" 표기 지원, 키가 없으면 DEFAULT로 폴백
        - 커스텀 문자열이면 그대로 사용
        """
        base = {"lines": 3, **params}
        t = cls._canon_type(type_)
        s = (prompt_or_id or "").strip()

        if not s:
            return cls._fmt(cls.PROMPTS[t].get("default", cls.PROMPTS[cls.DEFAULT_TYPE]["default"]), base)

        # "TYPE:key"
        if ":" in s:
            tt, key = s.split(":", 1)
            tt = cls._canon_type(tt)
            val = cls.PROMPTS.get(tt, {}).get(key)
            if val:
                return cls._fmt(val, base)
            # DEFAULT에서 같은 key 재시도
            val = cls.PROMPTS[cls.DEFAULT_TYPE].get(key)
            if val:
                return cls._fmt(val, base)
            # 그래도 없으면 DEFAULT.default
            return cls._fmt(cls.PROMPTS[cls.DEFAULT_TYPE]["default"], base)

        # 현재 타입에서 키 조회
        val = cls.PROMPTS.get(t, {}).get(s)
        if val:
            return cls._fmt(val, base)

        # DEFAULT에서 키 조회
        val = cls.PROMPTS[cls.DEFAULT_TYPE].get(s)
        if val:
            return cls._fmt(val, base)

        # 커스텀 문자열
        return cls._fmt(s, base)

    @classmethod
    def get_effective(
            cls,
            prompt_or_id: Optional[str],
            type_candidate: Optional[str],
            **params
    ) -> Tuple[str, str]:
        """
        (resolved_type, prompt_text) 반환.
        - type_candidate가 PROMPTS에 없으면 DEFAULT로 치환
        - prompt_or_id가 없으면 resolved_type의 default 사용
        - "TYPE:key" 지원 및 DEFAULT 폴백
        """
        resolved_type = cls._canon_type(type_candidate)
        text = cls.get(prompt_or_id, resolved_type, **params)
        # 주의: get() 내부에서도 resolved_type이 바뀌진 않게 설계 (TYPE:key의 TYPE은 별개)
        return resolved_type, text
