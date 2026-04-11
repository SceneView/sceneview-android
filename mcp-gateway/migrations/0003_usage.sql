-- 0003_usage.sql
-- One row per MCP tool invocation. The composite indexes make monthly
-- aggregations (`SELECT COUNT(*) ... WHERE bucket_month = ?`) cheap even
-- without a dedicated counter table.
--
-- `bucket_month` is stored as a pre-computed 'YYYY-MM' string so the
-- monthly quota query is a straight indexed equality rather than a
-- BETWEEN range scan over created_at.

CREATE TABLE IF NOT EXISTS usage_records (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  api_key_id TEXT NOT NULL REFERENCES api_keys(id),
  user_id TEXT NOT NULL REFERENCES users(id),
  tool_name TEXT NOT NULL,
  tier_required TEXT NOT NULL,          -- 'free' | 'pro'
  status TEXT NOT NULL,                 -- 'ok' | 'denied' | 'rate_limited' | 'error'
  bucket_month TEXT NOT NULL,           -- 'YYYY-MM'
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_usage_key_month ON usage_records(api_key_id, bucket_month);
CREATE INDEX IF NOT EXISTS idx_usage_user_month ON usage_records(user_id, bucket_month);
