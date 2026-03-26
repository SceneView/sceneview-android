import { describe, it, expect, vi, beforeEach } from "vitest";

// We test the private formatIssues logic by calling fetchKnownIssues with a
// mocked global fetch so we never hit the real GitHub API in tests.

const mockIssues = [
  {
    number: 123,
    title: "SIGABRT on dispose",
    html_url: "https://github.com/sceneview/sceneview/issues/123",
    body: "Calling destroy() causes a native crash.",
    labels: [{ name: "bug" }],
    created_at: "2026-01-01T00:00:00Z",
    updated_at: "2026-01-15T00:00:00Z",
    user: { login: "testuser" },
  },
  {
    number: 124,
    title: "How do I add shadows?",
    html_url: "https://github.com/sceneview/sceneview/issues/124",
    body: "I want to enable shadows for my AR scene.",
    labels: [{ name: "question" }],
    created_at: "2026-01-02T00:00:00Z",
    updated_at: "2026-01-16T00:00:00Z",
    user: { login: "anotheruser" },
  },
];

beforeEach(() => {
  // Reset module cache so the in-memory cache is cleared between tests
  vi.resetModules();
});

describe("fetchKnownIssues", () => {
  it("returns markdown with issue titles when GitHub API succeeds", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockIssues,
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("SIGABRT on dispose");
    expect(result).toContain("#123");
    expect(result).toContain("How do I add shadows?");
    expect(result).toContain("#124");
    vi.unstubAllGlobals();
  });

  it("groups bug-labelled issues under Bug Reports", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockIssues,
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("Bug Reports");
    expect(result).toContain("SIGABRT on dispose");
    vi.unstubAllGlobals();
  });

  it("groups non-bug issues under Other Issues", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockIssues,
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("Other Issues");
    expect(result).toContain("How do I add shadows?");
    vi.unstubAllGlobals();
  });

  it("includes github URLs", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockIssues,
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("https://github.com/sceneview/sceneview/issues/123");
    vi.unstubAllGlobals();
  });

  it("includes body excerpt", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => mockIssues,
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("native crash");
    vi.unstubAllGlobals();
  });

  it("shows warning message when GitHub API fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 403,
        statusText: "Forbidden",
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("403");
    vi.unstubAllGlobals();
  });

  it("shows warning message when fetch throws (network error)", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new Error("Network error")));

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("Network error");
    vi.unstubAllGlobals();
  });

  it("shows celebration when there are no open issues", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => [],
      })
    );

    const { fetchKnownIssues } = await import("./issues.js");
    const result = await fetchKnownIssues();

    expect(result).toContain("No open issues");
    vi.unstubAllGlobals();
  });
});
