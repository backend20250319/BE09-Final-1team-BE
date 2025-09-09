#!/bin/bash

# 뉴스레터 테스트 스크립트
# 뉴스 서비스에서 뉴스를 가져와서 뉴스레터를 전송하는 기능을 테스트합니다.

echo "🚀 뉴스레터 테스트 시작"
echo "================================"

# 기본 설정
NEWSLETTER_SERVICE_URL="http://localhost:8085"
NEWS_SERVICE_URL="http://localhost:8082"

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수 정의
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 1. 서비스 상태 확인
echo ""
echo "1. 서비스 상태 확인"
echo "-------------------"

# 뉴스 서비스 상태 확인
print_info "뉴스 서비스 상태 확인 중..."
if curl -s -f "$NEWS_SERVICE_URL/actuator/health" > /dev/null; then
    print_success "뉴스 서비스 연결 성공"
else
    print_error "뉴스 서비스 연결 실패"
    echo "뉴스 서비스가 실행 중인지 확인해주세요: $NEWS_SERVICE_URL"
    exit 1
fi

# 뉴스레터 서비스 상태 확인
print_info "뉴스레터 서비스 상태 확인 중..."
if curl -s -f "$NEWSLETTER_SERVICE_URL/actuator/health" > /dev/null; then
    print_success "뉴스레터 서비스 연결 성공"
else
    print_error "뉴스레터 서비스 연결 실패"
    echo "뉴스레터 서비스가 실행 중인지 확인해주세요: $NEWSLETTER_SERVICE_URL"
    exit 1
fi

# 2. 뉴스 데이터 조회 테스트
echo ""
echo "2. 뉴스 데이터 조회 테스트"
echo "-------------------------"

# 트렌딩 뉴스 데이터 조회
print_info "트렌딩 뉴스 데이터 조회 중..."
TRENDING_RESPONSE=$(curl -s "$NEWSLETTER_SERVICE_URL/api/test/newsletter/news-data?type=trending")
if echo "$TRENDING_RESPONSE" | grep -q '"success":true'; then
    print_success "트렌딩 뉴스 데이터 조회 성공"
    echo "$TRENDING_RESPONSE" | jq '.data.feedTemplate.content.title' 2>/dev/null || echo "제목: $(echo "$TRENDING_RESPONSE" | grep -o '"title":"[^"]*"' | head -1)"
else
    print_error "트렌딩 뉴스 데이터 조회 실패"
    echo "$TRENDING_RESPONSE"
fi

# 개인화 뉴스 데이터 조회
print_info "개인화 뉴스 데이터 조회 중..."
PERSONALIZED_RESPONSE=$(curl -s "$NEWSLETTER_SERVICE_URL/api/test/newsletter/news-data?type=personalized&param=1")
if echo "$PERSONALIZED_RESPONSE" | grep -q '"success":true'; then
    print_success "개인화 뉴스 데이터 조회 성공"
else
    print_error "개인화 뉴스 데이터 조회 실패"
    echo "$PERSONALIZED_RESPONSE"
fi

# 카테고리별 뉴스 데이터 조회
print_info "카테고리별 뉴스 데이터 조회 중..."
CATEGORY_RESPONSE=$(curl -s "$NEWSLETTER_SERVICE_URL/api/test/newsletter/news-data?type=category&param=정치")
if echo "$CATEGORY_RESPONSE" | grep -q '"success":true'; then
    print_success "카테고리별 뉴스 데이터 조회 성공"
else
    print_error "카테고리별 뉴스 데이터 조회 실패"
    echo "$CATEGORY_RESPONSE"
fi

# 3. 뉴스레터 전송 테스트 (시뮬레이션)
echo ""
echo "3. 뉴스레터 전송 테스트 (시뮬레이션)"
echo "----------------------------------"

# 트렌딩 뉴스레터 전송 테스트
print_info "트렌딩 뉴스레터 전송 테스트 중..."
SEND_TEST_RESPONSE=$(curl -s -X POST "$NEWSLETTER_SERVICE_URL/api/test/newsletter/send-test?type=trending&testUserId=1")
if echo "$SEND_TEST_RESPONSE" | grep -q '"success":true'; then
    print_success "트렌딩 뉴스레터 전송 테스트 성공"
else
    print_error "트렌딩 뉴스레터 전송 테스트 실패"
    echo "$SEND_TEST_RESPONSE"
fi

# 4. 뉴스 서비스 직접 테스트
echo ""
echo "4. 뉴스 서비스 직접 테스트"
echo "-------------------------"

# 뉴스 서비스에서 직접 뉴스 조회
print_info "뉴스 서비스에서 직접 뉴스 조회 중..."
DIRECT_NEWS_RESPONSE=$(curl -s "$NEWS_SERVICE_URL/api/trending?limit=5")
if echo "$DIRECT_NEWS_RESPONSE" | grep -q '"success":true'; then
    print_success "뉴스 서비스 직접 조회 성공"
    NEWS_COUNT=$(echo "$DIRECT_NEWS_RESPONSE" | jq '.data.content | length' 2>/dev/null || echo "0")
    print_info "조회된 뉴스 개수: $NEWS_COUNT"
else
    print_error "뉴스 서비스 직접 조회 실패"
    echo "$DIRECT_NEWS_RESPONSE"
fi

# 5. 결과 요약
echo ""
echo "5. 테스트 결과 요약"
echo "------------------"

# 성공/실패 카운트
SUCCESS_COUNT=0
TOTAL_TESTS=5

# 각 테스트 결과 확인
if curl -s -f "$NEWS_SERVICE_URL/actuator/health" > /dev/null; then
    ((SUCCESS_COUNT++))
fi

if curl -s -f "$NEWSLETTER_SERVICE_URL/actuator/health" > /dev/null; then
    ((SUCCESS_COUNT++))
fi

if echo "$TRENDING_RESPONSE" | grep -q '"success":true'; then
    ((SUCCESS_COUNT++))
fi

if echo "$SEND_TEST_RESPONSE" | grep -q '"success":true'; then
    ((SUCCESS_COUNT++))
fi

if echo "$DIRECT_NEWS_RESPONSE" | grep -q '"success":true'; then
    ((SUCCESS_COUNT++))
fi

# 결과 출력
echo "총 테스트: $TOTAL_TESTS"
echo "성공: $SUCCESS_COUNT"
echo "실패: $((TOTAL_TESTS - SUCCESS_COUNT))"

if [ $SUCCESS_COUNT -eq $TOTAL_TESTS ]; then
    print_success "모든 테스트 통과! 뉴스 서비스에서 뉴스를 가져와서 뉴스레터를 전송할 수 있습니다."
else
    print_warning "일부 테스트 실패. 위의 오류 메시지를 확인해주세요."
fi

echo ""
echo "🎯 다음 단계:"
echo "1. 실제 카카오톡 토큰으로 뉴스레터 전송 테스트"
echo "2. 프론트엔드에서 미리보기 페이지 테스트"
echo "3. 자동 전송 스케줄러 테스트"

echo ""
echo "📚 추가 정보:"
echo "- API 문서: http://localhost:8085/swagger-ui.html"
echo "- 테스트 가이드: services/newsletter-service/NEWSLETTER_TEST_GUIDE.md"
echo "- 프론트엔드 가이드: services/newsletter-service/FRONTEND_API_GUIDE.md"


