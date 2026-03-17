interface GitHubIssue {
  number: number;
  title: string;
  html_url: string;
  body: string | null;
  labels: Array<{ name: string }>;
  created_at: string;
  updated_at: string;
  user: { login: string };
}

interface CacheEntry {
  data: string;
  fetchedAt: number;
}

// Cache for 10 minutes — avoids hammering the GitHub API on every tool call
const CACHE_TTL_MS = 10 * 60 * 1000;
let cache: CacheEntry | null = null;

export async function fetchKnownIssues(): Promise<string> {
  const now = Date.now();
  if (cache && now - cache.fetchedAt < CACHE_TTL_MS) {
    return cache.data;
  }

  let issues: GitHubIssue[] = [];
  let fetchError: string | null = null;

  try {
    const response = await fetch(
      "https://api.github.com/repos/SceneView/sceneview-android/issues?state=open&per_page=30",
      {
        headers: {
          Accept: "application/vnd.github+json",
          "User-Agent": "sceneview-mcp/3.0",
          "X-GitHub-Api-Version": "2022-11-28",
        },
      }
    );

    if (!response.ok) {
      fetchError = `GitHub API returned ${response.status}: ${response.statusText}`;
    } else {
      issues = (await response.json()) as GitHubIssue[];
    }
  } catch (err) {
    fetchError = `Failed to fetch issues: ${err instanceof Error ? err.message : String(err)}`;
  }

  const result = formatIssues(issues, fetchError);
  cache = { data: result, fetchedAt: now };
  return result;
}

function formatIssues(issues: GitHubIssue[], fetchError: string | null): string {
  const lines: string[] = ["# SceneView — Open GitHub Issues\n"];

  if (fetchError) {
    lines.push(`> ⚠️ ${fetchError}. Showing cached data if available.\n`);
  } else {
    lines.push(`*Fetched live from GitHub — ${issues.length} open issue(s). Cached for 10 minutes.*\n`);
  }

  if (issues.length === 0) {
    lines.push("No open issues. 🎉");
    return lines.join("\n");
  }

  // Group by label: bugs first, then questions/other
  const bugs = issues.filter((i) => i.labels.some((l) => l.name === "bug"));
  const others = issues.filter((i) => !i.labels.some((l) => l.name === "bug"));

  const renderGroup = (title: string, group: GitHubIssue[]) => {
    if (group.length === 0) return;
    lines.push(`## ${title}\n`);
    for (const issue of group) {
      const labelStr =
        issue.labels.length > 0 ? ` [${issue.labels.map((l) => l.name).join(", ")}]` : "";
      lines.push(`### #${issue.number} — ${issue.title}${labelStr}`);
      lines.push(`*Opened by @${issue.user.login} · ${issue.updated_at.slice(0, 10)}*`);
      lines.push(issue.html_url);

      // Include first 300 chars of body as context
      if (issue.body) {
        const excerpt = issue.body
          .replace(/\r\n/g, "\n")
          .replace(/```[\s\S]*?```/g, "[code block]") // strip code blocks for brevity
          .trim()
          .slice(0, 300);
        lines.push(`\n> ${excerpt.replace(/\n/g, "\n> ")}${issue.body.length > 300 ? "…" : ""}`);
      }
      lines.push("");
    }
  };

  renderGroup("🐛 Bug Reports", bugs);
  renderGroup("📋 Other Issues", others);

  return lines.join("\n");
}
