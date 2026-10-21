-- V4: 文章阅读事件表（可审计、可去重的阅读统计）
CREATE TABLE mh_article_view_event (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    article_id  BIGINT UNSIGNED NOT NULL,
    viewer_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT 'Anonymous viewer/visitor hash for dedup',
    view_date   DATE NOT NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mh_article_view_event_article_date (article_id, view_date),
    KEY idx_mh_article_view_event_date (view_date),
    CONSTRAINT fk_mh_article_view_event_article FOREIGN KEY (article_id) REFERENCES mh_article (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Deduplicatable article view events';
