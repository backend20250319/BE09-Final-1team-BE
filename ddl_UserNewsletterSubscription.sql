-- 사용자별 뉴스레터 구독 정보 테이블 생성
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
    
    -- 인덱스 추가
    INDEX idx_user_id (user_id),
    INDEX idx_category (category),
    INDEX idx_user_category (user_id, category),
    INDEX idx_is_active (is_active),
    INDEX idx_subscribed_at (subscribed_at),
    
    -- 유니크 제약조건 제거 (사용자당 카테고리별 여러 구독 허용)
    -- UNIQUE KEY uk_user_category (user_id, category)
);

-- 댓글
COMMENT ON TABLE user_newsletter_subscriptions IS '사용자별 뉴스레터 구독 정보';
COMMENT ON COLUMN user_newsletter_subscriptions.id IS '구독 ID (PK)';
COMMENT ON COLUMN user_newsletter_subscriptions.user_id IS '사용자 ID';
COMMENT ON COLUMN user_newsletter_subscriptions.category IS '구독 카테고리 (POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE)';
COMMENT ON COLUMN user_newsletter_subscriptions.is_active IS '구독 활성화 여부';
COMMENT ON COLUMN user_newsletter_subscriptions.subscribed_at IS '구독 시작일시';
COMMENT ON COLUMN user_newsletter_subscriptions.updated_at IS '수정일시';
COMMENT ON COLUMN user_newsletter_subscriptions.frequency IS '발송 빈도 (DAILY, WEEKLY, MONTHLY)';
COMMENT ON COLUMN user_newsletter_subscriptions.send_time IS '발송 시간 (HH:MM 형식)';
COMMENT ON COLUMN user_newsletter_subscriptions.is_personalized IS '개인화 여부';
COMMENT ON COLUMN user_newsletter_subscriptions.keywords IS '관심 키워드 (JSON 형식)';
