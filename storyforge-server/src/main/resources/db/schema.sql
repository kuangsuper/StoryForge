-- Toonflow Database Schema
-- MySQL 8.0+, utf8mb4, snake_case

-- ========== 用户模块 ==========

CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    password    VARCHAR(128) NOT NULL,
    email       VARCHAR(128) DEFAULT NULL,
    avatar      VARCHAR(512) DEFAULT NULL,
    status      INT          DEFAULT 1 COMMENT '0=禁用 1=启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_user_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    role        VARCHAR(32)  NOT NULL DEFAULT 'viewer',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_user_quota (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT   NOT NULL,
    daily_chapter_limit INT      DEFAULT 50,
    daily_image_limit   INT      DEFAULT 200,
    daily_video_limit   INT      DEFAULT 100,
    used_chapters       INT      DEFAULT 0,
    used_images         INT      DEFAULT 0,
    used_videos         INT      DEFAULT 0,
    reset_date          DATE     DEFAULT NULL,
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 项目模块 ==========

CREATE TABLE IF NOT EXISTS t_project (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    intro       TEXT         DEFAULT NULL,
    type        VARCHAR(32)  DEFAULT NULL COMMENT '短剧/漫画/有声书',
    art_style   VARCHAR(64)  DEFAULT NULL,
    video_ratio VARCHAR(16)  DEFAULT NULL COMMENT '16:9/9:16',
    user_id     BIGINT       NOT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 小说模块 ==========

CREATE TABLE IF NOT EXISTS t_novel (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    chapter_index INT          DEFAULT 0,
    volume_index  INT          DEFAULT 1,
    reel          VARCHAR(64)  DEFAULT NULL,
    chapter       VARCHAR(128) DEFAULT NULL,
    chapter_data  LONGTEXT     DEFAULT NULL,
    summary       TEXT         DEFAULT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_version (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    novel_id      BIGINT       NOT NULL,
    project_id    BIGINT       NOT NULL,
    chapter_index INT          DEFAULT 0,
    chapter_data  LONGTEXT     DEFAULT NULL,
    summary       TEXT         DEFAULT NULL,
    source        VARCHAR(32)  DEFAULT NULL COMMENT 'ai/manual',
    version       INT          DEFAULT 1,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_novel_id (novel_id),
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_world (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id       BIGINT   NOT NULL,
    background       TEXT     DEFAULT NULL,
    power_system     TEXT     DEFAULT NULL,
    social_structure TEXT     DEFAULT NULL,
    core_rules       TEXT     DEFAULT NULL,
    taboos           TEXT     DEFAULT NULL,
    state            INT      DEFAULT 0,
    create_time      DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_character (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id     BIGINT       NOT NULL,
    name           VARCHAR(64)  NOT NULL,
    role           VARCHAR(32)  DEFAULT NULL,
    age            INT          DEFAULT NULL,
    appearance     TEXT         DEFAULT NULL,
    personality    TEXT         DEFAULT NULL,
    ability        TEXT         DEFAULT NULL,
    relationships  TEXT         DEFAULT NULL,
    growth_arc     TEXT         DEFAULT NULL,
    current_state  TEXT         DEFAULT NULL,
    speech_style   TEXT         DEFAULT NULL,
    voice_id       VARCHAR(128) DEFAULT NULL,
    state          INT          DEFAULT 0,
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_outline (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id        BIGINT       NOT NULL,
    main_plot         TEXT         DEFAULT NULL,
    theme             VARCHAR(256) DEFAULT NULL,
    volume_index      INT          DEFAULT 1,
    volume_name       VARCHAR(128) DEFAULT NULL,
    volume_plot       TEXT         DEFAULT NULL,
    start_chapter     INT          DEFAULT NULL,
    end_chapter       INT          DEFAULT NULL,
    volume_climax     TEXT         DEFAULT NULL,
    volume_cliffhanger TEXT        DEFAULT NULL,
    state             INT          DEFAULT 0,
    create_time       DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_chapter_plan (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT       NOT NULL,
    volume_index    INT          DEFAULT 1,
    chapter_index   INT          DEFAULT 0,
    title           VARCHAR(128) DEFAULT NULL,
    summary         TEXT         DEFAULT NULL,
    key_events      TEXT         DEFAULT NULL,
    characters      TEXT         DEFAULT NULL,
    emotion_curve   TEXT         DEFAULT NULL,
    foreshadowing   TEXT         DEFAULT NULL,
    payoff          TEXT         DEFAULT NULL,
    cliffhanger     TEXT         DEFAULT NULL,
    word_target     INT          DEFAULT NULL,
    state           INT          DEFAULT 0,
    create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_novel_quality_report (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id          BIGINT       NOT NULL,
    scope               VARCHAR(32)  DEFAULT NULL COMMENT 'chapter/volume/book',
    scope_index         INT          DEFAULT NULL,
    overall_score       INT          DEFAULT NULL,
    dimensions          JSON         DEFAULT NULL,
    summary             TEXT         DEFAULT NULL,
    auto_fix_suggestions TEXT        DEFAULT NULL,
    state               VARCHAR(32)  DEFAULT NULL,
    create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 故事线/大纲/剧本 ==========

CREATE TABLE IF NOT EXISTS t_storyline (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(128) DEFAULT NULL,
    content     LONGTEXT     DEFAULT NULL,
    novel_ids   VARCHAR(512) DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_outline (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT   NOT NULL,
    episode     INT      DEFAULT 1,
    data        JSON     DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_script (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    outline_id  BIGINT       DEFAULT NULL,
    name        VARCHAR(128) DEFAULT NULL,
    content     LONGTEXT     DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_outline_id (outline_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 资产/图片/视频 ==========

CREATE TABLE IF NOT EXISTS t_assets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(128) NOT NULL,
    intro       TEXT         DEFAULT NULL,
    prompt      TEXT         DEFAULT NULL,
    type        VARCHAR(32)  DEFAULT NULL COMMENT 'role/props/scene',
    file_path   VARCHAR(512) DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    assets_id   BIGINT       DEFAULT NULL,
    script_id   BIGINT       DEFAULT NULL,
    type        VARCHAR(32)  DEFAULT NULL,
    file_path   VARCHAR(512) DEFAULT NULL,
    state       INT          DEFAULT 0,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_video (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id      BIGINT        NOT NULL,
    script_id       BIGINT        DEFAULT NULL,
    shot_id         VARCHAR(64)   DEFAULT NULL,
    segment_id      INT           DEFAULT NULL,
    config_id       BIGINT        DEFAULT NULL,
    prompt          TEXT          DEFAULT NULL,
    video_prompt    TEXT          DEFAULT NULL,
    camera_motion   VARCHAR(64)   DEFAULT NULL,
    file_path       VARCHAR(512)  DEFAULT NULL,
    last_frame      VARCHAR(512)  DEFAULT NULL,
    state           INT           DEFAULT 0,
    duration        DECIMAL(10,2) DEFAULT NULL,
    resolution      VARCHAR(16)   DEFAULT NULL,
    manufacturer    VARCHAR(32)   DEFAULT NULL,
    model           VARCHAR(64)   DEFAULT NULL,
    task_id         VARCHAR(128)  DEFAULT NULL,
    retry_count     INT           DEFAULT 0,
    max_retry       INT           DEFAULT 3,
    version         INT           DEFAULT 1,
    selected        INT           DEFAULT 0,
    error_message   TEXT          DEFAULT NULL,
    create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_script_id (script_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_video_config (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT        NOT NULL,
    script_id     BIGINT        DEFAULT NULL,
    manufacturer  VARCHAR(32)   DEFAULT NULL,
    mode          VARCHAR(32)   DEFAULT NULL,
    resolution    VARCHAR(16)   DEFAULT NULL,
    duration      DECIMAL(10,2) DEFAULT NULL,
    audio_enabled INT           DEFAULT 0,
    style_prefix  TEXT          DEFAULT NULL,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_video_compose_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id           BIGINT       NOT NULL,
    script_id            BIGINT       DEFAULT NULL,
    transition           VARCHAR(32)  DEFAULT 'crossfade',
    transition_duration  INT          DEFAULT 500,
    bgm_path             VARCHAR(512) DEFAULT NULL,
    bgm_volume           INT          DEFAULT 50,
    tts_enabled          INT          DEFAULT 0,
    tts_volume           INT          DEFAULT 80,
    subtitle_enabled     INT          DEFAULT 0,
    subtitle_style       JSON         DEFAULT NULL,
    watermark_enabled    INT          DEFAULT 0,
    watermark_type       VARCHAR(32)  DEFAULT NULL,
    watermark_content    VARCHAR(256) DEFAULT NULL,
    watermark_position   VARCHAR(32)  DEFAULT NULL,
    watermark_opacity    INT          DEFAULT 80,
    intro_enabled        INT          DEFAULT 0,
    intro_text           VARCHAR(256) DEFAULT NULL,
    intro_duration       INT          DEFAULT 3,
    outro_enabled        INT          DEFAULT 0,
    outro_text           VARCHAR(256) DEFAULT NULL,
    outro_duration       INT          DEFAULT 3,
    output_resolution    VARCHAR(16)  DEFAULT '1080p',
    output_fps           INT          DEFAULT 30,
    create_time          DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_video_compose (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT        NOT NULL,
    script_id     BIGINT        DEFAULT NULL,
    config_id     BIGINT        DEFAULT NULL,
    video_ids     TEXT          DEFAULT NULL,
    file_path     VARCHAR(512)  DEFAULT NULL,
    duration      DECIMAL(10,2) DEFAULT NULL,
    state         INT           DEFAULT 0,
    retry_count   INT           DEFAULT 0,
    error_message TEXT          DEFAULT NULL,
    create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== AI配置/设置/Prompt ==========

CREATE TABLE IF NOT EXISTS t_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(32)  DEFAULT NULL COMMENT 'text/image/video/tts',
    name         VARCHAR(64)  DEFAULT NULL,
    manufacturer VARCHAR(32)  DEFAULT NULL,
    model        VARCHAR(64)  DEFAULT NULL,
    api_key      VARCHAR(512) DEFAULT NULL COMMENT 'AES encrypted',
    base_url     VARCHAR(256) DEFAULT NULL,
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_ai_model_map (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_id   BIGINT       NOT NULL,
    name        VARCHAR(64)  DEFAULT NULL,
    `key`       VARCHAR(64)  NOT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_config_id (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_setting (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       DEFAULT NULL,
    token_key      VARCHAR(256) DEFAULT NULL,
    image_model    VARCHAR(64)  DEFAULT NULL,
    language_model VARCHAR(64)  DEFAULT NULL,
    create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_prompts (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(64)  NOT NULL,
    name          VARCHAR(128) DEFAULT NULL,
    type          VARCHAR(32)  DEFAULT NULL,
    default_value LONGTEXT     DEFAULT NULL,
    custom_value  LONGTEXT     DEFAULT NULL,
    parent_code   VARCHAR(64)  DEFAULT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 对话/任务/日志 ==========

CREATE TABLE IF NOT EXISTS t_chat_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    type        VARCHAR(32)  DEFAULT NULL COMMENT 'outline/storyboard/novel',
    data        LONGTEXT     DEFAULT NULL,
    novel       TEXT         DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_task_list (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id  BIGINT       NOT NULL,
    name        VARCHAR(128) DEFAULT NULL,
    prompt      TEXT         DEFAULT NULL,
    state       VARCHAR(32)  DEFAULT 'pending',
    checkpoint  TEXT         DEFAULT NULL,
    start_time  DATETIME     DEFAULT NULL,
    end_time    DATETIME     DEFAULT NULL,
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_agent_log (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id    BIGINT       NOT NULL,
    agent_type    VARCHAR(32)  DEFAULT NULL,
    session_id    VARCHAR(64)  DEFAULT NULL,
    parent_log_id BIGINT       DEFAULT NULL,
    action        VARCHAR(32)  DEFAULT NULL,
    agent_name    VARCHAR(64)  DEFAULT NULL,
    tool_name     VARCHAR(64)  DEFAULT NULL,
    input         LONGTEXT     DEFAULT NULL,
    output        LONGTEXT     DEFAULT NULL,
    duration      BIGINT       DEFAULT NULL,
    token_used    INT          DEFAULT NULL,
    status        VARCHAR(32)  DEFAULT NULL,
    error_message TEXT         DEFAULT NULL,
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== TTS/素材 ==========

CREATE TABLE IF NOT EXISTS t_tts_config (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id   BIGINT        NOT NULL,
    character_id BIGINT        DEFAULT NULL,
    voice_id     VARCHAR(128)  DEFAULT NULL,
    manufacturer VARCHAR(32)   DEFAULT NULL,
    speed        DECIMAL(4,2)  DEFAULT 1.00,
    pitch        DECIMAL(4,2)  DEFAULT 1.00,
    emotion      VARCHAR(32)   DEFAULT NULL,
    state        INT           DEFAULT 0,
    create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_tts_audio (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id     BIGINT        NOT NULL,
    script_id      BIGINT        DEFAULT NULL,
    text           TEXT          DEFAULT NULL,
    character_name VARCHAR(64)   DEFAULT NULL,
    voice_id       VARCHAR(128)  DEFAULT NULL,
    file_path      VARCHAR(512)  DEFAULT NULL,
    duration       DECIMAL(10,2) DEFAULT NULL,
    state          INT           DEFAULT 0,
    create_time    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS t_material (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128)  DEFAULT NULL,
    type        VARCHAR(32)   DEFAULT NULL COMMENT 'bgm/sfx/intro/outro/watermark',
    file_path   VARCHAR(512)  DEFAULT NULL,
    category    VARCHAR(64)   DEFAULT NULL,
    duration    DECIMAL(10,2) DEFAULT NULL,
    tags        VARCHAR(256)  DEFAULT NULL,
    user_id     BIGINT        DEFAULT NULL,
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
