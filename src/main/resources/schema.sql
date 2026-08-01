CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(255) NOT NULL UNIQUE,

    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(20) NOT NULL DEFAULT 'USER',

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    short_bio VARCHAR(500),

    profile_picture VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_users_email
ON users(email);

CREATE TABLE user_education (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,

    institution     VARCHAR(255) NOT NULL,
    degree          VARCHAR(150),
    field_of_study  VARCHAR(150),

    start_year      INTEGER,
    end_year        INTEGER,

    grade           VARCHAR(50),

    CONSTRAINT fk_education_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_education_user_id
ON user_education(user_id);

CREATE TABLE user_experience (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,

    company         VARCHAR(255) NOT NULL,
    job_title       VARCHAR(150) NOT NULL,

    start_date      DATE NOT NULL,
    end_date        DATE,

    description     TEXT,

    CONSTRAINT fk_experience_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_experience_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_experience_user_id
ON user_experience(user_id);

CREATE TABLE user_skills (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,

    skill_name      VARCHAR(100) NOT NULL,

    CONSTRAINT fk_skill_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT uq_user_skill
        UNIQUE (user_id, skill_name)
);

CREATE INDEX idx_skills_user_id
ON user_skills(user_id);


