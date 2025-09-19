CREATE TABLE news_user_interaction
(
    id               BIGINT AUTO_INCREMENT NOT NULL,
    user_id          BIGINT                NOT NULL,
    news_id          BIGINT                NOT NULL,
    category         VARCHAR(255)          NOT NULL,
    interaction_type VARCHAR(255)          NOT NULL,
    reading_duration INT                   NULL,
    created_at       datetime              NOT NULL,
    CONSTRAINT pk_news_user_interaction PRIMARY KEY (id)
);