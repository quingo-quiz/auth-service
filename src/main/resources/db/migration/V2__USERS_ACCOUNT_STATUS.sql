ALTER TABLE users
    ADD status SMALLINT;

ALTER TABLE users
    DROP COLUMN blocked;