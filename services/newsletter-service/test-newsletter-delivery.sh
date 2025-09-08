#!/bin/bash

# 뉴스레터 발송 테스트 스크립트
# 사용법: ./test-newsletter-delivery.sh

echo "🚀 뉴스레터 발송 테스트 시작"
echo "================================"

# 기본 설정
BASE_URL="http://localhost:8083"
API_BASE="/api/newsletter"

# JWT 토큰 (실제 사용 시 유효한 토큰으로 교체 필요)
JWT_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

echo "📋 1. 뉴스레터 발송 테스트 (기본)"
curl -X POST "${BASE_URL}${API_BASE}/delivery/test" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.'

echo -e "\n📧 2. 뉴스레터 이메일 발송 테스트"
curl -X POST "${BASE_URL}${API_BASE}/delivery/test-email" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.'

echo -e "\n💬 3. 뉴스레터 카카오톡 발송 테스트"
curl -X POST "${BASE_URL}${API_BASE}/delivery/test-kakao" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.'

echo -e "\n📊 4. 뉴스레터 발송 통계 조회"
curl -X GET "${BASE_URL}${API_BASE}/delivery/stats" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.'

echo -e "\n📈 5. 뉴스레터 발송 상태 확인 (예시 ID: 1)"
curl -X GET "${BASE_URL}${API_BASE}/delivery/status/1" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -w "\nHTTP Status: %{http_code}\n" \
  -s | jq '.'

echo -e "\n✅ 뉴스레터 발송 테스트 완료"
echo "================================"
echo "💡 참고사항:"
echo "   - 이메일 발송은 실제 이메일 서비스 설정이 필요합니다"
echo "   - 카카오톡 발송은 시뮬레이션 모드로 실행됩니다"
echo "   - JWT 토큰을 실제 유효한 토큰으로 교체해주세요"
