# 프론트엔드 API 가이드

`http://localhost:3000/newsletter/preview` 페이지와 연결하기 위한 API 가이드입니다.

## 🚀 기본 설정

### CORS 설정
모든 컨트롤러에 `@CrossOrigin(origins = "*")` 설정이 되어 있어 프론트엔드에서 자유롭게 호출할 수 있습니다.

### Base URL
- **뉴스레터 서비스**: `http://localhost:8085`
- **프론트엔드**: `http://localhost:3000`

## 📋 피드 B형 뉴스레터 API

### 1. 미리보기 API

#### 피드 B형 일반 미리보기
```http
GET /api/newsletter/preview/feed-b
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "type": "trending",
    "param": "",
    "feedType": "FEED_B",
    "template": {
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
        },
        {
          "title": "더 많은 뉴스",
          "link": "/news",
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
        },
        {
          "title": "더 많은 뉴스",
          "link": "/news",
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
    "timestamp": 1703123456789
  }
}
```

#### 피드 B형 개인화 미리보기
```http
GET /api/newsletter/preview/feed-b?type=personalized&param=1
```

#### 피드 B형 카테고리별 미리보기
```http
GET /api/newsletter/preview/feed-b?type=category&param=정치
```

#### 피드 B형 트렌딩 미리보기
```http
GET /api/newsletter/preview/feed-b?type=trending
```

### 2. 전송 API

#### 피드 B형 개인화 뉴스레터 전송
```http
POST /api/newsletter/send/feed-b/personalized/{userId}
Authorization: Bearer {accessToken}
```

#### 피드 B형 카테고리별 뉴스레터 전송
```http
POST /api/newsletter/send/feed-b/category/{category}
Authorization: Bearer {accessToken}
```

#### 피드 B형 트렌딩 뉴스레터 전송
```http
POST /api/newsletter/send/feed-b/trending
Authorization: Bearer {accessToken}
```

## 🎨 프론트엔드 구현 예시

### React 컴포넌트 예시

```jsx
import React, { useState, useEffect } from 'react';

const NewsletterPreview = () => {
  const [previewData, setPreviewData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // 미리보기 데이터 가져오기
  const fetchPreview = async (type = 'trending', param = '') => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch(
        `http://localhost:8085/api/newsletter/preview/feed-b?type=${type}&param=${param}`
      );
      
      if (!response.ok) {
        throw new Error('미리보기 데이터를 가져올 수 없습니다.');
      }
      
      const data = await response.json();
      setPreviewData(data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // 뉴스레터 전송
  const sendNewsletter = async (type, param, accessToken) => {
    try {
      let url = '';
      switch (type) {
        case 'personalized':
          url = `http://localhost:8085/api/newsletter/send/feed-b/personalized/${param}`;
          break;
        case 'category':
          url = `http://localhost:8085/api/newsletter/send/feed-b/category/${param}`;
          break;
        case 'trending':
          url = `http://localhost:8085/api/newsletter/send/feed-b/trending`;
          break;
        default:
          throw new Error('지원하지 않는 타입입니다.');
      }

      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error('뉴스레터 전송에 실패했습니다.');
      }

      const result = await response.json();
      alert('뉴스레터가 성공적으로 전송되었습니다!');
      return result;
    } catch (err) {
      alert(`뉴스레터 전송 실패: ${err.message}`);
      throw err;
    }
  };

  useEffect(() => {
    fetchPreview();
  }, []);

  if (loading) return <div>로딩 중...</div>;
  if (error) return <div>에러: {error}</div>;
  if (!previewData) return <div>데이터가 없습니다.</div>;

  const template = previewData.template;

  return (
    <div className="newsletter-preview">
      <h1>피드 B형 뉴스레터 미리보기</h1>
      
      {/* 미리보기 타입 선택 */}
      <div className="preview-controls">
        <button onClick={() => fetchPreview('trending')}>트렌딩</button>
        <button onClick={() => fetchPreview('personalized', '1')}>개인화</button>
        <button onClick={() => fetchPreview('category', '정치')}>정치</button>
        <button onClick={() => fetchPreview('category', '경제')}>경제</button>
      </div>

      {/* 피드 B형 템플릿 미리보기 */}
      <div className="feed-template-preview">
        {/* 메인 콘텐츠 */}
        <div className="main-content">
          {template.content.imageUrl && (
            <img 
              src={template.content.imageUrl} 
              alt="뉴스 이미지"
              style={{ width: '100%', maxWidth: '400px', height: 'auto' }}
            />
          )}
          <h2>{template.content.title}</h2>
          <p>{template.content.description}</p>
          
          {/* 소셜 지표 */}
          {template.content.social && (
            <div className="social-metrics">
              <span>❤️ {template.content.social.likeCount}</span>
              <span>💬 {template.content.social.commentCount}</span>
              <span>📤 {template.content.social.shareCount}</span>
              <span>👁️ {template.content.social.viewCount}</span>
            </div>
          )}
        </div>

        {/* 버튼들 */}
        {template.buttons && (
          <div className="buttons">
            {template.buttons.map((button, index) => (
              <button key={index} className="action-button">
                {button.title}
              </button>
            ))}
          </div>
        )}

        {/* 아이템 콘텐츠 (피드 B형) */}
        {template.itemContents && template.itemContents.map((itemContent, index) => (
          <div key={index} className="item-content">
            <div className="profile-section">
              {itemContent.profileImageUrl && (
                <img 
                  src={itemContent.profileImageUrl} 
                  alt="프로필"
                  style={{ width: '40px', height: '40px', borderRadius: '50%' }}
                />
              )}
              <span>{itemContent.profileText}</span>
            </div>
            
            <div className="title-image-section">
              {itemContent.titleImageUrl && (
                <img 
                  src={itemContent.titleImageUrl} 
                  alt="타이틀 이미지"
                  style={{ width: '100%', maxWidth: '300px', height: 'auto' }}
                />
              )}
              <h3>{itemContent.titleImageText}</h3>
              <span className="category">{itemContent.titleImageCategory}</span>
            </div>

            {/* 텍스트 아이템들 */}
            {itemContent.items && itemContent.items.map((item, itemIndex) => (
              <div key={itemIndex} className="text-item">
                <h4>{item.title}</h4>
                <p>{item.description}</p>
              </div>
            ))}

            {/* 요약 정보 */}
            <div className="summary">
              <h4>{itemContent.sum}</h4>
              <p>{itemContent.sumOp}</p>
            </div>
          </div>
        ))}
      </div>

      {/* 전송 버튼 */}
      <div className="send-controls">
        <button 
          onClick={() => sendNewsletter(previewData.type, previewData.param, 'your-access-token')}
          className="send-button"
        >
          뉴스레터 전송하기
        </button>
      </div>
    </div>
  );
};

export default NewsletterPreview;
```

### CSS 스타일 예시

```css
.newsletter-preview {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.preview-controls {
  margin-bottom: 20px;
}

.preview-controls button {
  margin-right: 10px;
  padding: 8px 16px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
}

.preview-controls button:hover {
  background: #f5f5f5;
}

.feed-template-preview {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.main-content h2 {
  color: #333;
  margin: 16px 0 8px 0;
}

.main-content p {
  color: #666;
  line-height: 1.5;
  margin-bottom: 16px;
}

.social-metrics {
  display: flex;
  gap: 16px;
  margin: 16px 0;
  color: #666;
  font-size: 14px;
}

.buttons {
  display: flex;
  gap: 10px;
  margin: 20px 0;
}

.action-button {
  padding: 12px 24px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.action-button:hover {
  background: #0056b3;
}

.item-content {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.profile-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.title-image-section {
  margin-bottom: 20px;
}

.title-image-section h3 {
  color: #333;
  margin: 8px 0 4px 0;
}

.category {
  background: #e9ecef;
  color: #495057;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
}

.text-item {
  margin-bottom: 16px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 4px;
}

.text-item h4 {
  color: #333;
  margin: 0 0 8px 0;
  font-size: 14px;
}

.text-item p {
  color: #666;
  margin: 0;
  font-size: 13px;
}

.summary {
  background: #e3f2fd;
  padding: 16px;
  border-radius: 4px;
  margin-top: 16px;
}

.summary h4 {
  color: #1976d2;
  margin: 0 0 8px 0;
}

.summary p {
  color: #1976d2;
  margin: 0;
  font-size: 14px;
}

.send-controls {
  margin-top: 30px;
  text-align: center;
}

.send-button {
  padding: 16px 32px;
  background: #28a745;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
}

.send-button:hover {
  background: #218838;
}
```

## 🔧 개발 환경 설정

### 1. 백엔드 서버 실행
```bash
cd services/newsletter-service
./gradlew bootRun
```

### 2. 프론트엔드 서버 실행
```bash
cd frontend
npm start
```

### 3. API 테스트
```bash
# 미리보기 테스트
curl http://localhost:8085/api/newsletter/preview/feed-b

# 개인화 미리보기 테스트
curl "http://localhost:8085/api/newsletter/preview/feed-b?type=personalized&param=1"

# 카테고리별 미리보기 테스트
curl "http://localhost:8085/api/newsletter/preview/feed-b?type=category&param=정치"
```

## 📱 모바일 대응

피드 B형 템플릿은 카카오톡 메시지 형태로 설계되어 있어 모바일 환경에서도 최적화되어 있습니다.

### 반응형 디자인
```css
@media (max-width: 768px) {
  .newsletter-preview {
    padding: 10px;
  }
  
  .feed-template-preview {
    padding: 15px;
  }
  
  .buttons {
    flex-direction: column;
  }
  
  .action-button {
    width: 100%;
    margin-bottom: 10px;
  }
}
```

## 🚀 배포 시 고려사항

1. **CORS 설정**: 프로덕션 환경에서는 특정 도메인만 허용하도록 설정
2. **HTTPS**: 카카오톡 API는 HTTPS가 필요할 수 있음
3. **에러 처리**: 네트워크 오류 및 API 오류에 대한 적절한 처리
4. **로딩 상태**: 사용자 경험을 위한 로딩 인디케이터
5. **캐싱**: 미리보기 데이터의 적절한 캐싱 전략

이 가이드를 통해 프론트엔드에서 피드 B형 뉴스레터 미리보기와 전송 기능을 구현할 수 있습니다.
