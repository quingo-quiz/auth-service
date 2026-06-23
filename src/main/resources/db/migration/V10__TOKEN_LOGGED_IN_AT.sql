-- Время первого входа в рамках одной логин-сессии.
ALTER TABLE tokens ADD COLUMN logged_in_at timestamptz;
UPDATE tokens SET logged_in_at = issued_at WHERE logged_in_at IS NULL;
