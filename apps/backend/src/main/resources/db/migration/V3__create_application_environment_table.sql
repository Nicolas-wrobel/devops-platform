CREATE TABLE application_environment (
    id             BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES application (id),
    environment_id BIGINT NOT NULL REFERENCES environment (id),
    created_at     TIMESTAMP NOT NULL,
    CONSTRAINT uk_application_environment UNIQUE (application_id, environment_id)
);
