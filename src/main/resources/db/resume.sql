CREATE TABLE knowledge_documents (
    id BIGSERIAL PRIMARY KEY,

    company VARCHAR(255) NOT NULL,

    job_role VARCHAR(255) NOT NULL,

    content TEXT NOT NULL,

    embedding VECTOR,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_company_role UNIQUE (company, job_role)
);

CREATE TABLE resumes (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,

    file_path TEXT NOT NULL,

    company VARCHAR(255),

    job_role VARCHAR(255),

    resume_text TEXT NOT NULL,

    analysis_json JSONB,

    ats_score DECIMAL(5,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resume_user
ON resumes(user_id);

CREATE INDEX idx_knowledge_company_role
ON knowledge_documents(company, job_role);