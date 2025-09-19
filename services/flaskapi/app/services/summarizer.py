# app/services/summarizer.py
from openai import OpenAI
from ..config import Config

_client = OpenAI(api_key=Config.OPENAI_API_KEY)

def summarize(text: str, prompt: str) -> str:
    resp = _client.chat.completions.create(
        model=Config.OPENAI_MODEL,
        messages=[
            {"role": "system", "content": """당신은 뉴스 요약 전문가입니다. 
            규칙:
            1) HTML 태그, 스타일, 스크립트, 광고, 네비게이션 텍스트는 제거한다.
            2) 숫자, 날짜, 고유명사는 보존한다.
            3) 각 줄은 반드시 완전한 문장으로 끝나야 한다.
            """
             },
            {"role": "system", "content": "HTML 태그는 모두 제거하고 요약하세요."},
            {"role": "user", "content": f"{prompt}\n\n{text}"},
        ]
    )
    return resp.choices[0].message.content.strip()
# End of app/services/summarizer.py
