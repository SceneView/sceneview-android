-- 0004_magic_links.sql
-- Short-lived magic link tokens used by the dashboard login flow.
-- Tokens are hashed before storage so a database leak does not grant
-- account access. Each row is single-use and must be cleared on login.

CREATE TABLE IF NOT EXISTS magic_links (
  token_hash TEXT PRIMARY KEY,
  email TEXT NOT NULL,
  expires_at INTEGER NOT NULL,
  consumed_at INTEGER
);
CREATE INDEX IF NOT EXISTS idx_magic_links_email ON magic_links(email);
