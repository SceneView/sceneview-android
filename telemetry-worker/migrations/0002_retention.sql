-- Pre-aggregated daily rollups for long-term retention.
-- Raw events are deleted after 90 days; daily_rollups preserves the aggregated
-- counts indefinitely so historical trends remain queryable.
CREATE TABLE IF NOT EXISTS daily_rollups (
  date     TEXT    NOT NULL,  -- YYYY-MM-DD
  event    TEXT    NOT NULL CHECK (event IN ('init', 'tool')),
  client   TEXT    NOT NULL,
  mcp_ver  TEXT    NOT NULL,
  tier     TEXT    NOT NULL CHECK (tier IN ('free', 'pro')),
  tool     TEXT,              -- NULL for "init" events
  count    INTEGER NOT NULL DEFAULT 0,
  UNIQUE (date, event, client, mcp_ver, tier, tool)
);

-- Lookups are almost always date-range queries.
CREATE INDEX idx_daily_rollups_date ON daily_rollups (date);
