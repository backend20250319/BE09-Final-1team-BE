# 웹 푸시 알림 구현 가이드

## 개요
뉴스레터 서비스에 웹 푸시 알림 기능을 구현했습니다. Firebase Cloud Messaging(FCM)을 사용하여 사용자에게 실시간 알림을 전송할 수 있습니다.

## 구현된 컴포넌트

### 1. 모델 클래스
- **PushSubscription**: 웹 푸시 구독 정보를 나타내는 모델
- **PushMessage**: 푸시 알림 메시지를 나타내는 모델

### 2. 서비스 클래스
- **WebPushService**: FCM을 사용한 웹 푸시 알림 전송 서비스
- **UserService**: 사용자 서비스 클라이언트를 통한 구독자 조회 서비스
- **FirebaseConfig**: Firebase Admin SDK 초기화 설정

### 3. 컨트롤러
- **WebPushController**: 웹 푸시 알림 관리 API 엔드포인트

### 4. 클라이언트 인터페이스
- **UserServiceClient**: 사용자 서비스와의 통신을 위한 Feign 클라이언트

## 주요 기능

### 1. 웹 푸시 알림 전송
```java
// 단일 구독자에게 푸시 알림 전송
boolean success = webPushService.sendNotification(subscription, message);

// 여러 구독자에게 배치로 푸시 알림 전송
int successCount = webPushService.sendBulkNotification(subscriptions, message);

// 비동기로 푸시 알림 전송
CompletableFuture<Integer> future = webPushService.sendBulkNotificationAsync(subscriptions, message);
```

### 2. 뉴스레터 통합
KakaoMessageService의 `sendWebPushNotification` 메서드가 구현되어 뉴스레터 전송 시 자동으로 웹 푸시 알림도 함께 전송됩니다.

### 3. 배치 처리
- 최대 500명씩 배치로 나누어 전송
- FCM 제한을 고려한 배치 간 간격 설정
- 비동기 처리로 성능 최적화

## 설정

### 1. 의존성 추가
```gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
implementation 'com.google.guava:guava:32.1.3-jre'
```

### 2. 환경 변수 설정
```yaml
# 웹 푸시 알림 설정
webpush:
  vapid:
    public-key: ${WEBPUSH_VAPID_PUBLIC_KEY:}
    private-key: ${WEBPUSH_VAPID_PRIVATE_KEY:}
  batch:
    size: 500
  retry:
    attempts: 3

# Firebase 설정
firebase:
  config:
    path: firebase-service-account.json
  project:
    id: ${FIREBASE_PROJECT_ID:}
```

### 3. Firebase 서비스 계정 키
`src/main/resources/firebase-service-account.json` 파일에 Firebase 서비스 계정 키를 배치해야 합니다.

## API 엔드포인트

### 1. 테스트 푸시 알림 전송
```
POST /api/webpush/test
Parameters:
- title: 알림 제목
- body: 알림 내용
```

### 2. 뉴스레터 푸시 알림 전송
```
POST /api/webpush/newsletter
Parameters:
- title: 뉴스레터 제목
- summary: 뉴스레터 요약
- newsletterId: 뉴스레터 ID
```

### 3. 푸시 알림 통계 조회
```
GET /api/webpush/stats
```

### 4. 구독자 수 조회
```
GET /api/webpush/subscribers/count
```

## 사용자 서비스 연동

UserServiceClient를 통해 다음 API를 호출합니다:

### 1. 웹 푸시 구독자 목록 조회
```
GET /api/users/webpush/subscriptions
```

### 2. 사용자별 웹 푸시 구독 정보 조회
```
GET /api/users/{userId}/webpush/subscription
```

### 3. 웹 푸시 구독 등록
```
POST /api/users/{userId}/webpush/subscription
```

### 4. 웹 푸시 구독 해제
```
DELETE /api/users/{userId}/webpush/subscription
```

## 에러 처리

### 1. 토큰 유효성 검사
- 유효하지 않은 FCM 토큰 자동 감지
- 만료된 구독 자동 비활성화

### 2. 재시도 메커니즘
- 전송 실패 시 최대 3회 재시도
- 배치 전송 실패 시 개별 전송으로 폴백

### 3. 로깅
- 상세한 로그로 전송 상태 추적
- 성공/실패 통계 제공

## 보안 고려사항

### 1. VAPID 키 관리
- 공개 키는 클라이언트에서 사용
- 개인 키는 서버에서만 사용
- 환경 변수로 안전하게 관리

### 2. 토큰 보안
- FCM 토큰은 암호화하여 저장
- 정기적인 토큰 갱신 처리

## 모니터링

### 1. 전송 통계
- 총 전송 수
- 성공/실패 수
- 성공률 계산

### 2. 성능 모니터링
- 배치 전송 시간 측정
- FCM 응답 시간 추적

## 향후 개선 사항

1. **푸시 알림 템플릿 관리**: 다양한 알림 템플릿 지원
2. **개인화된 알림**: 사용자 선호도 기반 알림 전송
3. **알림 스케줄링**: 특정 시간에 알림 전송
4. **A/B 테스트**: 알림 메시지 효과 측정
5. **다국어 지원**: 여러 언어로 알림 전송

## 문제 해결

### 1. Firebase 초기화 실패
- 서비스 계정 키 파일 경로 확인
- 프로젝트 ID 설정 확인

### 2. 푸시 전송 실패
- VAPID 키 설정 확인
- 네트워크 연결 상태 확인
- FCM 할당량 확인

### 3. 구독자 조회 실패
- UserService 연결 상태 확인
- API 엔드포인트 설정 확인
