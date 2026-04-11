import { describe, it, expect } from "vitest";
import { TOOL_DEFINITIONS, dispatchTool } from "./tools.js";

describe("TOOL_DEFINITIONS", () => {
  it("exposes the five tools defined by the plan", () => {
    const names = TOOL_DEFINITIONS.map((t) => t.name).sort();
    expect(names).toEqual([
      "embed_web_viewer",
      "explain_concept",
      "generate_ar_logger",
      "generate_python_sidecar",
      "setup_rerun_project",
    ]);
  });

  it("every tool has a non-empty description and object inputSchema", () => {
    for (const t of TOOL_DEFINITIONS) {
      expect(t.description.length).toBeGreaterThan(20);
      expect(t.inputSchema.type).toBe("object");
    }
  });
});

describe("dispatchTool — setup_rerun_project", () => {
  it("returns files + instructions for android", async () => {
    const result = await dispatchTool("setup_rerun_project", {
      platform: "android",
    });
    expect(result.isError).toBeFalsy();
    const text = result.content[0]!.text;
    expect(text).toContain("## Rerun setup — android");
    expect(text).toContain("ARWithRerun.kt");
    expect(text).toContain("rerun-bridge.py");
  });

  it("reports an error on invalid platform", async () => {
    const result = await dispatchTool("setup_rerun_project", {
      platform: "windows",
    });
    expect(result.isError).toBe(true);
    expect(result.content[0]!.text.toLowerCase()).toContain("invalid platform");
  });
});

describe("dispatchTool — generate_ar_logger", () => {
  it("returns Kotlin code when language=kotlin", async () => {
    const result = await dispatchTool("generate_ar_logger", {
      language: "kotlin",
      dataTypes: ["pose", "planes"],
    });
    expect(result.isError).toBeFalsy();
    const text = result.content[0]!.text;
    expect(text).toContain("Kotlin (Jetpack Compose)");
    expect(text).toContain("@Composable");
    expect(text).toContain("rememberRerunBridge");
  });

  it("returns Swift code when language=swift", async () => {
    const result = await dispatchTool("generate_ar_logger", {
      language: "swift",
      dataTypes: ["pose"],
    });
    expect(result.isError).toBeFalsy();
    expect(result.content[0]!.text).toContain("SwiftUI");
    expect(result.content[0]!.text).toContain("ARSceneView");
  });

  it("errors when dataTypes is empty", async () => {
    const result = await dispatchTool("generate_ar_logger", {
      language: "kotlin",
      dataTypes: [],
    });
    expect(result.isError).toBe(true);
    expect(result.content[0]!.text.toLowerCase()).toContain("at least one");
  });

  it("errors when language is invalid", async () => {
    const result = await dispatchTool("generate_ar_logger", {
      language: "rust",
      dataTypes: ["pose"],
    });
    expect(result.isError).toBe(true);
  });
});

describe("dispatchTool — generate_python_sidecar", () => {
  it("returns a Python script with rerun-sdk imports", async () => {
    const result = await dispatchTool("generate_python_sidecar", {});
    expect(result.isError).toBeFalsy();
    expect(result.content[0]!.text).toContain("import rerun as rr");
  });

  it("propagates the custom port", async () => {
    const result = await dispatchTool("generate_python_sidecar", { port: 1234 });
    expect(result.content[0]!.text).toContain("PORT = 1234");
  });

  it("reports a validation error on bad port", async () => {
    const result = await dispatchTool("generate_python_sidecar", { port: 99999 });
    expect(result.isError).toBe(true);
  });
});

describe("dispatchTool — embed_web_viewer", () => {
  it("returns html + script + fullDocument sections", async () => {
    const result = await dispatchTool("embed_web_viewer", {
      rrdUrl: "/demo.rrd",
    });
    expect(result.isError).toBeFalsy();
    const text = result.content[0]!.text;
    expect(text).toContain("Container HTML");
    expect(text).toContain("Module script");
    expect(text).toContain("Full standalone document");
    expect(text).toContain("@rerun-io/web-viewer");
  });

  it("errors when rrdUrl is missing", async () => {
    const result = await dispatchTool("embed_web_viewer", {});
    expect(result.isError).toBe(true);
  });

  it("honours a timeRange option", async () => {
    const result = await dispatchTool("embed_web_viewer", {
      rrdUrl: "/demo.rrd",
      timeRange: [1, 100],
    });
    expect(result.content[0]!.text).toContain("setTimeRange");
  });
});

describe("dispatchTool — explain_concept", () => {
  it("returns a concept explanation", async () => {
    const result = await dispatchTool("explain_concept", { concept: "rrd" });
    expect(result.isError).toBeFalsy();
    expect(result.content[0]!.text).toContain("## .rrd");
  });

  it("errors on unknown concept", async () => {
    const result = await dispatchTool("explain_concept", { concept: "bogus" });
    expect(result.isError).toBe(true);
  });
});

describe("dispatchTool — unknown tool", () => {
  it("returns an error listing available tools", async () => {
    const result = await dispatchTool("nonexistent", {});
    expect(result.isError).toBe(true);
    expect(result.content[0]!.text).toContain("setup_rerun_project");
    expect(result.content[0]!.text).toContain("embed_web_viewer");
  });
});
