CREATE TABLE appointment (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    patient_name    VARCHAR(150) NOT NULL,
    patient_cpf     VARCHAR(11)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    protocol        VARCHAR(36)  NULL,
    attempt_count   INT          NOT NULL DEFAULT 0,
    last_error      VARCHAR(500) NULL,
    next_attempt_at DATETIME(6)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_appointment_protocol UNIQUE (protocol),
    INDEX idx_appointment_status_next_attempt_at (status, next_attempt_at),
    INDEX idx_appointment_patient_cpf (patient_cpf)
);
