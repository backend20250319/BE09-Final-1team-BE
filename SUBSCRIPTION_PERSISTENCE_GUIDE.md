# 구독 정보 영속성 해결 가이드

## 문제 상황
로그인한 사용자의 구독 정보가 새로고침 시 사라지는 문제가 있었습니다. 이는 기존에 하드코딩된 임시 데이터를 반환하고 있었기 때문입니다.

## 해결 방법

### 1. 데이터베이스 테이블 생성
```sql
-- ddl_UserNewsletterSubscription.sql 파일 실행
CREATE TABLE IF NOT EXISTS user_newsletter_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    subscribed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    frequency VARCHAR(20),
    send_time VARCHAR(10),
    is_personalized BOOLEAN DEFAULT FALSE,
    keywords TEXT,
    
    UNIQUE KEY uk_user_category (user_id, category)
);
```

### 2. 구현된 컴포넌트

#### A. 엔티티 클래스
- **파일**: `UserNewsletterSubscription.java`
- **기능**: 사용자별 뉴스레터 구독 정보를 나타내는 JPA 엔티티
- **주요 필드**:
  - `userId`: 사용자 ID
  - `category`: 구독 카테고리 (POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE)
  - `isActive`: 구독 활성화 여부
  - `subscribedAt`: 구독 시작일시
  - `frequency`: 발송 빈도
  - `sendTime`: 발송 시간

#### B. Repository 인터페이스
- **파일**: `UserNewsletterSubscriptionRepository.java`
- **기능**: 구독 정보 CRUD 작업을 위한 Spring Data JPA Repository
- **주요 메서드**:
  - `findByUserId(Long userId)`: 사용자별 모든 구독 정보 조회
  - `findByUserIdAndCategory(Long userId, String category)`: 특정 카테고리 구독 정보 조회
  - `updateSubscriptionStatus()`: 구독 상태 업데이트
  - `countActiveSubscribersByCategory()`: 카테고리별 활성 구독자 수 조회

#### C. 컨트롤러 수정
- **파일**: `NewsletterController.java`
- **수정된 메서드**:
  - `getMySubscriptions()`: 실제 데이터베이스에서 구독 정보 조회
  - `toggleSubscription()`: 구독 상태 변경 (새로 추가)
  - `initTestSubscriptionData()`: 테스트용 구독 데이터 초기화 (새로 추가)

### 3. API 엔드포인트

#### A. 구독 목록 조회
```http
GET /api/newsletter/subscription/my
Authorization: Bearer {JWT_TOKEN}
```

**응답 예시**:
```json
{
  "success": true,
  "message": "구독 목록 조회가 완료되었습니다.",
  "data": [
    {
      "categoryId": -1234567890,
      "categoryName": "POLITICS",
      "categoryNameKo": "정치",
      "isActive": true,
      "subscribedAt": "2024-01-15T09:00:00"
    },
    {
      "categoryId": -987654321,
      "categoryName": "ECONOMY",
      "categoryNameKo": "경제",
      "isActive": false,
      "subscribedAt": null
    }
  ]
}
```

#### B. 구독 상태 변경
```http
POST /api/newsletter/subscription/toggle
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "category": "POLITICS",
  "isActive": true
}
```

**응답 예시**:
```json
{
  "success": true,
  "message": "구독이 활성화되었습니다.",
  "data": "구독이 활성화되었습니다."
}
```

#### C. 테스트 데이터 초기화 (개발용)
```http
POST /api/newsletter/subscription/init-test-data
Authorization: Bearer {JWT_TOKEN}
```

### 4. 사용 방법

#### A. 데이터베이스 설정
1. `ddl_UserNewsletterSubscription.sql` 파일을 실행하여 테이블 생성
2. 애플리케이션 재시작

#### B. 테스트 데이터 생성
1. 로그인 후 `/api/newsletter/subscription/init-test-data` API 호출
2. 모든 카테고리에 대한 기본 구독 정보가 생성됨

#### C. 구독 정보 확인
1. `/api/newsletter/subscription/my` API 호출하여 구독 목록 확인
2. 새로고침 후에도 구독 정보가 유지되는지 확인

#### D. 구독 상태 변경
1. `/api/newsletter/subscription/toggle` API로 구독 상태 변경
2. 변경된 상태가 데이터베이스에 저장되는지 확인

### 5. 주요 개선사항

1. **데이터 영속성**: 하드코딩된 임시 데이터 대신 실제 데이터베이스 사용
2. **사용자별 구독 관리**: 각 사용자별로 독립적인 구독 정보 관리
3. **실시간 상태 변경**: 구독 상태를 실시간으로 변경하고 저장
4. **확장성**: 새로운 카테고리나 구독 옵션 추가 용이
5. **데이터 무결성**: 유니크 제약조건으로 중복 구독 방지

### 6. 주의사항

1. **JWT 토큰**: 모든 API는 유효한 JWT 토큰이 필요합니다.
2. **카테고리 코드**: 카테고리는 영어 코드(POLITICS, ECONOMY 등)를 사용합니다.
3. **테스트 데이터**: `init-test-data` API는 개발/테스트 환경에서만 사용하세요.
4. **데이터베이스 연결**: 데이터베이스 연결 설정이 올바른지 확인하세요.

### 7. 트러블슈팅

#### A. 구독 정보가 조회되지 않는 경우
- 데이터베이스 테이블이 생성되었는지 확인
- JWT 토큰이 유효한지 확인
- 사용자 ID가 올바르게 추출되는지 로그 확인

#### B. 구독 상태 변경이 안 되는 경우
- 요청 데이터 형식이 올바른지 확인 (category, isActive 필드)
- 데이터베이스 연결 상태 확인
- Repository 메서드가 올바르게 호출되는지 로그 확인

이제 로그인한 사용자의 구독 정보가 새로고침 시에도 유지되며, 실제 데이터베이스에 저장되어 영속성을 보장합니다.
