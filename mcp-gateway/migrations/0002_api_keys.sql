-- 0002_api_keys.sql
-- One row per developer API key. We store only the SHA-256 of the secret
-- (key_hash) so a database leak cannot be used to impersonate users. The
-- plaintext `sv_live_...` value is returned exactly once, at creation time.
--
-- `key_prefix` is the first 14 characters of the plaintext (e.g.
-- "sv_live_abcdef"). It is safe to show in UIs and API responses and lets
-- users identify which key was used without exposing the secret.

CREATE TABLE IF NOT EXISTS api_keys (
  id TEXT PRIMARY KEY,                  -- key_xxx
  user_id TEXT NOT NULL REFERENCES users(id),
  name TEXT NOT NULL,                   -- "Default", "CI", etc.
  key_hash TEXT NOT NULL UNIQUE,        -- SHA-256 of the secret (hex)
  key_prefix TEXT NOT NULL,             -- first 14 chars of the plaintext
  last_used_at INTEGER,
  revoked_at INTEGER,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_keys_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_keys_user ON api_keys(user_id);
