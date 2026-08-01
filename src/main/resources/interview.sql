CREATE TABLE interview_question (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question TEXT NOT NULL,
    reference_answer TEXT NOT NULL,
    key_points TEXT NOT NULL
);

CREATE TABLE interview_attempt (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer TEXT NOT NULL,
    overall_score DOUBLE PRECISION NOT NULL,
    evaluation JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_interview_attempt_user
        FOREIGN KEY (user_id) REFERENCES users(id),

    CONSTRAINT fk_interview_attempt_question
        FOREIGN KEY (question_id) REFERENCES interview_question(id)
);

CREATE INDEX idx_attempt_user_created
    ON interview_attempt(user_id, created_at DESC);

CREATE INDEX idx_question_filter
    ON interview_question(type, topic, difficulty);

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

SELECT name, default_version, installed_version
FROM pg_available_extensions
WHERE name = 'vector';

CREATE TABLE vector_test (
    id BIGSERIAL PRIMARY KEY,
    embedding vector(3)
);