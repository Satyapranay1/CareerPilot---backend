CREATE TABLE company_knowledge (
    id BIGSERIAL PRIMARY KEY,

    company_name VARCHAR(255) NOT NULL,
    website VARCHAR(1000) NOT NULL,
    normalized_website VARCHAR(1000) NOT NULL UNIQUE,

    content_hash VARCHAR(64),

    indexed BOOLEAN NOT NULL DEFAULT FALSE,
    indexed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interview_sessions (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    company_knowledge_id BIGINT,

    company_name VARCHAR(255),
    company_website VARCHAR(1000),

    job_role VARCHAR(255) NOT NULL,
    job_description TEXT,
    job_description_hash VARCHAR(64),

    interview_type VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',

    overall_score DOUBLE PRECISION,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,

    CONSTRAINT fk_interview_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_interview_company
        FOREIGN KEY (company_knowledge_id)
        REFERENCES company_knowledge(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_interview_type
        CHECK (
            interview_type IN (
                'TECHNICAL',
                'BEHAVIOURAL',
                'MIXED'
            )
        ),

    CONSTRAINT chk_interview_difficulty
        CHECK (
            difficulty IN (
                'EASY',
                'MEDIUM',
                'HARD'
            )
        ),

    CONSTRAINT chk_interview_status
        CHECK (
            status IN (
                'CREATED',
                'IN_PROGRESS',
                'COMPLETED'
            )
        )
);

CREATE TABLE interview_questions (
    id BIGSERIAL PRIMARY KEY,

    session_id BIGINT NOT NULL,

    question TEXT NOT NULL,

    question_type VARCHAR(30) NOT NULL,

    topic VARCHAR(255),

    question_number INTEGER NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_question_session
        FOREIGN KEY (session_id)
        REFERENCES interview_sessions(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_question_type
        CHECK (
            question_type IN (
                'TECHNICAL',
                'BEHAVIOURAL'
            )
        ),

    CONSTRAINT uq_session_question_number
        UNIQUE (session_id, question_number)
);

CREATE TABLE interview_attempts (
    id BIGSERIAL PRIMARY KEY,

    question_id BIGINT NOT NULL UNIQUE,

    user_answer TEXT NOT NULL,

    score DOUBLE PRECISION,

    correctness DOUBLE PRECISION,
    completeness DOUBLE PRECISION,
    clarity DOUBLE PRECISION,
    depth DOUBLE PRECISION,
    relevance DOUBLE PRECISION,

    star_situation DOUBLE PRECISION,
    star_task DOUBLE PRECISION,
    star_action DOUBLE PRECISION,
    star_result DOUBLE PRECISION,

    strengths TEXT,
    missing_concepts TEXT,
    feedback TEXT,
    suggested_answer TEXT,

    answered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attempt_question
        FOREIGN KEY (question_id)
        REFERENCES interview_questions(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_interview_sessions_user
ON interview_sessions(user_id);

CREATE INDEX idx_interview_sessions_company
ON interview_sessions(company_knowledge_id);

CREATE INDEX idx_interview_questions_session
ON interview_questions(session_id);

CREATE INDEX idx_interview_attempts_question
ON interview_attempts(question_id);

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

TRUNCATE TABLE
    interview_attempts,
    interview_questions,
    interview_sessions,
    company_knowledge
RESTART IDENTITY CASCADE;

SELECT * FROM company_knowledge;
SELECT * FROM interview_sessions;
SELECT * FROM interview_questions;
SELECT * FROM interview_attempts;

SELECT
    blocked.pid AS blocked_pid,
    blocked.application_name AS blocked_app,
    blocked.query AS blocked_query,

    blocker.pid AS blocker_pid,
    blocker.application_name AS blocker_app,
    blocker.state AS blocker_state,
    blocker.xact_start AS blocker_transaction_started,
    blocker.query AS blocker_query

FROM pg_stat_activity blocked

CROSS JOIN LATERAL
    unnest(pg_blocking_pids(blocked.pid))
    AS b(blocker_pid)

JOIN pg_stat_activity blocker
    ON blocker.pid = b.blocker_pid

WHERE blocked.datname = 'postgres';

SELECT pg_terminate_backend(590);
SELECT pg_terminate_backend(581);

SELECT
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'company_knowledge';


SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    blocker.pid AS blocker_pid,
    blocker.state AS blocker_state,
    blocker.query AS blocker_query
FROM pg_stat_activity blocked
JOIN pg_stat_activity blocker
    ON blocker.pid = ANY(pg_blocking_pids(blocked.pid))
WHERE blocked.datname = 'postgres';

SELECT pg_terminate_backend(669);

SELECT COUNT(*) FROM vector_store;

SELECT *
FROM vector_store;