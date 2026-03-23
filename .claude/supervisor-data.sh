#!/bin/bash
# Generates supervisor-data.json for the dashboard
OUT="/Users/thomasgorisse/Projects/sceneview-android/.claude/supervisor-data.json"
REPO="/Users/thomasgorisse/Projects/sceneview-android"
NOW=$(date '+%H:%M:%S')

# Count agents & mcps
AGENTS_RAW=$(ps aux | grep -E 'claude.*(--model|--chrome)' | grep -v grep | grep -v disclaimer)
MCP_RAW=$(ps aux | grep 'claude mcp serve' | grep -v grep | grep -v disclaimer)
AGENT_COUNT=$(echo "$AGENTS_RAW" | grep -c .)
MCP_COUNT=$(echo "$MCP_RAW" | grep -c . 2>/dev/null || echo 0)

# Worktree data
WT_MAIN_COMMIT=$(git -C "$REPO" log --oneline -1 2>/dev/null)
WT_MAIN_HASH=$(echo "$WT_MAIN_COMMIT" | cut -d' ' -f1)
WT_MAIN_MSG=$(echo "$WT_MAIN_COMMIT" | cut -d' ' -f2-)
WT_MAIN_FILES=$(git -C "$REPO" status --short 2>/dev/null | wc -l | tr -d ' ')
WT_MAIN_BRANCH=$(git -C "$REPO" branch --show-current 2>/dev/null)

WT2="$REPO/.claude/worktrees/hardcore-swirles"
WT2_COMMIT=$(git -C "$WT2" log --oneline -1 2>/dev/null)
WT2_HASH=$(echo "$WT2_COMMIT" | cut -d' ' -f1)
WT2_MSG=$(echo "$WT2_COMMIT" | cut -d' ' -f2-)
WT2_FILES=$(git -C "$WT2" status --short 2>/dev/null | wc -l | tr -d ' ')
WT2_BRANCH=$(git -C "$WT2" branch --show-current 2>/dev/null)

WT3="$REPO/.claude/worktrees/agent-a6f1cdac"
WT3_COMMIT=$(git -C "$WT3" log --oneline -1 2>/dev/null)
WT3_HASH=$(echo "$WT3_COMMIT" | cut -d' ' -f1)
WT3_MSG=$(echo "$WT3_COMMIT" | cut -d' ' -f2-)
WT3_FILES=$(git -C "$WT3" status --short 2>/dev/null | wc -l | tr -d ' ')
WT3_BRANCH=$(git -C "$WT3" branch --show-current 2>/dev/null)

TOTAL_FILES=$((WT_MAIN_FILES + WT2_FILES + WT3_FILES))

# Recent commits across all worktrees
RECENT=$(
  (git -C "$REPO" log --oneline --format='%ar|%h|%s' -3 2>/dev/null;
   git -C "$WT3" log --oneline --format='%ar|%h|%s' -3 2>/dev/null) | sort -u | head -6
)

# Build agent entries from ps
build_agents() {
  local first=true
  echo "["

  # Parse each claude process
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    PID=$(echo "$line" | awk '{print $2}')
    CPU=$(echo "$line" | awk '{print $3}')
    MEM=$(echo "$line" | awk '{print $4}')
    START=$(echo "$line" | awk '{print $9}')
    CMD="$line"

    # Determine model
    if echo "$CMD" | grep -q 'opus-4-6\[1m\]'; then
      MODEL="Opus 4.6 [1M]"
      MCLASS="opus"
    elif echo "$CMD" | grep -q 'opus-4-6'; then
      MODEL="Opus 4.6"
      MCLASS="opus"
    elif echo "$CMD" | grep -q 'model default'; then
      MODEL="Default (Sonnet)"
      MCLASS="default"
    elif echo "$CMD" | grep -q '\-\-chrome'; then
      MODEL="CLI + Chrome"
      MCLASS="opus"
    else
      MODEL="Unknown"
      MCLASS="default"
    fi

    # Determine MCPs
    MCPS=""
    echo "$CMD" | grep -q 'd4f303ec' && MCPS="$MCPS\"Gmail\","
    echo "$CMD" | grep -q 'ba4f7894' && MCPS="$MCPS\"GCal\","
    echo "$CMD" | grep -q '135dd734' && MCPS="$MCPS\"Slack\","
    echo "$CMD" | grep -q 'Claude in Chrome' && MCPS="$MCPS\"Chrome\","
    echo "$CMD" | grep -q 'Claude Preview' && MCPS="$MCPS\"Preview\","
    echo "$CMD" | grep -q 'claude-code' && MCPS="$MCPS\"CC\","
    echo "$CMD" | grep -q 'scheduled-tasks' && MCPS="$MCPS\"Sched\","
    MCPS="[${MCPS%,}]"

    # Status
    STATUS="running"
    CPU_INT=${CPU%.*}
    if [ "$CPU_INT" -lt 1 ] 2>/dev/null; then
      STATUS="idle"
    fi

    # Terminal info
    TTY=$(echo "$line" | awk '{print $7}')
    if echo "$CMD" | grep -q '\-\-continue'; then
      MODEL="$MODEL --continue"
    fi

    # CPU color
    CPUCOLOR="var(--text-dim)"
    if [ "$CPU_INT" -gt 5 ] 2>/dev/null; then
      CPUCOLOR="var(--green)"
    elif [ "$CPU_INT" -gt 1 ] 2>/dev/null; then
      CPUCOLOR="var(--yellow)"
    fi

    [ "$first" = true ] && first=false || echo ","
    cat << ENTRY
    {
      "pid": "$PID",
      "model": "$MODEL",
      "modelClass": "$MCLASS",
      "uptime": "$START",
      "cpu": "${CPU}%",
      "cpuColor": "$CPUCOLOR",
      "mem": "${MEM}%",
      "status": "$STATUS",
      "tty": "$TTY",
      "mcps": $MCPS
    }
ENTRY
  done <<< "$AGENTS_RAW"

  echo "]"
}

# Build timeline
build_timeline() {
  echo "["
  local first=true
  echo "$RECENT" | while IFS='|' read -r AGO HASH MSG; do
    [ -z "$HASH" ] && continue
    [ "$first" = true ] && first=false || echo ","
    echo "    {\"time\": \"$AGO\", \"text\": \"<code>$HASH</code> $MSG\", \"color\": \"var(--accent)\"}"
  done
  echo "]"
}

AGENTS_JSON=$(build_agents)
TIMELINE_JSON=$(build_timeline)

# WT main files class
[ "$WT_MAIN_FILES" -eq 0 ] && MAIN_FC="clean" && MAIN_FT="clean" || MAIN_FC="dirty" && MAIN_FT="$WT_MAIN_FILES fichiers"
[ "$WT2_FILES" -eq 0 ] && WT2_FC="clean" && WT2_FT="clean" || WT2_FC="dirty" && WT2_FT="$WT2_FILES fichiers"
[ "$WT3_FILES" -eq 0 ] && WT3_FC="clean" && WT3_FT="clean" || WT3_FC="dirty" && WT3_FT="$WT3_FILES fichiers"

cat > "$OUT" << JSON
{
  "timestamp": "$NOW",
  "stats": {
    "agentCount": "$AGENT_COUNT",
    "agentSub": "Claude Code processes",
    "mcpCount": "$MCP_COUNT",
    "mcpSub": "bridge servers",
    "wtCount": "3",
    "wtSub": "git worktrees",
    "filesCount": "$TOTAL_FILES",
    "filesSub": "across all worktrees"
  },
  "agents": $AGENTS_JSON,
  "worktrees": [
    {
      "name": "main repo",
      "icon": "📂",
      "cardClass": "active",
      "branch": "$WT_MAIN_BRANCH",
      "commitHash": "$WT_MAIN_HASH",
      "commitMsg": "$WT_MAIN_MSG",
      "filesClass": "$MAIN_FC",
      "filesText": "$MAIN_FT",
      "badge": {"class": "head", "text": "HEAD"}
    },
    {
      "name": "hardcore-swirles",
      "icon": "📂",
      "cardClass": "",
      "branch": "$WT2_BRANCH",
      "commitHash": "$WT2_HASH",
      "commitMsg": "$WT2_MSG",
      "filesClass": "$WT2_FC",
      "filesText": "$WT2_FT",
      "badge": null
    },
    {
      "name": "agent-a6f1cdac",
      "icon": "📂",
      "cardClass": "ahead",
      "branch": "$WT3_BRANCH",
      "commitHash": "$WT3_HASH",
      "commitMsg": "$WT3_MSG",
      "filesClass": "$WT3_FC",
      "filesText": "$WT3_FT",
      "badge": {"class": "ahead", "text": "+6 AHEAD"}
    }
  ],
  "branches": [
    {"name": "$WT_MAIN_BRANCH", "active": true},
    {"name": "$WT2_BRANCH", "active": false}
  ],
  "timeline": $TIMELINE_JSON
}
JSON
