#!/usr/bin/env bash
#
# update-metrics.sh — refresh the numbers on website-static/index.html
#
# Fetches GitHub stars/forks/contributors, npm 30-day download counts for
# sceneview-mcp and sceneview-web, and the number of released versions on
# Maven Central (counted from the authoritative maven-metadata.xml, never
# from search.maven.org which serves a stale index).
#
# Usage:
#   bash .claude/scripts/update-metrics.sh          # print a diff-ready summary
#   bash .claude/scripts/update-metrics.sh --write  # patch index.html in place
#
# The script relies on the `data-metric="..."` attributes on each
# .metric-card__value element so the patch is stable across layout
# changes. If a card is renamed or removed, update the case statement
# below accordingly.
#
# Rules:
#   - Never round UP (no "1.9K" when the real count is 1154). Round to
#     nearest 100 and display with one decimal K, e.g. 1154 -> "1.1K".
#   - Never invent a Maven Central download count. Sonatype does not
#     expose one publicly and the legacy `search.maven.org` stats API
#     is stale. Use the release count from maven-metadata.xml instead.
#   - Run from the repo root.

set -euo pipefail

REPO="sceneview/sceneview"
HTML="website-static/index.html"
WRITE=0
if [[ "${1-}" == "--write" ]]; then WRITE=1; fi

if [[ ! -f "$HTML" ]]; then
  echo "error: $HTML not found — run from the repo root" >&2
  exit 1
fi

# -- helpers -----------------------------------------------------------------

# format 1154 -> "1.1K", 211 -> "211", 3417 -> "3.4K", 70 -> "70"
human() {
  local n=$1
  if (( n >= 1000 )); then
    awk -v n="$n" 'BEGIN{printf "%.1fK", n/1000}'
  else
    echo "$n"
  fi
}

# Fetch one JSON field safely
fetch_json() {
  local url=$1 field=$2
  curl -sfL "$url" | python3 -c "import sys,json;print(json.load(sys.stdin).get('$field',''))"
}

# -- fetchers ----------------------------------------------------------------

echo "Fetching GitHub repo stats..."
stars=$(gh api "repos/$REPO" --jq .stargazers_count)
forks=$(gh api "repos/$REPO" --jq .forks_count)

echo "Fetching contributor count..."
# GH contributors endpoint is paginated; sum the pages.
contributors=$(gh api "repos/$REPO/contributors?per_page=100" --paginate --jq 'length' \
  | awk '{s+=$1} END {print s}')

echo "Fetching npm 30-day downloads..."
mcp_dl=$(fetch_json "https://api.npmjs.org/downloads/point/last-month/sceneview-mcp" downloads)
web_dl=$(fetch_json "https://api.npmjs.org/downloads/point/last-month/sceneview-web" downloads)

echo "Counting Maven Central releases..."
releases=$(curl -sfL "https://repo1.maven.org/maven2/io/github/sceneview/sceneview/maven-metadata.xml" \
  | grep -oE '<version>[^<]+</version>' | wc -l | tr -d ' ')

# -- display -----------------------------------------------------------------

printf '\n%-18s %-10s %s\n' METRIC VALUE RAW
printf '%s\n' '--------------------------------------------'
printf '%-18s %-10s %s\n' stars           "$(human "$stars")"         "$stars"
printf '%-18s %-10s %s\n' forks           "$(human "$forks")"         "$forks"
printf '%-18s %-10s %s\n' mcp-downloads   "$(human "$mcp_dl")"        "$mcp_dl"
printf '%-18s %-10s %s\n' web-downloads   "$(human "$web_dl")"        "$web_dl"
printf '%-18s %-10s %s\n' contributors    "$(human "$contributors")"  "$contributors"
printf '%-18s %-10s %s\n' releases        "$(human "$releases")"      "$releases"

if (( WRITE == 0 )); then
  echo
  echo "Dry run. Re-run with --write to patch $HTML."
  exit 0
fi

# -- patch -------------------------------------------------------------------

patch_metric() {
  local key=$1 value=$2
  # Replace the value inside <div class="metric-card__value" data-metric="KEY">OLD</div>
  python3 - "$HTML" "$key" "$value" <<'PY'
import re, sys, pathlib
path, key, value = sys.argv[1], sys.argv[2], sys.argv[3]
p = pathlib.Path(path)
src = p.read_text(encoding="utf-8")
pat = re.compile(
    r'(<div class="metric-card__value" data-metric="' + re.escape(key) + r'">)[^<]*(</div>)'
)
new, n = pat.subn(r'\g<1>' + value + r'\g<2>', src)
if n != 1:
    print(f"patch failed for {key}: {n} matches (expected 1)", file=sys.stderr)
    sys.exit(1)
p.write_text(new, encoding="utf-8")
PY
}

patch_metric stars          "$(human "$stars")"
patch_metric forks          "$(human "$forks")"
patch_metric mcp-downloads  "$(human "$mcp_dl")"
patch_metric web-downloads  "$(human "$web_dl")"
patch_metric contributors   "$(human "$contributors")"
patch_metric releases       "$(human "$releases")"

echo
echo "Patched $HTML. Review with: git diff $HTML"
