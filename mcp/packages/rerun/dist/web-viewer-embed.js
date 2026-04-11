// ─── Rerun Web Viewer embed generator ─────────────────────────────────────────
//
// Emits an HTML/JS snippet the developer can drop into any web page to embed
// the Rerun Web Viewer WASM module next to their existing SceneView canvas.
/**
 * Returns three views of the same embed: the container HTML, the module
 * script, and a standalone HTML document that combines them — pick whichever
 * fits your target page.
 */
export function generateWebViewerEmbed(options) {
    const { rrdUrl, containerId = "rerun-viewer", hideWelcome = true, viewerVersion = "latest", timeRange, } = options;
    if (!rrdUrl || !rrdUrl.trim()) {
        throw new Error("rrdUrl is required");
    }
    if (!/^[a-zA-Z0-9_-]+$/.test(containerId)) {
        throw new Error(`containerId "${containerId}" must contain only letters, digits, "_" or "-"`);
    }
    const moduleUrl = viewerVersion === "latest"
        ? "https://cdn.jsdelivr.net/npm/@rerun-io/web-viewer/+esm"
        : `https://cdn.jsdelivr.net/npm/@rerun-io/web-viewer@${viewerVersion}/+esm`;
    const html = `<div id="${containerId}" style="width: 100%; height: 100%;"></div>`;
    const timeRangeLine = timeRange
        ? `\n    // Jump to the requested time range (nanos)
    viewer.setTimeRange("device_clock", ${timeRange[0]}, ${timeRange[1]});`
        : "";
    const script = `<script type="module">
import { WebViewer } from "${moduleUrl}";

const viewer = new WebViewer();
await viewer.start(
    ${JSON.stringify(rrdUrl)},
    document.getElementById(${JSON.stringify(containerId)}),
    { hide_welcome_screen: ${hideWelcome} }
);${timeRangeLine}
</script>`;
    const fullDocument = `<!doctype html>
<html>
<head>
    <meta charset="utf-8" />
    <title>Rerun Viewer</title>
    <style>
        html, body { margin: 0; height: 100%; background: #0f0f14; }
        #${containerId} { width: 100vw; height: 100vh; }
    </style>
</head>
<body>
    ${html}
    ${script}
</body>
</html>
`;
    return { html, script, fullDocument };
}
