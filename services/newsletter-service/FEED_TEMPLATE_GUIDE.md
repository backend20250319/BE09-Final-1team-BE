# 피드 템플릿 가이드

뉴스 서비스에서 뉴스를 가져와서 카카오톡 피드 템플릿으로 구성하는 뉴스레터 시스템입니다.

## 📋 개요

피드 템플릿은 카카오톡 메시지 템플릿의 한 종류로, 뉴스 콘텐츠를 구조화된 형태로 전달할 수 있습니다.

### 지원하는 템플릿 종류

- **피드 A형**: 기본 피드 템플릿 (이미지, 텍스트, 소셜, 버튼)
- **피드 B형**: 아이템이 추가된 피드 템플릿 (피드 A형 + 아이템 콘텐츠)

## 🏗️ 피드 템플릿 구조

### 피드 A형 구성 요소

| 구분 | 구성 요소 | 설명 | 기본 템플릿 | 사용자 정의 템플릿 |
|------|-----------|------|-------------|-------------------|
| 🅐 | 이미지 | 메시지 상단에 추가되는 이미지 | Content.image_url<br>Content.image_width<br>Content.image_height | 이미지 |
| 🅑 | 텍스트 | 메시지 제목과 설명 영역 | Content.title<br>Content.description | 메시지 제목/본문 |
| 🅒 | 소셜 | 소셜 지표 영역 | social | 소셜 |
| 🅓 | 버튼 | 메시지 하단의 버튼 영역 | buttons<br>button_title | 버튼 |
| - | 링크 | 클릭 가능한 컴포넌트에 추가할 링크 | Content.link<br>Button.link | 컴포넌트 링크 관리 |

### 피드 B형 구성 요소

피드 B형은 피드 A형의 모든 구성 요소에 추가로 다음 요소들을 포함합니다:

| 구분 | 구성 요소 | 설명 | 기본 템플릿 | 사용자 정의 템플릿 |
|------|-----------|------|-------------|-------------------|
| 🅔 | 헤더 또는 프로필 | 헤더와 프로필 중 한 가지만 사용 가능<br>- 헤더: 상단 문구 형식<br>- 프로필: 닉네임과 프로필 사진으로 구성 | ItemContent.profile_text<br>ItemContent.profile_image_url | 헤더, 프로필 |
| 🅕 | 이미지 아이템 | 제목, 카테고리, 이미지로 구성된 영역<br>최대 1개 추가 가능 | ItemContent.title_image_text<br>ItemContent.title_image_url<br>ItemContent.title_image_category | 이미지 아이템 |
| 🅖 | 텍스트 아이템 | 텍스트로 구성된 아이템 영역<br>최대 5개 추가 가능 | ItemContent.items | 텍스트 아이템 리스트 |
| 🅗 | 요약 정보 | 텍스트 아이템의 종합 및 요약 정보, 제목과 설명으로 구성 | ItemContent.sum<br>ItemContent.sum_op | 텍스트 아이템 리스트 |

## 🚀 API 사용법

### 1. 피드 템플릿 생성 API

#### 개인화된 피드 A형 템플릿 생성
```http
GET /api/feed-template/personalized/feed-a/{userId}
```

#### 개인화된 피드 B형 템플릿 생성
```http
GET /api/feed-template/personalized/feed-b/{userId}
```

#### 카테고리별 피드 A형 템플릿 생성
```http
GET /api/feed-template/category/feed-a/{category}
```

#### 카테고리별 피드 B형 템플릿 생성
```http
GET /api/feed-template/category/feed-b/{category}
```

#### 트렌딩 뉴스 피드 A형 템플릿 생성
```http
GET /api/feed-template/trending/feed-a
```

#### 트렌딩 뉴스 피드 B형 템플릿 생성
```http
GET /api/feed-template/trending/feed-b
```

#### 최신 뉴스 피드 A형 템플릿 생성
```http
GET /api/feed-template/latest/feed-a
```

#### 최신 뉴스 피드 B형 템플릿 생성
```http
GET /api/feed-template/latest/feed-b
```

### 2. 카카오톡 메시지 전송 API

#### 피드 A형 개인화 뉴스레터 전송
```http
POST /api/kakao/send/feed-a/personalized/{userId}
Authorization: Bearer {accessToken}
```

#### 피드 B형 개인화 뉴스레터 전송
```http
POST /api/kakao/send/feed-b/personalized/{userId}
Authorization: Bearer {accessToken}
```

#### 피드 A형 카테고리별 뉴스레터 전송
```http
POST /api/kakao/send/feed-a/category/{category}
Authorization: Bearer {accessToken}
```

#### 피드 B형 카테고리별 뉴스레터 전송
```http
POST /api/kakao/send/feed-b/category/{category}
Authorization: Bearer {accessToken}
```

#### 피드 A형 트렌딩 뉴스레터 전송
```http
POST /api/kakao/send/feed-a/trending
Authorization: Bearer {accessToken}
```

#### 피드 B형 트렌딩 뉴스레터 전송
```http
POST /api/kakao/send/feed-b/trending
Authorization: Bearer {accessToken}
```

### 3. 유틸리티 API

#### 피드 템플릿을 카카오톡 API용 변수로 변환
```http
POST /api/feed-template/convert-to-kakao-args
Content-Type: application/json

{
  "feedType": "FEED_A",
  "content": {
    "title": "뉴스 제목",
    "description": "뉴스 설명",
    "imageUrl": "https://example.com/image.jpg",
    "imageWidth": 800,
    "imageHeight": 400,
    "link": "https://example.com/news/1"
  },
  "buttons": [
    {
      "title": "뉴스 보기",
      "link": "https://example.com/news/1",
      "action": "web"
    }
  ]
}
```

#### 피드 템플릿 미리보기
```http
GET /api/feed-template/preview/{feedType}
```

## 📝 사용 예시

### 1. 개인화된 피드 A형 뉴스레터 생성

```java
// FeedTemplateService를 사용하여 개인화된 피드 A형 템플릿 생성
FeedTemplate feedTemplate = feedTemplateService.createPersonalizedFeedTemplate(
    userId, FeedTemplate.FeedType.FEED_A);

// 카카오톡 API용 템플릿 변수 생성
Map<String, Object> kakaoArgs = feedTemplate.toKakaoTemplateArgs();

// 카카오톡 메시지 전송
kakaoMessageService.sendMessage(accessToken, feedATemplateId, kakaoArgs);
```

### 2. 카테고리별 피드 B형 뉴스레터 생성

```java
// 카테고리별 피드 B형 템플릿 생성
FeedTemplate feedTemplate = feedTemplateService.createCategoryFeedTemplate(
    "정치", FeedTemplate.FeedType.FEED_B);

// 카카오톡 API용 템플릿 변수 생성
Map<String, Object> kakaoArgs = feedTemplate.toKakaoTemplateArgs();

// 카카오톡 메시지 전송
kakaoMessageService.sendMessage(accessToken, feedBTemplateId, kakaoArgs);
```

### 3. 트렌딩 뉴스 피드 A형 뉴스레터 생성

```java
// 트렌딩 뉴스 피드 A형 템플릿 생성
FeedTemplate feedTemplate = feedTemplateService.createTrendingFeedTemplate(
    FeedTemplate.FeedType.FEED_A);

// 카카오톡 API용 템플릿 변수 생성
Map<String, Object> kakaoArgs = feedTemplate.toKakaoTemplateArgs();

// 카카오톡 메시지 전송
kakaoMessageService.sendMessage(accessToken, feedATemplateId, kakaoArgs);
```

## ⚙️ 설정

### application.yml 설정

```yaml
kakao:
  templates:
    feed-a: ${KAKAO_FEED_A_TEMPLATE_ID:123799}
    feed-b: ${KAKAO_FEED_B_TEMPLATE_ID:123800}
  message:
    enabled: false  # 개발 환경에서는 시뮬레이션 모드
```

### 환경 변수 설정

```bash
# 카카오톡 피드 A형 템플릿 ID
KAKAO_FEED_A_TEMPLATE_ID=123799

# 카카오톡 피드 B형 템플릿 ID
KAKAO_FEED_B_TEMPLATE_ID=123800
```

## 🔧 개발 및 테스트

### 1. 시뮬레이션 모드

개발 환경에서는 `kakao.message.enabled=false`로 설정하여 실제 카카오톡 메시지 전송 없이 시뮬레이션 모드로 동작합니다.

### 2. 피드 템플릿 미리보기

```http
GET /api/feed-template/preview/FEED_A
GET /api/feed-template/preview/FEED_B
```

### 3. 로그 확인

피드 템플릿 생성 및 전송 과정은 상세한 로그를 제공합니다:

```
INFO  - 개인화된 피드 템플릿 생성 시작: userId=1, feedType=FEED_A
INFO  - 관심사 기반 뉴스 조회: category=정치, count=5
INFO  - 개인화된 피드 템플릿 생성 완료: userId=1, articleCount=5
INFO  - 피드 A형 뉴스레터 전송 시작: userId=1
INFO  - 피드 A형 뉴스레터 전송 완료: userId=1
```

## 📊 데이터 흐름

1. **뉴스 데이터 조회**: NewsServiceClient를 통해 뉴스 데이터 조회
2. **개인화 처리**: UserServiceClient를 통해 사용자 관심사 조회
3. **템플릿 생성**: FeedTemplateService에서 피드 템플릿 생성
4. **변수 변환**: 카카오톡 API용 템플릿 변수로 변환
5. **메시지 전송**: KakaoMessageService를 통해 카카오톡 메시지 전송

## 🎯 주요 기능

- **개인화**: 사용자의 관심사 기반 뉴스 추천
- **카테고리별**: 특정 카테고리의 뉴스만 선별
- **트렌딩**: 인기 있는 뉴스 우선 표시
- **최신**: 최신 뉴스 우선 표시
- **유연한 템플릿**: 피드 A형과 B형 선택 가능
- **에러 처리**: 뉴스 조회 실패 시 기본 뉴스 제공
- **시뮬레이션**: 개발 환경에서 실제 전송 없이 테스트 가능

## 🔍 문제 해결

### 뉴스 데이터가 없는 경우
- 기본 뉴스 데이터를 자동으로 생성하여 제공
- 로그에서 "기본 뉴스 제공" 메시지 확인

### 카카오톡 메시지 전송 실패
- `kakao.message.enabled` 설정 확인
- 액세스 토큰 유효성 확인
- 템플릿 ID 설정 확인

### 개인화 데이터 부족
- 사용자 관심사 데이터 확인
- 트렌딩 뉴스로 자동 대체

이 가이드를 통해 뉴스 서비스의 데이터를 활용한 카카오톡 피드 템플릿 뉴스레터 시스템을 효과적으로 사용할 수 있습니다.
