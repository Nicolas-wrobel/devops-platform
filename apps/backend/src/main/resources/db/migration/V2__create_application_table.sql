CREATE TABLE application (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP NOT NULL,
    updated_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_application_name UNIQUE (name)
);
