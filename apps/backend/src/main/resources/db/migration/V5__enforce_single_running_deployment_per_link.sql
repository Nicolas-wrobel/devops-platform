-- At most one RUNNING deployment per application/environment link.
-- Partial: uniqueness only applies to RUNNING rows, so any number of
-- PENDING/SUCCESS/FAILED rows per link remain allowed. Enforced here, not
-- only in DeploymentService, because a check-then-act in application code
-- cannot see concurrent uncommitted transactions (see ADR-0026).
CREATE UNIQUE INDEX uk_deployment_one_running_per_link
    ON deployment (application_environment_id)
    WHERE status = 'RUNNING';
