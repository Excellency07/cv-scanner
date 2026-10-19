-- Candidates: contact fields are optional (not every CV has them) + extracted details
ALTER TABLE candidates
    ALTER COLUMN email DROP NOT NULL,
    ALTER COLUMN phone DROP NOT NULL,
    ADD COLUMN years_of_experience INTEGER,
    ADD COLUMN location            VARCHAR(255),
    ADD COLUMN job_type            VARCHAR(50),
    ADD COLUMN job_location_type   VARCHAR(50);

CREATE TABLE candidate_skills
(
    candidate_id UUID         NOT NULL,
    skill        VARCHAR(255) NOT NULL,
    CONSTRAINT pk_candidate_skills PRIMARY KEY (candidate_id, skill),
    CONSTRAINT fk_candidate_skills_on_candidate FOREIGN KEY (candidate_id) REFERENCES candidates (id) ON DELETE CASCADE
);

CREATE INDEX idx_candidate_skills_skill ON candidate_skills (LOWER(skill));
CREATE INDEX idx_candidates_years_of_experience ON candidates (years_of_experience);
CREATE INDEX idx_candidates_job_location_type ON candidates (job_location_type);

-- Batches: who uploaded it and which Spring Batch run processed it
ALTER TABLE cv_batches
    ADD COLUMN created_by       VARCHAR(255),
    ADD COLUMN job_execution_id BIGINT,
    ADD COLUMN started_at       TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN finished_at      TIMESTAMP(6) WITHOUT TIME ZONE;

-- Files: why a file failed to parse
ALTER TABLE cv_files
    ADD COLUMN error_message VARCHAR(1000);

CREATE INDEX idx_cv_files_batch_status ON cv_files (batch_id, status);
