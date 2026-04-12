-- Telemetry Analytics Queries for SceneView MCP
-- Run with: wrangler d1 execute telemetry-db --command "SELECT ..."
-- Or paste entire query blocks into the local explorer UI

-- ============================================================================
-- 1. TOTAL EVENTS BY TIME WINDOW
-- ============================================================================
-- Use these to understand overall traffic volume and trends
-- Useful for: capacity planning, detecting outages, measuring growth

-- Total events in last 24 hours
SELECT 
  COUNT(*) as total_events,
  COUNT(DISTINCT client) as unique_clients,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_events,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_events
FROM events
WHERE ingested > datetime('now', '-1 day');

-- Total events in last 7 days
SELECT 
  COUNT(*) as total_events,
  COUNT(DISTINCT client) as unique_clients,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_events,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_events,
  COUNT(*) * 100.0 / NULLIF(SUM(COUNT(*)) OVER (), 0) as pct_of_30d
FROM events
WHERE ingested > datetime('now', '-7 days');

-- Total events in last 30 days
SELECT 
  COUNT(*) as total_events,
  COUNT(DISTINCT client) as unique_clients,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_events,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_events
FROM events
WHERE ingested > datetime('now', '-30 days');


-- ============================================================================
-- 2. TOP 10 TOOLS BY USAGE (last 7 days)
-- ============================================================================
-- Identifies which tools are most popular and engaging
-- Use for: feature prioritization, API design validation, roadmap planning

SELECT 
  tool,
  COUNT(*) as usage_count,
  COUNT(DISTINCT client) as unique_clients,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as pct_of_tool_calls
FROM events
WHERE event = 'tool' 
  AND ingested > datetime('now', '-7 days')
GROUP BY tool
ORDER BY usage_count DESC
LIMIT 10;


-- ============================================================================
-- 3. UNIQUE CLIENTS (MCP HOST APPS) BREAKDOWN
-- ============================================================================
-- Shows which applications are hosting sceneview-mcp
-- Use for: understanding ecosystem adoption, prioritizing platform support

SELECT 
  client,
  COUNT(DISTINCT client_ver) as versions_in_use,
  COUNT(*) as total_events,
  COUNT(DISTINCT mcp_ver) as mcp_versions_used,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_count,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_count,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as pct_of_traffic
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY client
ORDER BY total_events DESC;


-- ============================================================================
-- 4. VERSION ADOPTION (sceneview-mcp versions in use)
-- ============================================================================
-- Tracks which versions of the MCP are actively being used
-- Use for: deprecation planning, breaking change planning, security fixes

SELECT 
  mcp_ver,
  COUNT(DISTINCT client) as unique_clients,
  COUNT(*) as total_events,
  MAX(ingested) as last_seen,
  MIN(ingested) as first_seen,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as pct_of_traffic
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY mcp_ver
ORDER BY total_events DESC;

-- Same, but only init events (shows actual "in use" not cached tool calls)
SELECT 
  mcp_ver,
  COUNT(DISTINCT client) as unique_clients,
  COUNT(*) as init_events,
  MAX(ingested) as last_seen,
  MIN(ingested) as first_seen
FROM events
WHERE event = 'init' AND ingested > datetime('now', '-30 days')
GROUP BY mcp_ver
ORDER BY init_events DESC;


-- ============================================================================
-- 5. DAILY EVENT COUNTS (last 30 days, grouped by date)
-- ============================================================================
-- Time series for trend analysis, spike detection, anomaly detection
-- Use for: detecting outages, bot activity, seasonal patterns

SELECT 
  DATE(ingested) as date,
  COUNT(*) as total_events,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_events,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_events,
  COUNT(DISTINCT client) as unique_clients,
  ROUND(AVG(CASE WHEN event = 'tool' THEN 1 ELSE 0 END), 2) as avg_tools_per_init
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY DATE(ingested)
ORDER BY date DESC;


-- ============================================================================
-- 6. INIT vs TOOL EVENT RATIO
-- ============================================================================
-- Measures engagement: tools per init session
-- High ratio = users finding value, Low ratio = users not engaging with tools
-- Use for: measuring UX effectiveness, tool discoverability

SELECT 
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_count,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_count,
  ROUND(
    CAST(SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) AS FLOAT) / 
    NULLIF(SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END), 0),
    2
  ) as tools_per_init_session,
  ROUND(100.0 * SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) / COUNT(*), 2) as init_pct,
  ROUND(100.0 * SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) / COUNT(*), 2) as tool_pct
FROM events
WHERE ingested > datetime('now', '-7 days');

-- Ratio by client (see which apps have most engaged users)
SELECT 
  client,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_count,
  SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) as tool_count,
  ROUND(
    CAST(SUM(CASE WHEN event = 'tool' THEN 1 ELSE 0 END) AS FLOAT) / 
    NULLIF(SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END), 0),
    2
  ) as tools_per_init
FROM events
WHERE ingested > datetime('now', '-7 days')
GROUP BY client
ORDER BY tools_per_init DESC;


-- ============================================================================
-- 7. PRO vs FREE TIER SPLIT
-- ============================================================================
-- Monitors premium adoption and freemium funnel
-- Use for: monetization metrics, conversion tracking, tier adoption

SELECT 
  tier,
  COUNT(*) as total_events,
  COUNT(DISTINCT client) as unique_clients,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as pct_of_traffic
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY tier;

-- Tier split by event type (do pro users engage more with tools?)
SELECT 
  tier,
  event,
  COUNT(*) as event_count,
  COUNT(DISTINCT client) as unique_clients,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (PARTITION BY tier), 2) as pct_within_tier
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY tier, event
ORDER BY tier, event;

-- Pro vs free by version adoption (version drift indicator)
SELECT 
  mcp_ver,
  tier,
  COUNT(*) as init_events,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (PARTITION BY tier), 2) as pct_within_tier
FROM events
WHERE event = 'init' AND ingested > datetime('now', '-7 days')
GROUP BY mcp_ver, tier
ORDER BY tier, init_events DESC;


-- ============================================================================
-- 8. HOURLY TRAFFIC PATTERN (bot vs human detection)
-- ============================================================================
-- Identifies traffic distribution across hours
-- Humans peak during work hours (8-18 UTC or offset by timezone)
-- Bots have uniform distribution or odd hours
-- Use for: anomaly detection, bot activity patterns, capacity planning

SELECT 
  CAST(STRFTIME('%H', ingested) AS INTEGER) as hour_utc,
  COUNT(*) as event_count,
  COUNT(DISTINCT client) as unique_clients,
  ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER (), 2) as pct_of_daily_traffic
FROM events
WHERE ingested > datetime('now', '-7 days')
GROUP BY CAST(STRFTIME('%H', ingested) AS INTEGER)
ORDER BY hour_utc;

-- If timestamps are in local time, adjust STRFTIME accordingly
-- For example, if clients send local time, you may need:
-- CAST(STRFTIME('%H', datetime(ingested, 'localtime')) AS INTEGER) as hour_local

-- Hourly pattern by client (see if specific apps have anomalous traffic)
SELECT 
  client,
  CAST(STRFTIME('%H', ingested) AS INTEGER) as hour_utc,
  COUNT(*) as event_count,
  ROUND(AVG(CASE WHEN event = 'tool' THEN 1.0 ELSE 0 END), 2) as tool_ratio
FROM events
WHERE ingested > datetime('now', '-3 days')
GROUP BY client, CAST(STRFTIME('%H', ingested) AS INTEGER)
ORDER BY client, hour_utc;


-- ============================================================================
-- BONUS QUERIES
-- ============================================================================

-- Cohort analysis: New vs returning clients (by first-seen date)
SELECT 
  DATE(MIN(ingested)) as cohort_date,
  COUNT(DISTINCT client) as new_clients,
  COUNT(*) as events_in_first_period
FROM events
WHERE event = 'init'
GROUP BY DATE(MIN(ingested))
ORDER BY cohort_date DESC;

-- Tool usage heat map: which tools are used together?
SELECT 
  e1.tool as tool_1,
  e2.tool as tool_2,
  COUNT(*) as co_occurrence
FROM events e1
JOIN events e2 USING (client)
WHERE e1.event = 'tool' 
  AND e2.event = 'tool'
  AND e1.tool < e2.tool  -- avoid duplicates
  AND e1.ingested > datetime('now', '-7 days')
GROUP BY tool_1, tool_2
HAVING co_occurrence > 5
ORDER BY co_occurrence DESC
LIMIT 20;

-- Client-version matrix: which client/version combos are active?
SELECT 
  client,
  client_ver,
  COUNT(*) as event_count,
  COUNT(DISTINCT mcp_ver) as mcp_versions,
  MAX(ingested) as last_seen
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY client, client_ver
ORDER BY event_count DESC;

-- Daily active sessions (unique clients per day)
SELECT 
  DATE(ingested) as date,
  COUNT(DISTINCT client) as daily_active_clients,
  SUM(CASE WHEN event = 'init' THEN 1 ELSE 0 END) as init_count
FROM events
WHERE ingested > datetime('now', '-30 days')
GROUP BY DATE(ingested)
ORDER BY date DESC;
