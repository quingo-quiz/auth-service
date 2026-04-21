CREATE TABLE user_mfa_settings
(
    id             UUID       NOT NULL,
    user_id        UUID REFERENCES users(id),
    type           SMALLINT     NULL,
    secret_key     VARCHAR(255) NULL,
    method_enabled BOOLEAN       NULL,
    CONSTRAINT pk_usermfasettings PRIMARY KEY (id)
);

ALTER TABLE users
    ADD mfa_enabled BOOLEAN;
