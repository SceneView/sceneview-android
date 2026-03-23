#!/bin/bash
# Supervisor monitor — detects finished tasks, new commits, blocked processes
REPO="/Users/thomasgorisse/Projects/sceneview-android"
STATE="$REPO/.claude/supervisor-state.json"
EVENTS=""

# ── Current state ──
CURRENT_PIDS=$(ps aux | grep -E 'claude.*(--model|mcp serve|--chrome)' | grep -v grep | grep -v disclaimer | awk '{print $2}' | sort)
CURRENT_PID_COUNT=$(echo "$CURRENT_PIDS" | grep -c .)

# Worktree commits
WT_MAIN_HASH=$(git -C "$REPO" log --format='%h' -1 2>/dev/null)
WT_HS_HASH=$(git -C "$REPO/.claude/worktrees/hardcore-swirles" log --format='%h' -1 2>/dev/null)
WT_AG_HASH=$(git -C "$REPO/.claude/worktrees/agent-a6f1cdac" log --format='%h' -1 2>/dev/null)

# Worktree dirty counts
WT_MAIN_DIRTY=$(git -C "$REPO" status --short 2>/dev/null | wc -l | tr -d ' ')
WT_HS_DIRTY=$(git -C "$REPO/.claude/worktrees/hardcore-swirles" status --short 2>/dev/null | wc -l | tr -d ' ')
WT_AG_DIRTY=$(git -C "$REPO/.claude/worktrees/agent-a6f1cdac" status --short 2>/dev/null | wc -l | tr -d ' ')

# Worktree list
CURRENT_WTS=$(git -C "$REPO" worktree list --porcelain 2>/dev/null | grep "^worktree " | sort)
CURRENT_WT_COUNT=$(echo "$CURRENT_WTS" | grep -c .)

# ── Load previous state ──
if [ -f "$STATE" ]; then
  PREV_PIDS=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('pids','').strip())" 2>/dev/null)
  PREV_PID_COUNT=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('pid_count','0'))" 2>/dev/null)
  PREV_MAIN_HASH=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('main_hash',''))" 2>/dev/null)
  PREV_HS_HASH=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('hs_hash',''))" 2>/dev/null)
  PREV_AG_HASH=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('ag_hash',''))" 2>/dev/null)
  PREV_WT_COUNT=$(cat "$STATE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('wt_count','0'))" 2>/dev/null)
else
  PREV_PIDS=""
  PREV_PID_COUNT="0"
  PREV_MAIN_HASH=""
  PREV_HS_HASH=""
  PREV_AG_HASH=""
  PREV_WT_COUNT="0"
fi

# ── Normalize PIDs to newline-separated ──
PREV_PIDS_NL=$(echo "$PREV_PIDS" | tr ' ' '\n' | grep -v '^$' | sort)
CURRENT_PIDS_NL=$(echo "$CURRENT_PIDS" | tr ' ' '\n' | grep -v '^$' | sort)

# ── Detect finished processes (PIDs that disappeared) ──
if [ -n "$PREV_PIDS_NL" ]; then
  for pid in $PREV_PIDS_NL; do
    if ! echo "$CURRENT_PIDS_NL" | grep -qw "$pid"; then
      EVENTS="${EVENTS}PROCESS_FINISHED:${pid}\n"
    fi
  done
fi

# ── Detect new processes ──
if [ -n "$CURRENT_PIDS_NL" ]; then
  for pid in $CURRENT_PIDS_NL; do
    if [ -z "$PREV_PIDS_NL" ] || ! echo "$PREV_PIDS_NL" | grep -qw "$pid"; then
      MODEL=$(ps -p $pid -o args= 2>/dev/null | grep -o 'model [^ ]*' | head -1)
      EVENTS="${EVENTS}PROCESS_STARTED:${pid}:${MODEL}\n"
    fi
  done
fi

# ── Detect new commits ──
if [ -n "$PREV_MAIN_HASH" ] && [ "$WT_MAIN_HASH" != "$PREV_MAIN_HASH" ]; then
  MSG=$(git -C "$REPO" log --format='%s' -1 2>/dev/null)
  EVENTS="${EVENTS}NEW_COMMIT:main_repo:${WT_MAIN_HASH}:${MSG}\n"
fi
if [ -n "$PREV_HS_HASH" ] && [ "$WT_HS_HASH" != "$PREV_HS_HASH" ]; then
  MSG=$(git -C "$REPO/.claude/worktrees/hardcore-swirles" log --format='%s' -1 2>/dev/null)
  EVENTS="${EVENTS}NEW_COMMIT:hardcore-swirles:${WT_HS_HASH}:${MSG}\n"
fi
if [ -n "$PREV_AG_HASH" ] && [ "$WT_AG_HASH" != "$PREV_AG_HASH" ]; then
  MSG=$(git -C "$REPO/.claude/worktrees/agent-a6f1cdac" log --format='%s' -1 2>/dev/null)
  EVENTS="${EVENTS}NEW_COMMIT:agent-a6f1cdac:${WT_AG_HASH}:${MSG}\n"
fi

# ── Detect worktree changes ──
if [ "$CURRENT_WT_COUNT" != "$PREV_WT_COUNT" ]; then
  if [ "$CURRENT_WT_COUNT" -gt "$PREV_WT_COUNT" ] 2>/dev/null; then
    EVENTS="${EVENTS}WORKTREE_CREATED:count=${CURRENT_WT_COUNT}\n"
  else
    EVENTS="${EVENTS}WORKTREE_REMOVED:count=${CURRENT_WT_COUNT}\n"
  fi
fi

# ── Save current state ──
cat > "$STATE" << JSON
{
  "pids": "$(echo $CURRENT_PIDS | tr '\n' ' ')",
  "pid_count": "$CURRENT_PID_COUNT",
  "main_hash": "$WT_MAIN_HASH",
  "hs_hash": "$WT_HS_HASH",
  "ag_hash": "$WT_AG_HASH",
  "wt_count": "$CURRENT_WT_COUNT",
  "main_dirty": "$WT_MAIN_DIRTY",
  "hs_dirty": "$WT_HS_DIRTY",
  "ag_dirty": "$WT_AG_DIRTY",
  "timestamp": "$(date '+%H:%M:%S')"
}
JSON

# ── Also regenerate dashboard data ──
bash "$REPO/.claude/supervisor-data.sh" 2>/dev/null

# ── Output events (only if something happened) ──
if [ -n "$EVENTS" ]; then
  echo "SUPERVISOR_EVENTS"
  echo -e "$EVENTS"
else
  echo "NO_CHANGES"
fi
