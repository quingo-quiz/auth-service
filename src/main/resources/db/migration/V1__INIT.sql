CREATE TABLE tokens
(
    id         UUID    NOT NULL,
    token      VARCHAR(400),
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    issued_at  TIMESTAMP WITHOUT TIME ZONE,
    revoked    BOOLEAN NOT NULL,
    user_id    UUID,
    CONSTRAINT pk_tokens PRIMARY KEY (id)
);

CREATE TABLE users
(
    id       UUID NOT NULL,
    username VARCHAR(100),
    email    VARCHAR(100),
    password VARCHAR(255),
    roles    SMALLINT[],
    verified BOOLEAN,
    blocked  BOOLEAN,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

ALTER TABLE tokens
    ADD CONSTRAINT FK_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);