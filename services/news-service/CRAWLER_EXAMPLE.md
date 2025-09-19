# 크롤러에서 백엔드로 데이터 전송 예시

## 📋 개요

크롤러(news_crawler)에서 백엔드(news-service)로 뉴스 데이터를 전송하는 방법을 설명합니다.

## 🔗 API 엔드포인트

### 1. 뉴스 저장 API

```
POST http://localhost:8082/api/news/crawl
Content-Type: application/json
```

### 2. 뉴스 미리보기 API

```
POST http://localhost:8082/api/news/crawl/preview
Content-Type: application/json
```

## 📦 요청 데이터 형식 (NewsCrawlDto)

```json
{
  "linkId": "https://example.com/news/123",
  "title": "뉴스 제목",
  "press": "언론사명",
  "content": "뉴스 내용...",
  "reporterName": "기자명",
  "publishedAt": "2024-01-15T10:30:00",
  "categoryId": 1
}
```

## 🐍 Python 크롤러 예시

```python
import requests
import json
from datetime import datetime

class NewsCrawler:
    def __init__(self, backend_url="http://localhost:8082"):
        self.backend_url = backend_url

    def send_news_data(self, news_data):
        """크롤링된 뉴스 데이터를 백엔드로 전송"""

        # DTO 형식으로 데이터 구성
        dto = {
            "linkId": int(news_data.get("link", "0")),  # Long 타입으로 변환
            "title": news_data.get("title"),
            "press": news_data.get("press"),
            "content": news_data.get("content"),
            "reporterName": news_data.get("reporter"),
            "publishedAt": news_data.get("published_at"),
            "categoryId": news_data.get("category_id")
        }

        try:
            # 백엔드로 POST 요청
            response = requests.post(
                f"{self.backend_url}/api/news/crawl",
                json=dto,
                headers={"Content-Type": "application/json"}
            )

            if response.status_code == 200:
                print("✅ 뉴스 저장 성공:", response.text)
                return True
            else:
                print("❌ 뉴스 저장 실패:", response.text)
                return False

        except Exception as e:
            print(f"❌ 전송 오류: {e}")
            return False

    def preview_news_data(self, news_data):
        """뉴스 데이터 미리보기 (저장하지 않음)"""

        dto = {
            "linkId": int(news_data.get("link", "0")),  # Long 타입으로 변환
            "title": news_data.get("title"),
            "press": news_data.get("press"),
            "content": news_data.get("content"),
            "reporterName": news_data.get("reporter"),
            "publishedAt": news_data.get("published_at"),
            "categoryId": news_data.get("category_id")
        }

        try:
            response = requests.post(
                f"{self.backend_url}/api/news/crawl/preview",
                json=dto,
                headers={"Content-Type": "application/json"}
            )

            if response.status_code == 200:
                preview_data = response.json()
                print("📋 미리보기 성공:", preview_data)
                return preview_data
            else:
                print("❌ 미리보기 실패:", response.text)
                return None

        except Exception as e:
            print(f"❌ 미리보기 오류: {e}")
            return None

# 사용 예시
if __name__ == "__main__":
    crawler = NewsCrawler()

    # 크롤링된 뉴스 데이터 예시
    sample_news = {
        "link": 123,  # Long 타입으로 변경
        "title": "샘플 뉴스 제목",
        "press": "샘플 언론사",
        "content": "이것은 샘플 뉴스 내용입니다...",
        "reporter": "홍길동",
        "published_at": "2024-01-15T10:30:00",
        "category_id": 1
    }

    # 미리보기 먼저 실행
    preview = crawler.preview_news_data(sample_news)

    # 미리보기가 성공하면 저장
    if preview:
        crawler.send_news_data(sample_news)
```

## ☕ Java 크롤러 예시

```java
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class NewsCrawler {

    private final RestTemplate restTemplate;
    private final String backendUrl;

    public NewsCrawler(String backendUrl) {
        this.restTemplate = new RestTemplate();
        this.backendUrl = backendUrl;
    }

    public boolean sendNewsData(NewsDetail newsDetail) {
        try {
            // DTO 생성
            NewsCrawlDto dto = NewsCrawlDto.builder()
                .linkId(newsDetail.getLink())
                .title(newsDetail.getTitle())
                .press(newsDetail.getPress())
                .content(newsDetail.getContent())
                .reporterName(newsDetail.getReporter())
                .publishedAt(newsDetail.getPublishedAt())
                .categoryId(newsDetail.getCategoryId())
                .build();

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NewsCrawlDto> request = new HttpEntity<>(dto, headers);

            // 백엔드로 POST 요청
            ResponseEntity<String> response = restTemplate.postForEntity(
                backendUrl + "/api/news/crawl",
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ 뉴스 저장 성공: " + response.getBody());
                return true;
            } else {
                System.out.println("❌ 뉴스 저장 실패: " + response.getBody());
                return false;
            }

        } catch (Exception e) {
            System.out.println("❌ 전송 오류: " + e.getMessage());
            return false;
        }
    }
}
```

## 🔧 설정 사항

### 1. 백엔드 포트 확인

- 기본 포트: `8082`
- `application.yml`에서 포트 설정 확인

### 2. CORS 설정

- 백엔드에서 `@CrossOrigin(origins = "*")` 설정 완료
- 크롤러에서 CORS 오류 없이 요청 가능

### 3. 데이터베이스 연결

- MySQL 연결 확인
- `news_crawl` 테이블 존재 확인

## 📊 데이터 흐름

```
[크롤러] → [DTO 생성] → [HTTP POST] → [백엔드 API] → [Entity 변환] → [DB 저장]
```

1. **크롤러**: 뉴스 데이터 수집 및 DTO 생성
2. **HTTP 전송**: RestTemplate/requests로 백엔드로 전송
3. **백엔드**: DTO를 Entity로 변환하여 DB 저장
4. **결과**: 성공/실패 응답 반환

## ⚠️ 주의사항

1. **중복 체크**: 같은 `linkId`로 중복 저장 방지
2. **데이터 검증**: 필수 필드 누락 시 저장 실패
3. **에러 처리**: 네트워크 오류, DB 오류 등 예외 처리
4. **성능**: 대량 데이터 전송 시 배치 처리 고려
