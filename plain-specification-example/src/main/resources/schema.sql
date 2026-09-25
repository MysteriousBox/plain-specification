CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(64)  NOT NULL,
    email       VARCHAR(128),
    age         INT          NOT NULL,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
