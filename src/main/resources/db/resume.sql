CREATE TABLE resume_analyses
(
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    company VARCHAR(150) NOT NULL,
    role VARCHAR(150) NOT NULL,

    original_filename VARCHAR(255) NOT NULL,
    stored_file_path VARCHAR(500) NOT NULL,

    extracted_text TEXT NOT NULL,

    ats_score INTEGER NOT NULL,
    keyword_match_score INTEGER NOT NULL,
    impact_score INTEGER NOT NULL,
    readability_score INTEGER NOT NULL,
    grammar_score INTEGER NOT NULL,
    structure_score INTEGER NOT NULL,

    matched_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    missing_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    weak_keywords JSONB NOT NULL DEFAULT '[]'::jsonb,
    strong_areas JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggestions JSONB NOT NULL DEFAULT '[]'::jsonb,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resume_analyses_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_resume_ats_score
        CHECK (ats_score BETWEEN 0 AND 100),

    CONSTRAINT chk_resume_keyword_score
        CHECK (keyword_match_score BETWEEN 0 AND 100),

    CONSTRAINT chk_resume_impact_score
        CHECK (impact_score BETWEEN 0 AND 100),

    CONSTRAINT chk_resume_readability_score
        CHECK (readability_score BETWEEN 0 AND 100),

    CONSTRAINT chk_resume_grammar_score
        CHECK (grammar_score BETWEEN 0 AND 100),

    CONSTRAINT chk_resume_structure_score
        CHECK (structure_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_resume_analyses_user_created
    ON resume_analyses (user_id, created_at DESC);

CREATE INDEX idx_resume_company_role
    ON resume_analyses (company, role);