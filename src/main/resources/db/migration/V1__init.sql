CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE accounts
(
    id         UUID         NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    roles      VARCHAR(255)[],
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);

CREATE TABLE candidates
(
    id         UUID         NOT NULL,
    full_name  VARCHAR(255),
    email      VARCHAR(255) NOT NULL,
    phone      VARCHAR(255) NOT NULL,
    file_id    UUID         NOT NULL,
    created_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_candidates PRIMARY KEY (id)
);

CREATE TABLE cv_batches
(
    id                UUID         NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    status            VARCHAR(255),
    total             INTEGER      NOT NULL,
    processed_files   INTEGER      NOT NULL,
    failed_files      INTEGER      NOT NULL,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_cv_batches PRIMARY KEY (id)
);

CREATE TABLE cv_files
(
    id                UUID         NOT NULL,
    batch_id          UUID         NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_filename  VARCHAR(255) NOT NULL,
    file_size         BIGINT       NOT NULL,
    status            VARCHAR(255) NOT NULL,
    created_at        TIMESTAMP(6) WITHOUT TIME ZONE,
    CONSTRAINT pk_cv_files PRIMARY KEY (id)
);

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

ALTER TABLE accounts
    ADD CONSTRAINT uc_accounts_email UNIQUE (email);

ALTER TABLE candidates
    ADD CONSTRAINT uc_candidates_file UNIQUE (file_id);

ALTER TABLE cv_files
    ADD CONSTRAINT uc_cv_files_storage_filename UNIQUE (storage_filename);

ALTER TABLE candidates
    ADD CONSTRAINT FK_CANDIDATES_ON_FILE FOREIGN KEY (file_id) REFERENCES cv_files (id);

ALTER TABLE cv_files
    ADD CONSTRAINT FK_CV_FILES_ON_BATCH FOREIGN KEY (batch_id) REFERENCES cv_batches (id);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);