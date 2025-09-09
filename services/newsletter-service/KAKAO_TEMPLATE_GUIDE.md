# 카카오톡 뉴스레터 메시지 템플릿 가이드

## 📱 뉴스레터 카카오톡 메시지 템플릿 구성

### 1. 일일 뉴스레터 템플릿 (Template ID: 123798)

#### 메시지 구성
```
📰 ${user_name}님의 오늘 뉴스레터

${newsletter_title}

${newsletter_content}

📅 ${newsletter_date}
⏱️ 읽기 시간: ${estimated_read_time}

[뉴스레터 보기] [구독 관리]
```

#### 사용자 인자 (template_args)
```json
{
  "user_name": "홍길동",
  "newsletter_title": "오늘의 주요 뉴스",
  "newsletter_content": "정치, 경제, 사회 분야의 주요 뉴스를 정리했습니다...",
  "newsletter_date": "2024년 01월 15일",
  "estimated_read_time": "3분"
}
```

### 2. 카테고리별 뉴스레터 템플릿 (Template ID: 123802-123807)

#### 정치 뉴스레터 (Template ID: 123802)
```
🏛️ ${user_name}님의 정치 뉴스레터

${newsletter_title}

${newsletter_content}

📅 ${newsletter_date}
⏱️ 읽기 시간: ${estimated_read_time}

[정치 뉴스 보기] [구독 해지]
```

#### 경제 뉴스레터 (Template ID: 123803)
```
💰 ${user_name}님의 경제 뉴스레터

${newsletter_title}

${newsletter_content}

📅 ${newsletter_date}
⏱️ 읽기 시간: ${estimated_read_time}

[경제 뉴스 보기] [구독 해지]
```

### 3. 속보 뉴스레터 템플릿 (Template ID: 123801)

```
🚨 ${user_name}님, 속보가 도착했습니다!

${newsletter_title}

${newsletter_content}

📅 ${newsletter_date}
⏱️ 읽기 시간: ${estimated_read_time}

[속보 보기] [구독 관리]
```

## 🔗 링크 설정

### 공통 링크 설정
- **Mobile Web**: `https://newsletter.example.com/mobile/${user_id}/${newsletter_id}`
- **Web**: `https://newsletter.example.com/web/${user_id}/${newsletter_id}`
- **Android Scheme**: `kakao${NATIVE_APP_KEY}://kakaolink?user_id=${user_id}&newsletter_id=${newsletter_id}`
- **iOS Scheme**: `kakao${NATIVE_APP_KEY}://kakaolink?user_id=${user_id}&newsletter_id=${newsletter_id}`

### 컴포넌트별 링크
- **뉴스레터 보기 버튼**: 개인화된 뉴스레터 페이지
- **구독 관리 버튼**: 구독 설정 페이지
- **구독 해지 버튼**: 구독 해지 페이지

## 📊 사용자 인자 활용 예시

### 1. 개인화된 인사말
```json
{
  "user_name": "홍길동",
  "greeting": "안녕하세요, 홍길동님!"
}
```

### 2. 카테고리별 맞춤 메시지
```json
{
  "category_name": "정치",
  "category_emoji": "🏛️",
  "category_priority": "high"
}
```

### 3. 동적 버튼 정렬
```json
{
  "BUT": "0"  // 0: 가로 정렬, 1: 세로 정렬
}
```

### 4. 개인화된 링크
```json
{
  "newsletter_link": "https://newsletter.example.com/personalized/12345/67890",
  "unsubscribe_link": "https://newsletter.example.com/unsubscribe/12345"
}
```

## 🎨 템플릿 디자인 가이드

### 색상 팔레트
- **주 색상**: #3C1E1E (카카오 브랜드 컬러)
- **보조 색상**: #FEE500 (카카오 옐로우)
- **텍스트 색상**: #000000, #666666

### 폰트 크기
- **제목**: 18px (굵게)
- **본문**: 14px (보통)
- **부가 정보**: 12px (연하게)

### 레이아웃
- **여백**: 상하좌우 16px
- **줄 간격**: 1.4
- **버튼 간격**: 8px

## 🔧 API 호출 예시

### 개인화된 뉴스레터 발송
```bash
curl -v -X POST "https://kapi.kakao.com/v2/api/talk/memo/send" \
  -H "Content-Type: application/x-www-form-urlencoded;charset=utf-8" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "template_id=123798" \
  -d 'template_args={
        "user_name": "홍길동",
        "newsletter_title": "오늘의 주요 뉴스",
        "newsletter_content": "정치, 경제, 사회 분야의 주요 뉴스를 정리했습니다...",
        "newsletter_date": "2024년 01월 15일",
        "estimated_read_time": "3분"
      }'
```

### 카테고리별 뉴스레터 발송
```bash
curl -v -X POST "https://kapi.kakao.com/v2/api/talk/memo/send" \
  -H "Content-Type: application/x-www-form-urlencoded;charset=utf-8" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -d "template_id=123802" \
  -d 'template_args={
        "user_name": "홍길동",
        "newsletter_title": "정치 뉴스 요약",
        "newsletter_content": "국회에서 중요한 법안이 통과되었습니다...",
        "newsletter_date": "2024년 01월 15일",
        "estimated_read_time": "2분",
        "category_name": "정치",
        "category_emoji": "🏛️"
      }'
```

## 📈 성능 최적화 팁

### 1. 템플릿 캐싱
- 자주 사용되는 템플릿은 메모리에 캐싱
- 템플릿 ID 매핑 테이블 구성

### 2. 사용자 인자 최적화
- 필수 인자만 포함
- 불필요한 데이터 제거

### 3. 에러 처리
- 템플릿 인자 누락 시 기본값 사용
- 발송 실패 시 재시도 로직

### 4. 모니터링
- 발송 성공률 추적
- 사용자 반응률 분석
- 템플릿별 성능 비교

## 🚀 확장 계획

### 1. A/B 테스트
- 다양한 템플릿 디자인 테스트
- 사용자 반응률 기반 최적화

### 2. 다국어 지원
- 영어, 일본어 템플릿 추가
- 사용자 언어 설정 기반 자동 선택

### 3. 고급 개인화
- 사용자 행동 기반 콘텐츠 추천
- 시간대별 발송 최적화

### 4. 분석 및 리포팅
- 클릭률, 읽기율 분석
- 사용자 참여도 지표 수집
