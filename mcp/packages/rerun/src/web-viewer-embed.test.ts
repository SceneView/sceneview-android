import { describe, it, expect } from "vitest";
import { generateWebViewerEmbed } from "./web-viewer-embed.js";

describe("generateWebViewerEmbed", () => {
  it("returns html, script, and fullDocument", () => {
    const result = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(result.html).toBeTruthy();
    expect(result.script).toBeTruthy();
    expect(result.fullDocument).toBeTruthy();
  });

  it("uses the default container id when not provided", () => {
    const { html, script } = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(html).toContain('id="rerun-viewer"');
    expect(script).toContain('"rerun-viewer"');
  });

  it("honours a custom container id", () => {
    const { html } = generateWebViewerEmbed({
      rrdUrl: "/demo.rrd",
      containerId: "my_viewer",
    });
    expect(html).toContain('id="my_viewer"');
  });

  it("rejects an invalid container id", () => {
    expect(() =>
      generateWebViewerEmbed({
        rrdUrl: "/demo.rrd",
        containerId: "with spaces!",
      }),
    ).toThrow(/containerId/i);
  });

  it("rejects an empty rrdUrl", () => {
    expect(() => generateWebViewerEmbed({ rrdUrl: "" })).toThrow(/rrdUrl is required/i);
    expect(() => generateWebViewerEmbed({ rrdUrl: "  " })).toThrow(/rrdUrl is required/i);
  });

  it("loads the WebViewer module from jsdelivr/+esm by default", () => {
    const { script } = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(script).toContain("cdn.jsdelivr.net/npm/@rerun-io/web-viewer");
    expect(script).toContain("+esm");
    expect(script).toContain("import { WebViewer }");
  });

  it("pins the viewer version when requested", () => {
    const { script } = generateWebViewerEmbed({
      rrdUrl: "/demo.rrd",
      viewerVersion: "0.22.0",
    });
    expect(script).toContain("@rerun-io/web-viewer@0.22.0");
  });

  it("hides the welcome screen by default", () => {
    const { script } = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(script).toContain("hide_welcome_screen: true");
  });

  it("emits a setTimeRange call when timeRange is provided", () => {
    const { script } = generateWebViewerEmbed({
      rrdUrl: "/demo.rrd",
      timeRange: [1_000_000, 5_000_000],
    });
    expect(script).toContain("viewer.setTimeRange");
    expect(script).toContain("1000000");
    expect(script).toContain("5000000");
  });

  it("omits setTimeRange when no timeRange is provided", () => {
    const { script } = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(script).not.toContain("setTimeRange");
  });

  it("wraps the container in a full HTML document", () => {
    const { fullDocument } = generateWebViewerEmbed({ rrdUrl: "/demo.rrd" });
    expect(fullDocument).toContain("<!doctype html>");
    expect(fullDocument).toContain("<html>");
    expect(fullDocument).toContain("</html>");
  });

  it("JSON-encodes the rrdUrl to survive quotes and special chars", () => {
    const { script } = generateWebViewerEmbed({
      rrdUrl: 'https://ex.com/a"b.rrd',
    });
    expect(script).toContain('"https://ex.com/a\\"b.rrd"');
  });
});
