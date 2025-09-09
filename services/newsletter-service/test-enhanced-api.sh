#!/bin/bash

# Enhanced 뉴스레터 API 테스트 스크립트
# 사용법: ./test-enhanced-api.sh [base_url] [jwt_token]

BASE_URL=${1:-"http://localhost:8085"}
JWT_TOKEN=${2:-""}

echo "🚀 Enhanced 뉴스레터 API 테스트 시작"
echo "Base URL: $BASE_URL"
echo "JWT Token: ${JWT_TOKEN:0:20}..." # 토큰의 처음 20자만 표시
echo ""

# 헤더 설정
HEADERS="Content-Type: application/json"
if [ ! -z "$JWT_TOKEN" ]; then
    HEADERS="$HEADERS -H Authorization: Bearer $JWT_TOKEN"
fi

# 1. Enhanced 뉴스레터 메인 API 테스트
echo "📰 1. Enhanced 뉴스레터 메인 API 테스트"
echo "GET $BASE_URL/api/newsletter/enhanced"
echo ""

response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=8")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n -1)

echo "HTTP Status: $http_code"
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공"
    echo "$body" | jq '.data | {totalCategories, headlinesPerCategory, trendingKeywords: .trendingKeywords[0:3], userSubscriptionInfo}' 2>/dev/null || echo "$body"
else
    echo "❌ 실패"
    echo "$body"
fi
echo ""

# 2. 카테고리별 상세 정보 API 테스트
echo "📂 2. 카테고리별 상세 정보 API 테스트"
echo "GET $BASE_URL/api/newsletter/enhanced/category/정치"
echo ""

response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced/category/정치?headlinesLimit=10&keywordsLimit=8")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | head -n -1)

echo "HTTP Status: $http_code"
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공"
    echo "$body" | jq '.data | {category, categoryEn, totalHeadlines, totalKeywords, trendingKeywords: .trendingKeywords[0:3], subscriptionStatus}' 2>/dev/null || echo "$body"
else
    echo "❌ 실패"
    echo "$body"
fi
echo ""

# 3. 다양한 카테고리 테스트
echo "🎯 3. 다양한 카테고리 테스트"
categories=("경제" "사회" "IT/과학" "예술")

for category in "${categories[@]}"; do
    echo "테스트 카테고리: $category"
    response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced/category/$category?headlinesLimit=5&keywordsLimit=5")
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 200 ]; then
        echo "✅ $category 성공"
    else
        echo "❌ $category 실패 (HTTP $http_code)"
    fi
done
echo ""

# 4. 파라미터 테스트
echo "⚙️ 4. 파라미터 테스트"
echo "다양한 파라미터로 테스트..."

# 더 많은 헤드라인 요청
echo "더 많은 헤드라인 요청 (headlinesPerCategory=10)"
response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced?headlinesPerCategory=10&trendingKeywordsLimit=5")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공"
else
    echo "❌ 실패 (HTTP $http_code)"
fi

# 더 많은 키워드 요청
echo "더 많은 키워드 요청 (trendingKeywordsLimit=15)"
response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=15")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공"
else
    echo "❌ 실패 (HTTP $http_code)"
fi
echo ""

# 5. 에러 케이스 테스트
echo "🚨 5. 에러 케이스 테스트"

# 잘못된 카테고리
echo "잘못된 카테고리 테스트"
response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced/category/잘못된카테고리")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -eq 200 ]; then
    echo "✅ 예상치 못한 성공"
else
    echo "✅ 예상된 실패 (HTTP $http_code)"
fi

# 잘못된 파라미터
echo "잘못된 파라미터 테스트"
response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced?headlinesPerCategory=-1&trendingKeywordsLimit=abc")
http_code=$(echo "$response" | tail -n1)
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공 (기본값 사용)"
else
    echo "❌ 실패 (HTTP $http_code)"
fi
echo ""

# 6. 성능 테스트
echo "⚡ 6. 성능 테스트"
echo "응답 시간 측정..."

start_time=$(date +%s%N)
response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced?headlinesPerCategory=5&trendingKeywordsLimit=8")
end_time=$(date +%s%N)
http_code=$(echo "$response" | tail -n1)

duration=$(( (end_time - start_time) / 1000000 )) # 밀리초로 변환

echo "응답 시간: ${duration}ms"
if [ "$http_code" -eq 200 ]; then
    echo "✅ 성공"
else
    echo "❌ 실패 (HTTP $http_code)"
fi
echo ""

# 7. 인증 테스트 (JWT 토큰이 있는 경우)
if [ ! -z "$JWT_TOKEN" ]; then
    echo "🔐 7. 인증 테스트"
    echo "JWT 토큰과 함께 요청..."
    
    response=$(curl -s -w "\n%{http_code}" $HEADERS "$BASE_URL/api/newsletter/enhanced")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n -1)
    
    echo "HTTP Status: $http_code"
    if [ "$http_code" -eq 200 ]; then
        echo "✅ 인증 성공"
        # 사용자 구독 정보 확인
        user_info=$(echo "$body" | jq '.data.userSubscriptionInfo' 2>/dev/null)
        if [ "$user_info" != "null" ] && [ "$user_info" != "{}" ]; then
            echo "✅ 사용자 구독 정보 포함"
            echo "$user_info" | jq '.'
        else
            echo "⚠️ 사용자 구독 정보 없음"
        fi
    else
        echo "❌ 인증 실패"
        echo "$body"
    fi
else
    echo "🔐 7. 인증 테스트 건너뜀 (JWT 토큰 없음)"
fi
echo ""

echo "🎉 Enhanced 뉴스레터 API 테스트 완료!"
echo ""
echo "📋 테스트 요약:"
echo "- Enhanced 뉴스레터 메인 API"
echo "- 카테고리별 상세 정보 API"
echo "- 다양한 카테고리 테스트"
echo "- 파라미터 테스트"
echo "- 에러 케이스 테스트"
echo "- 성능 테스트"
echo "- 인증 테스트 (토큰 제공 시)"
echo ""
echo "💡 사용법:"
echo "  ./test-enhanced-api.sh [base_url] [jwt_token]"
echo "  예시: ./test-enhanced-api.sh http://localhost:8085 eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
