# 뉴스레터 테스트 가이드

뉴스 서비스에서 뉴스를 가져와서 뉴스레터를 전송하는 기능을 테스트하는 가이드입니다.

## 🚀 테스트 API 엔드포인트

### 1. 뉴스 데이터 조회 테스트

#### 트렌딩 뉴스 데이터 조회
```bash
curl -X GET "http://localhost:8085/api/test/newsletter/news-data?type=trending" \
  -H "Content-Type: application/json"
```

#### 개인화 뉴스 데이터 조회
```bash
curl -X GET "http://localhost:8085/api/test/newsletter/news-data?type=personalized&param=1" \
  -H "Content-Type: application/json"
```

#### 카테고리별 뉴스 데이터 조회
```bash
curl -X GET "http://localhost:8085/api/test/newsletter/news-data?type=category&param=정치" \
  -H "Content-Type: application/json"
```

### 2. 뉴스레터 전송 테스트 (시뮬레이션)

#### 트렌딩 뉴스레터 전송 테스트
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-test?type=trending&testUserId=1" \
  -H "Content-Type: application/json"
```

#### 개인화 뉴스레터 전송 테스트
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-test?type=personalized&param=1&testUserId=1" \
  -H "Content-Type: application/json"
```

#### 카테고리별 뉴스레터 전송 테스트
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-test?type=category&param=정치&testUserId=1" \
  -H "Content-Type: application/json"
```

### 3. 실제 뉴스레터 전송

#### 실제 트렌딩 뉴스레터 전송
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-real?type=trending&userId=1&accessToken=your-access-token" \
  -H "Content-Type: application/json"
```

#### 실제 개인화 뉴스레터 전송
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-real?type=personalized&param=1&userId=1&accessToken=your-access-token" \
  -H "Content-Type: application/json"
```

#### 실제 카테고리별 뉴스레터 전송
```bash
curl -X POST "http://localhost:8085/api/test/newsletter/send-real?type=category&param=정치&userId=1&accessToken=your-access-token" \
  -H "Content-Type: application/json"
```

### 4. 뉴스 서비스 연결 상태 확인

```bash
curl -X GET "http://localhost:8085/api/test/newsletter/news-service-status" \
  -H "Content-Type: application/json"
```

## 📊 응답 예시

### 뉴스 데이터 조회 응답
```json
{
  "success": true,
  "data": {
    "success": true,
    "type": "trending",
    "param": null,
    "feedTemplate": {
      "feedType": "FEED_B",
      "content": {
        "title": "오늘의 주요 뉴스",
        "description": "현재 뉴스를 불러올 수 없어 기본 뉴스를 표시합니다.",
        "imageUrl": "https://via.placeholder.com/800x400",
        "imageWidth": 800,
        "imageHeight": 400,
        "link": "https://example.com/news/1",
        "social": {
          "likeCount": 0,
          "commentCount": 0,
          "shareCount": 10,
          "viewCount": 100
        }
      },
      "buttons": [
        {
          "title": "뉴스 보기",
          "link": "https://example.com/news/1",
          "action": "web"
        }
      ],
      "itemContents": [
        {
          "profileText": "관련 뉴스",
          "profileImageUrl": "https://via.placeholder.com/800x400",
          "titleImageText": "경제 동향",
          "titleImageUrl": "https://via.placeholder.com/800x400",
          "titleImageCategory": "ECONOMY",
          "items": [],
          "sum": "총 2개의 뉴스",
          "sumOp": "다양한 카테고리의 뉴스를 확인해보세요"
        }
      ]
    },
    "kakaoArgs": {
      "title": "오늘의 주요 뉴스",
      "description": "현재 뉴스를 불러올 수 없어 기본 뉴스를 표시합니다.",
      "image_url": "https://via.placeholder.com/800x400",
      "image_width": 800,
      "image_height": 400,
      "link": "https://example.com/news/1",
      "social": {
        "like_count": 0,
        "comment_count": 0,
        "share_count": 10,
        "view_count": 100
      },
      "buttons": [
        {
          "title": "뉴스 보기",
          "link": "https://example.com/news/1",
          "action": "web"
        }
      ],
      "profile_text": "관련 뉴스",
      "profile_image_url": "https://via.placeholder.com/800x400",
      "title_image_text": "경제 동향",
      "title_image_url": "https://via.placeholder.com/800x400",
      "title_image_category": "ECONOMY",
      "sum": "총 2개의 뉴스",
      "sum_op": "다양한 카테고리의 뉴스를 확인해보세요",
      "items": []
    },
    "message": "뉴스 데이터 조회 성공"
  }
}
```

### 전송 테스트 응답
```json
{
  "success": true,
  "data": {
    "success": true,
    "type": "trending",
    "param": null,
    "testUserId": 1,
    "feedTemplate": {
      // 피드 템플릿 데이터
    },
    "kakaoArgs": {
      // 카카오톡 API용 변수
    },
    "message": "뉴스레터 전송 테스트 완료 (시뮬레이션 모드)"
  }
}
```

## 🔧 테스트 시나리오

### 1. 기본 연결 테스트
1. 뉴스 서비스가 실행 중인지 확인
2. 뉴스레터 서비스가 실행 중인지 확인
3. 연결 상태 확인 API 호출

### 2. 뉴스 데이터 조회 테스트
1. 트렌딩 뉴스 데이터 조회
2. 개인화 뉴스 데이터 조회
3. 카테고리별 뉴스 데이터 조회
4. 각 응답에서 실제 뉴스 데이터가 포함되어 있는지 확인

### 3. 뉴스레터 생성 테스트
1. 뉴스 데이터로 피드 템플릿 생성
2. 카카오톡 API용 변수 변환
3. 템플릿 구조 검증

### 4. 전송 테스트
1. 시뮬레이션 모드로 전송 테스트
2. 실제 전송 테스트 (카카오톡 토큰 필요)
3. 전송 결과 확인

## 🚨 문제 해결

### 뉴스 서비스 연결 실패
```bash
# 뉴스 서비스 상태 확인
curl -X GET "http://localhost:8082/actuator/health"

# 뉴스 서비스 API 테스트
curl -X GET "http://localhost:8082/api/trending?limit=5"
```

### 뉴스 데이터가 없는 경우
- 뉴스 서비스에 실제 뉴스 데이터가 있는지 확인
- 데이터베이스 연결 상태 확인
- 뉴스 크롤링 서비스 동작 확인

### 카카오톡 전송 실패
- 카카오톡 액세스 토큰 유효성 확인
- 템플릿 ID 설정 확인
- 카카오톡 API 호출 제한 확인

## 📱 프론트엔드에서 테스트

### JavaScript 예시
```javascript
// 뉴스 데이터 조회 테스트
async function testNewsData() {
  try {
    const response = await fetch('http://localhost:8085/api/test/newsletter/news-data?type=trending');
    const data = await response.json();
    console.log('뉴스 데이터:', data);
    
    if (data.success && data.data.feedTemplate) {
      console.log('피드 템플릿:', data.data.feedTemplate);
      console.log('카카오 변수:', data.data.kakaoArgs);
    }
  } catch (error) {
    console.error('뉴스 데이터 조회 실패:', error);
  }
}

// 뉴스레터 전송 테스트
async function testNewsletterSending() {
  try {
    const response = await fetch('http://localhost:8085/api/test/newsletter/send-test?type=trending&testUserId=1', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    const data = await response.json();
    console.log('전송 테스트 결과:', data);
  } catch (error) {
    console.error('전송 테스트 실패:', error);
  }
}

// 실제 뉴스레터 전송
async function sendRealNewsletter() {
  try {
    const response = await fetch('http://localhost:8085/api/test/newsletter/send-real?type=trending&userId=1&accessToken=your-access-token', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      }
    });
    const data = await response.json();
    console.log('실제 전송 결과:', data);
  } catch (error) {
    console.error('실제 전송 실패:', error);
  }
}
```

## 🎯 성공 기준

1. ✅ **뉴스 서비스 연결**: 뉴스 서비스에서 실제 뉴스 데이터 조회 성공
2. ✅ **피드 템플릿 생성**: 뉴스 데이터로 피드 B형 템플릿 생성 성공
3. ✅ **카카오 변수 변환**: 카카오톡 API용 변수 변환 성공
4. ✅ **전송 테스트**: 시뮬레이션 모드 전송 성공
5. ✅ **실제 전송**: 카카오톡을 통한 실제 뉴스레터 전송 성공

이 가이드를 통해 뉴스 서비스에서 뉴스를 가져와서 뉴스레터를 전송하는 전체 과정을 테스트할 수 있습니다.


