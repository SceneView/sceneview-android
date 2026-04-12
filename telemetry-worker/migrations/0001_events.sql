-- Anonymous telemetry events from sceneview-mcp.
-- No PII — no IP, no hostname, no user identity.
CREATE TABLE IF NOT EXISTS events (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  timestamp  TEXT    NOT NULL,  -- ISO 8601 UTC from client
  ingested   TEXT    NOT NULL DEFAULT (datetime('now')),  -- server receipt time
  event      TEXT    NOT NULL CHECK (event IN ('init', 'tool')),
  client     TEXT    NOT NULL,  -- e.g. "claude-desktop", "cursor"
  client_ver TEXT    NOT NULL,  -- client version string
  mcp_ver    TEXT    NOT NULL,  -- sceneview-mcp version
  tier       TEXT    NOT NULL CHECK (tier IN ('free', 'pro')),
  tool       TEXT               -- NULL for "init" events
);

-- Query patterns: daily active sessions, tool popularity, version adoption
CREATE INDEX idx_events_event     ON events (event);
CREATE INDEX idx_events_ingested  ON events (ingested);
CREATE INDEX idx_events_tool      ON events (tool) WHERE tool IS NOT NULL;
CREATE INDEX idx_events_mcp_ver   ON events (mcp_ver);
