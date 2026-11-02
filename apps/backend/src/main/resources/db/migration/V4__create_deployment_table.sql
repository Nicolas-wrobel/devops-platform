CREATE TABLE deployment (
    id                          BIGSERIAL PRIMARY KEY,
    application_environment_id BIGINT NOT NULL REFERENCES application_environment (id),
    version                    VARCHAR(50) NOT NULL,
    status                     VARCHAR(20) NOT NULL,
    triggered_by               VARCHAR(100) NOT NULL,
    created_at                 TIMESTAMP NOT NULL,
    started_at                 TIMESTAMP,
    planned_finish_at          TIMESTAMP,
    finished_at                TIMESTAMP
);
