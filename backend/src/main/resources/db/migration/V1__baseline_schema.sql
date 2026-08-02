-- MindHaven baseline schema for MySQL 8.0+.
-- All tables use InnoDB and utf8mb4. Application timestamps are stored in UTC
-- or in the timezone explicitly agreed by the deployment configuration.

-- -----------------------------------------------------------------------------
-- Identity and access control
-- -----------------------------------------------------------------------------

CREATE TABLE mh_user (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username                VARCHAR(50) NOT NULL,
    email                   VARCHAR(254) NOT NULL,
    password_hash           VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    nickname                VARCHAR(80) NULL,
    phone                   VARCHAR(32) NULL COMMENT 'Sensitive field; encrypt or tokenize at application boundary',
    avatar_file_id          BIGINT UNSIGNED NULL COMMENT 'Logical reference to mh_file_object; no FK avoids circular creation dependency',
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    privacy_consent_version VARCHAR(30) NULL,
    consented_at            DATETIME(3) NULL,
    last_login_at           DATETIME(3) NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at              DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_user_username (username),
    UNIQUE KEY uk_mh_user_email (email),
    KEY idx_mh_user_status_created (status, created_at),
    CONSTRAINT chk_mh_user_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED', 'PENDING'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User accounts';

CREATE TABLE mh_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name        VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    built_in    TINYINT(1) NOT NULL DEFAULT 0,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_role_code (code),
    CONSTRAINT chk_mh_role_built_in CHECK (built_in IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC roles';

CREATE TABLE mh_permission (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='RBAC permissions';

CREATE TABLE mh_user_role (
    user_id    BIGINT UNSIGNED NOT NULL,
    role_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_by BIGINT UNSIGNED NULL,
    PRIMARY KEY (user_id, role_id),
    KEY idx_mh_user_role_role (role_id, user_id),
    CONSTRAINT fk_mh_user_role_user FOREIGN KEY (user_id) REFERENCES mh_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_mh_user_role_role FOREIGN KEY (role_id) REFERENCES mh_role (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mh_user_role_creator FOREIGN KEY (created_by) REFERENCES mh_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='User to role assignments';

CREATE TABLE mh_role_permission (
    role_id       BIGINT UNSIGNED NOT NULL,
    permission_id BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (role_id, permission_id),
    KEY idx_mh_role_permission_permission (permission_id, role_id),
    CONSTRAINT fk_mh_role_permission_role FOREIGN KEY (role_id) REFERENCES mh_role (id) ON DELETE CASCADE,
    CONSTRAINT fk_mh_role_permission_permission FOREIGN KEY (permission_id) REFERENCES mh_permission (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Role to permission assignments';

CREATE TABLE mh_auth_session (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    token_hash        CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT 'SHA-256 or equivalent digest of opaque refresh/session token',
    device_summary    VARCHAR(255) NULL,
    client_ip_hash    CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    expires_at        DATETIME(3) NOT NULL,
    last_used_at      DATETIME(3) NULL,
    revoked_at        DATETIME(3) NULL,
    revoke_reason     VARCHAR(100) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_auth_session_token_hash (token_hash),
    KEY idx_mh_auth_session_user_expiry (user_id, expires_at),
    KEY idx_mh_auth_session_expiry (expires_at),
    CONSTRAINT fk_mh_auth_session_user FOREIGN KEY (user_id) REFERENCES mh_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Revocable authentication sessions';

-- -----------------------------------------------------------------------------
-- Emotion diary
-- -----------------------------------------------------------------------------

CREATE TABLE mh_emotion_diary (
    id                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id               BIGINT UNSIGNED NOT NULL,
    diary_date            DATE NOT NULL,
    mood_score            TINYINT UNSIGNED NOT NULL,
    dominant_emotion      VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    emotion_triggers      TEXT NULL,
    content               TEXT NOT NULL,
    sleep_quality         TINYINT UNSIGNED NOT NULL,
    stress_level          TINYINT UNSIGNED NOT NULL,
    version               INT UNSIGNED NOT NULL DEFAULT 0,
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at            DATETIME(3) NULL,
    active_diary_date     DATE GENERATED ALWAYS AS (
        CASE WHEN deleted_at IS NULL THEN diary_date ELSE NULL END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_emotion_diary_user_active_date (user_id, active_diary_date),
    KEY idx_mh_emotion_diary_user_date (user_id, diary_date DESC),
    KEY idx_mh_emotion_diary_user_score (user_id, mood_score),
    CONSTRAINT fk_mh_emotion_diary_user FOREIGN KEY (user_id) REFERENCES mh_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_mh_emotion_diary_mood_score CHECK (mood_score BETWEEN 1 AND 10),
    CONSTRAINT chk_mh_emotion_diary_sleep_quality CHECK (sleep_quality BETWEEN 1 AND 5),
    CONSTRAINT chk_mh_emotion_diary_stress_level CHECK (stress_level BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sensitive user emotion diary';

-- -----------------------------------------------------------------------------
-- AI chat
-- -----------------------------------------------------------------------------

CREATE TABLE mh_chat_session (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    title             VARCHAR(200) NOT NULL DEFAULT '新会话',
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    message_count     INT UNSIGNED NOT NULL DEFAULT 0,
    last_message_at   DATETIME(3) NULL,
    started_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ended_at          DATETIME(3) NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at        DATETIME(3) NULL,
    PRIMARY KEY (id),
    KEY idx_mh_chat_session_user_last_message (user_id, last_message_at DESC),
    KEY idx_mh_chat_session_status_updated (status, updated_at),
    CONSTRAINT fk_mh_chat_session_user FOREIGN KEY (user_id) REFERENCES mh_user (id) ON DELETE RESTRICT,
    CONSTRAINT chk_mh_chat_session_status CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI companion chat sessions';

CREATE TABLE mh_chat_message (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id        BIGINT UNSIGNED NOT NULL,
    sender_type       VARCHAR(16) NOT NULL,
    content           LONGTEXT NOT NULL,
    client_request_id VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    finish_reason     VARCHAR(30) NULL,
    model_provider    VARCHAR(50) NULL,
    model_version     VARCHAR(100) NULL,
    input_tokens      INT UNSIGNED NULL,
    output_tokens     INT UNSIGNED NULL,
    created_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at      DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_chat_message_idempotency (session_id, client_request_id, sender_type),
    KEY idx_mh_chat_message_session_created (session_id, created_at, id),
    KEY idx_mh_chat_message_status_created (status, created_at),
    CONSTRAINT fk_mh_chat_message_session FOREIGN KEY (session_id) REFERENCES mh_chat_session (id) ON DELETE CASCADE,
    CONSTRAINT chk_mh_chat_message_sender CHECK (sender_type IN ('USER', 'ASSISTANT', 'SYSTEM')),
    CONSTRAINT chk_mh_chat_message_status CHECK (status IN ('PENDING', 'STREAMING', 'COMPLETED', 'FAILED', 'BLOCKED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Sensitive chat messages';

CREATE TABLE mh_emotion_analysis (
    id                           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_type                  VARCHAR(20) NOT NULL,
    source_id                    BIGINT UNSIGNED NOT NULL,
    emotion_score                TINYINT UNSIGNED NULL,
    label                        VARCHAR(32) CHARACTER SET ascii COLLATE ascii_bin NULL,
    is_negative                  TINYINT(1) NULL,
    risk_level                   VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    risk_description             TEXT NULL,
    suggestion                   TEXT NULL,
    improvement_suggestions_json JSON NULL,
    model_provider               VARCHAR(50) NULL,
    model_version                VARCHAR(100) NULL,
    policy_version               VARCHAR(50) NULL,
    status                       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    current_result               TINYINT(1) NOT NULL DEFAULT 1,
    analyzed_at                  DATETIME(3) NULL,
    created_at                   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mh_emotion_analysis_source (source_type, source_id, current_result),
    KEY idx_mh_emotion_analysis_risk (risk_level, analyzed_at),
    CONSTRAINT chk_mh_emotion_analysis_source CHECK (source_type IN ('DIARY', 'CHAT_SESSION')),
    CONSTRAINT chk_mh_emotion_analysis_score CHECK (emotion_score IS NULL OR emotion_score BETWEEN 0 AND 100),
    CONSTRAINT chk_mh_emotion_analysis_negative CHECK (is_negative IS NULL OR is_negative IN (0, 1)),
    CONSTRAINT chk_mh_emotion_analysis_risk CHECK (risk_level IN ('NORMAL', 'CONCERN', 'WARNING', 'CRISIS')),
    CONSTRAINT chk_mh_emotion_analysis_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'SUPERSEDED')),
    CONSTRAINT chk_mh_emotion_analysis_current CHECK (current_result IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Versioned AI emotion analysis results';

-- -----------------------------------------------------------------------------
-- Files and knowledge content
-- -----------------------------------------------------------------------------

CREATE TABLE mh_file_object (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    storage_provider VARCHAR(30) NOT NULL,
    storage_key      VARCHAR(512) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    original_name    VARCHAR(255) NOT NULL,
    content_type     VARCHAR(127) NOT NULL,
    size_bytes       BIGINT UNSIGNED NOT NULL,
    sha256           CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    business_type    VARCHAR(30) NULL,
    business_id      BIGINT UNSIGNED NULL,
    business_field   VARCHAR(50) NULL,
    owner_user_id    BIGINT UNSIGNED NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'TEMP',
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted_at       DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_file_object_storage_key (storage_key),
    KEY idx_mh_file_object_business (business_type, business_id, business_field),
    KEY idx_mh_file_object_owner_created (owner_user_id, created_at),
    KEY idx_mh_file_object_status_created (status, created_at),
    CONSTRAINT fk_mh_file_object_owner FOREIGN KEY (owner_user_id) REFERENCES mh_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_mh_file_object_status CHECK (status IN ('TEMP', 'BOUND', 'QUARANTINED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File metadata; binary content lives in object storage';

CREATE TABLE mh_knowledge_category (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parent_id  BIGINT UNSIGNED NULL,
    name       VARCHAR(80) NOT NULL,
    slug       VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_knowledge_category_slug (slug),
    KEY idx_mh_knowledge_category_parent_sort (parent_id, sort_order, id),
    CONSTRAINT fk_mh_knowledge_category_parent FOREIGN KEY (parent_id) REFERENCES mh_knowledge_category (id) ON DELETE RESTRICT,
    CONSTRAINT chk_mh_knowledge_category_status CHECK (status IN ('ACTIVE', 'DISABLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge article categories';

CREATE TABLE mh_article (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    category_id    BIGINT UNSIGNED NOT NULL,
    author_user_id BIGINT UNSIGNED NOT NULL,
    title          VARCHAR(200) NOT NULL,
    slug           VARCHAR(220) CHARACTER SET ascii COLLATE ascii_bin NULL,
    summary        VARCHAR(1000) NULL,
    content_html   LONGTEXT NOT NULL COMMENT 'Server-sanitized HTML only',
    cover_file_id  BIGINT UNSIGNED NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    view_count     BIGINT UNSIGNED NOT NULL DEFAULT 0,
    published_at   DATETIME(3) NULL,
    version        INT UNSIGNED NOT NULL DEFAULT 0,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at     DATETIME(3) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_article_slug (slug),
    KEY idx_mh_article_status_published (status, published_at DESC),
    KEY idx_mh_article_category_status_published (category_id, status, published_at DESC),
    KEY idx_mh_article_author_created (author_user_id, created_at DESC),
    CONSTRAINT fk_mh_article_category FOREIGN KEY (category_id) REFERENCES mh_knowledge_category (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mh_article_author FOREIGN KEY (author_user_id) REFERENCES mh_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mh_article_cover FOREIGN KEY (cover_file_id) REFERENCES mh_file_object (id) ON DELETE SET NULL,
    CONSTRAINT chk_mh_article_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'OFFLINE', 'ARCHIVED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge articles';

CREATE TABLE mh_tag (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50) NOT NULL,
    slug       VARCHAR(70) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_mh_tag_name (name),
    UNIQUE KEY uk_mh_tag_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Knowledge article tags';

CREATE TABLE mh_article_tag (
    article_id BIGINT UNSIGNED NOT NULL,
    tag_id     BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (article_id, tag_id),
    KEY idx_mh_article_tag_tag (tag_id, article_id),
    CONSTRAINT fk_mh_article_tag_article FOREIGN KEY (article_id) REFERENCES mh_article (id) ON DELETE CASCADE,
    CONSTRAINT fk_mh_article_tag_tag FOREIGN KEY (tag_id) REFERENCES mh_tag (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Article to tag relations';

-- -----------------------------------------------------------------------------
-- Psychological safety and audit
-- -----------------------------------------------------------------------------

CREATE TABLE mh_risk_event (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    source_type      VARCHAR(20) NOT NULL,
    source_id        BIGINT UNSIGNED NOT NULL,
    risk_level       VARCHAR(20) NOT NULL,
    trigger_codes_json JSON NULL,
    redacted_summary TEXT NULL,
    policy_version   VARCHAR(50) NULL,
    model_version    VARCHAR(100) NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    assigned_to      BIGINT UNSIGNED NULL,
    resolved_by      BIGINT UNSIGNED NULL,
    resolution_note  TEXT NULL,
    detected_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    resolved_at      DATETIME(3) NULL,
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mh_risk_event_queue (status, risk_level, detected_at),
    KEY idx_mh_risk_event_user_detected (user_id, detected_at DESC),
    KEY idx_mh_risk_event_source (source_type, source_id),
    CONSTRAINT fk_mh_risk_event_user FOREIGN KEY (user_id) REFERENCES mh_user (id) ON DELETE RESTRICT,
    CONSTRAINT fk_mh_risk_event_assignee FOREIGN KEY (assigned_to) REFERENCES mh_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_mh_risk_event_resolver FOREIGN KEY (resolved_by) REFERENCES mh_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_mh_risk_event_source CHECK (source_type IN ('CHAT_MESSAGE', 'EMOTION_DIARY')),
    CONSTRAINT chk_mh_risk_event_level CHECK (risk_level IN ('CONCERN', 'WARNING', 'CRISIS')),
    CONSTRAINT chk_mh_risk_event_status CHECK (status IN ('OPEN', 'REVIEWING', 'RESOLVED', 'FALSE_POSITIVE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Minimal, controlled psychological risk event records';

CREATE TABLE mh_audit_log (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    actor_user_id    BIGINT UNSIGNED NULL,
    action           VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resource_type    VARCHAR(50) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    resource_id      VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    result           VARCHAR(20) NOT NULL,
    reason           VARCHAR(500) NULL,
    request_id       VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    client_ip_hash   CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    user_agent_summary VARCHAR(255) NULL,
    metadata_json    JSON NULL COMMENT 'Must not contain passwords, tokens, full diary text, or full chat messages',
    created_at       DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_mh_audit_log_actor_created (actor_user_id, created_at DESC),
    KEY idx_mh_audit_log_resource_created (resource_type, resource_id, created_at DESC),
    KEY idx_mh_audit_log_action_created (action, created_at DESC),
    KEY idx_mh_audit_log_request (request_id),
    CONSTRAINT fk_mh_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES mh_user (id) ON DELETE SET NULL,
    CONSTRAINT chk_mh_audit_log_result CHECK (result IN ('SUCCESS', 'DENIED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Append-only security and administration audit trail';
