-- 카테고리별 다중 구독을 허용하기 위한 마이그레이션 스크립트
-- 기존 UNIQUE 제약조건을 제거하여 사용자당 카테고리별 여러 구독이 가능하도록 변경

-- 1. 기존 UNIQUE 제약조건 제거
ALTER TABLE user_newsletter_subscriptions DROP INDEX IF EXISTS uk_user_category;

-- 2. 인덱스는 유지 (성능을 위해)
-- idx_user_id, idx_category, idx_user_category, idx_is_active, idx_subscribed_at 인덱스는 그대로 유지

-- 3. 변경사항 확인
SHOW INDEX FROM user_newsletter_subscriptions;

-- 4. 테이블 구조 확인
DESCRIBE user_newsletter_subscriptions;

-- 주의사항:
-- - 이 마이그레이션은 기존 데이터에 영향을 주지 않습니다
-- - UNIQUE 제약조건만 제거하고 다른 제약조건은 유지됩니다
-- - 인덱스는 성능을 위해 그대로 유지됩니다
-- - 마이그레이션 후 카테고리별 다중 구독이 가능해집니다
