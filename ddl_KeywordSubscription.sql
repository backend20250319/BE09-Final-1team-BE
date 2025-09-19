-- 키워드 구독 테이블 생성
CREATE TABLE IF NOT EXISTS keyword_subscription (
    subscription_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    keyword TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- 인덱스 추가
    INDEX idx_user_id (user_id),
    INDEX idx_keyword (keyword(255)),
    INDEX idx_user_keyword (user_id, keyword(255)),
    INDEX idx_is_active (is_active)
);

-- 댓글
COMMENT ON TABLE keyword_subscription IS '키워드 구독 정보';
COMMENT ON COLUMN keyword_subscription.subscription_id IS '구독 ID (PK)';
COMMENT ON COLUMN keyword_subscription.user_id IS '사용자 ID';
COMMENT ON COLUMN keyword_subscription.keyword IS '구독 키워드';
COMMENT ON COLUMN keyword_subscription.is_active IS '활성화 여부';
COMMENT ON COLUMN keyword_subscription.created_at IS '생성일시';
COMMENT ON COLUMN keyword_subscription.updated_at IS '수정일시';
