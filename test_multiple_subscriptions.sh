#!/bin/bash

# 카테고리별 다중 구독 테스트 스크립트
# 사용법: ./test_multiple_subscriptions.sh [BASE_URL] [JWT_TOKEN]

BASE_URL=${1:-"http://localhost:8080"}
JWT_TOKEN=${2:-"your_jwt_token_here"}

echo "=== 카테고리별 다중 구독 테스트 ==="
echo "Base URL: $BASE_URL"
echo ""

# 테스트용 카테고리
CATEGORY="POLITICS"

echo "1. 첫 번째 구독 생성 (일반 설정)"
curl -X POST "$BASE_URL/api/newsletter/subscription-management/subscribe" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "category": "'$CATEGORY'",
    "frequency": "DAILY",
    "sendTime": "09:00",
    "isPersonalized": false
  }' | jq '.'

echo -e "\n2. 두 번째 구독 생성 (개인화 설정)"
curl -X POST "$BASE_URL/api/newsletter/subscription-management/subscribe" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "category": "'$CATEGORY'",
    "frequency": "WEEKLY",
    "sendTime": "18:00",
    "isPersonalized": true,
    "keywords": "정치,선거,국회"
  }' | jq '.'

echo -e "\n3. 세 번째 구독 생성 (다른 빈도)"
curl -X POST "$BASE_URL/api/newsletter/subscription-management/subscribe" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "category": "'$CATEGORY'",
    "frequency": "MONTHLY",
    "sendTime": "12:00",
    "isPersonalized": false
  }' | jq '.'

echo -e "\n4. 카테고리별 구독 목록 조회"
curl -X GET "$BASE_URL/api/newsletter/subscription-management/category/$CATEGORY" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo -e "\n5. 구독 여부 확인 (상세 정보)"
curl -X GET "$BASE_URL/api/newsletter/subscription-management/check/$CATEGORY" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo -e "\n6. 전체 구독 목록 조회"
curl -X GET "$BASE_URL/api/newsletter/subscription-management/list" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo -e "\n7. 활성 구독 목록 조회"
curl -X GET "$BASE_URL/api/newsletter/subscription-management/list/active" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo -e "\n8. 카테고리별 구독 통계"
curl -X GET "$BASE_URL/api/newsletter/subscription-management/stats/by-category" \
  -H "Authorization: Bearer $JWT_TOKEN" | jq '.'

echo -e "\n=== 테스트 완료 ==="
echo "다중 구독이 정상적으로 생성되고 조회되는지 확인하세요."
echo "각 구독이 서로 다른 설정을 가지고 있는지 확인하세요."
