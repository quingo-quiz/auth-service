CREATE TABLE user_social_account
(
    id          UUID NOT NULL,
    user_id     UUID REFERENCES users(id),
    provider    SMALLINT,
    provider_user_id VARCHAR(255),
    CONSTRAINT pk_usersocialaccount PRIMARY KEY (id)
);