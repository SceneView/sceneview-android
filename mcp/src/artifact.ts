// ─── 3D Artifact Generator ───────────────────────────────────────────────────
//
// Generates complete, self-contained HTML artifacts with interactive 3D content
// that Claude can render directly in conversations via artifacts.
//
// Types:
//   - model-viewer: interactive 3D model viewer with orbit controls + AR
//   - chart-3d: 3D bar/pie charts for data visualization
//   - scene: multi-model 3D scene with lighting and environment
//   - product-360: product turntable with hotspot annotations + AR

// ─── Types ───────────────────────────────────────────────────────────────────

export type ArtifactType = "model-viewer" | "chart-3d" | "scene" | "product-360";

export interface ChartDataPoint {
  label: string;
  value: number;
  color?: string;
}

export interface ArtifactOptions {
  autoRotate?: boolean;
  ar?: boolean;
  backgroundColor?: string;
  cameraOrbit?: string;
}

export interface Hotspot {
  position: string;    // e.g. "0.5 1.2 0.3"
  normal: string;      // e.g. "0 1 0"
  label: string;
  description?: string;
}

export interface ArtifactInput {
  type: ArtifactType;
  modelUrl?: string;
  title?: string;
  data?: ChartDataPoint[];
  options?: ArtifactOptions;
  hotspots?: Hotspot[];
  /** For scene type: additional model URLs */
  models?: Array<{ url: string; position?: string; scale?: string }>;
}

export interface ArtifactResult {
  html: string;
  title: string;
  type: ArtifactType;
}

// ─── Constants ───────────────────────────────────────────────────────────────

const MODEL_VIEWER_CDN = "https://ajax.googleapis.com/ajax/libs/model-viewer/4.0.0/model-viewer.min.js";

const DEFAULT_MODEL =
  "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/DamagedHelmet/glTF-Binary/DamagedHelmet.glb";

const DEFAULT_COLORS = [
  "#4285F4", "#EA4335", "#FBBC04", "#34A853", "#FF6D01",
  "#46BDC6", "#7BAAF7", "#F07B72", "#FCD04F", "#57BB8A",
];

const BRANDING = `<div style="position:absolute;bottom:8px;right:12px;font-size:11px;color:#888;font-family:system-ui,sans-serif;pointer-events:none">Powered by SceneView</div>`;

// ─── Shared HTML skeleton ────────────────────────────────────────────────────

function htmlShell(title: string, body: string, extraHead: string = ""): string {
  return `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${escapeHtml(title)}</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
html,body{width:100%;height:100%;overflow:hidden;background:#1a1a2e;color:#e0e0e0;font-family:system-ui,-apple-system,sans-serif}
</style>
${extraHead}
</head>
<body>
${body}
</body>
</html>`;
}

function escapeHtml(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}

// ─── Validation ──────────────────────────────────────────────────────────────

export function validateArtifactInput(input: ArtifactInput): string | null {
  const validTypes: ArtifactType[] = ["model-viewer", "chart-3d", "scene", "product-360"];
  if (!validTypes.includes(input.type)) {
    return `Invalid type "${input.type}". Must be one of: ${validTypes.join(", ")}`;
  }

  if (input.type === "chart-3d") {
    if (!input.data || !Array.isArray(input.data) || input.data.length === 0) {
      return 'Type "chart-3d" requires a non-empty `data` array with {label, value} objects.';
    }
    for (const d of input.data) {
      if (typeof d.label !== "string" || typeof d.value !== "number") {
        return "Each data item must have a string `label` and numeric `value`.";
      }
    }
  }

  if (input.modelUrl) {
    if (!input.modelUrl.startsWith("https://") && !input.modelUrl.startsWith("http://")) {
      return "modelUrl must be an HTTP(S) URL.";
    }
  }

  return null;
}

// ─── Generators ──────────────────────────────────────────────────────────────

export function generateArtifact(input: ArtifactInput): ArtifactResult {
  switch (input.type) {
    case "model-viewer":
      return generateModelViewer(input);
    case "chart-3d":
      return generateChart3D(input);
    case "scene":
      return generateScene(input);
    case "product-360":
      return generateProduct360(input);
  }
}

// ── model-viewer ─────────────────────────────────────────────────────────────

function generateModelViewer(input: ArtifactInput): ArtifactResult {
  const model = input.modelUrl || DEFAULT_MODEL;
  const title = input.title || "3D Model Viewer";
  const opts = input.options || {};
  const bg = opts.backgroundColor || "#1a1a2e";
  const orbit = opts.cameraOrbit || "0deg 75deg 105%";
  const autoRotate = opts.autoRotate !== false;
  const ar = opts.ar !== false;

  const arAttrs = ar
    ? `ar ar-modes="webxr scene-viewer quick-look"`
    : "";
  const rotateAttr = autoRotate ? `auto-rotate auto-rotate-delay="0"` : "";

  const body = `
<style>
model-viewer{width:100%;height:100%;background:${bg};--poster-color:${bg}}
.title{position:absolute;top:16px;left:16px;font-size:18px;font-weight:600;color:#fff;text-shadow:0 2px 8px rgba(0,0,0,0.5);z-index:10}
.ar-btn{position:absolute;bottom:16px;left:50%;transform:translateX(-50%);padding:10px 24px;background:rgba(66,133,244,0.9);color:#fff;border:none;border-radius:24px;font-size:14px;font-weight:500;cursor:pointer;backdrop-filter:blur(8px);z-index:10}
.ar-btn:hover{background:rgba(66,133,244,1)}
</style>
<script type="module" src="${MODEL_VIEWER_CDN}"></script>
<div class="title">${escapeHtml(title)}</div>
<model-viewer
  src="${escapeHtml(model)}"
  camera-controls
  touch-action="pan-y"
  camera-orbit="${escapeHtml(orbit)}"
  shadow-intensity="1"
  shadow-softness="0.5"
  exposure="1"
  environment-image="neutral"
  ${rotateAttr}
  ${arAttrs}
  style="width:100%;height:100%"
>
  ${ar ? `<button slot="ar-button" class="ar-btn">View in AR</button>` : ""}
</model-viewer>
${BRANDING}`;

  return {
    html: htmlShell(title, body),
    title,
    type: "model-viewer",
  };
}

// ── chart-3d ─────────────────────────────────────────────────────────────────

function generateChart3D(input: ArtifactInput): ArtifactResult {
  const title = input.title || "3D Data Visualization";
  const data = input.data!;
  const bg = input.options?.backgroundColor || "#1a1a2e";
  const maxVal = Math.max(...data.map((d) => d.value));

  const bars = data
    .map((d, i) => {
      const color = d.color || DEFAULT_COLORS[i % DEFAULT_COLORS.length];
      const heightPct = maxVal > 0 ? (d.value / maxVal) * 100 : 0;
      const height = Math.max(heightPct * 2.5, 8); // px scale, min 8px
      return `
      <div class="bar-group" style="--delay:${i * 0.08}s">
        <div class="bar-wrapper">
          <div class="bar-value">${formatNumber(d.value)}</div>
          <div class="bar" style="height:${height}px;background:linear-gradient(180deg,${color},${darken(color, 0.3)});box-shadow:4px 4px 0 ${darken(color, 0.5)}, 0 0 20px ${color}40">
            <div class="bar-face-top" style="background:${lighten(color, 0.15)}"></div>
            <div class="bar-face-right" style="background:${darken(color, 0.2)}"></div>
          </div>
        </div>
        <div class="bar-label">${escapeHtml(d.label)}</div>
      </div>`;
    })
    .join("\n");

  const body = `
<style>
body{background:${bg};display:flex;flex-direction:column;align-items:center;justify-content:center;height:100%;perspective:800px;overflow:hidden}
.chart-title{font-size:22px;font-weight:700;margin-bottom:24px;color:#fff;letter-spacing:-0.5px}
.chart-subtitle{font-size:13px;color:#888;margin-bottom:32px}
.chart-container{display:flex;align-items:flex-end;gap:clamp(8px,2vw,24px);padding:24px 32px;transform:rotateX(8deg) rotateY(-8deg);transform-style:preserve-3d;background:rgba(255,255,255,0.03);border-radius:16px;border:1px solid rgba(255,255,255,0.06);max-width:95vw;overflow-x:auto}
.bar-group{display:flex;flex-direction:column;align-items:center;animation:barIn 0.6s ease-out var(--delay) both}
.bar-wrapper{position:relative;display:flex;flex-direction:column;align-items:center}
.bar{width:clamp(28px,6vw,56px);border-radius:4px 4px 0 0;position:relative;transform-style:preserve-3d;transition:height 0.5s ease-out}
.bar-face-top{position:absolute;top:-6px;left:-2px;right:-2px;height:8px;border-radius:4px 4px 0 0;transform:rotateX(45deg);transform-origin:bottom}
.bar-face-right{position:absolute;top:0;right:-6px;bottom:0;width:8px;border-radius:0 4px 4px 0;transform:rotateY(45deg);transform-origin:left}
.bar-value{font-size:12px;font-weight:600;color:#fff;margin-bottom:6px;white-space:nowrap}
.bar-label{font-size:11px;color:#aaa;margin-top:8px;text-align:center;max-width:72px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
@keyframes barIn{from{opacity:0;transform:scaleY(0) translateY(20px)}to{opacity:1;transform:scaleY(1) translateY(0)}}
.total-row{margin-top:24px;font-size:14px;color:#aaa}
.total-row strong{color:#fff;font-size:16px}
</style>
<div class="chart-title">${escapeHtml(title)}</div>
<div class="chart-subtitle">${data.length} data points</div>
<div class="chart-container">
${bars}
</div>
<div class="total-row">Total: <strong>${formatNumber(data.reduce((s, d) => s + d.value, 0))}</strong></div>
${BRANDING}`;

  return {
    html: htmlShell(title, body),
    title,
    type: "chart-3d",
  };
}

// ── scene ────────────────────────────────────────────────────────────────────

function generateScene(input: ArtifactInput): ArtifactResult {
  const model = input.modelUrl || DEFAULT_MODEL;
  const title = input.title || "3D Scene";
  const opts = input.options || {};
  const bg = opts.backgroundColor || "#1a1a2e";
  const autoRotate = opts.autoRotate !== false;
  const ar = opts.ar !== false;
  const orbit = opts.cameraOrbit || "0deg 75deg 150%";

  const arAttrs = ar ? `ar ar-modes="webxr scene-viewer quick-look"` : "";
  const rotateAttr = autoRotate ? `auto-rotate auto-rotate-delay="0"` : "";

  const body = `
<style>
model-viewer{width:100%;height:100%;background:${bg};--poster-color:${bg}}
.scene-title{position:absolute;top:16px;left:16px;font-size:20px;font-weight:700;color:#fff;text-shadow:0 2px 12px rgba(0,0,0,0.6);z-index:10;letter-spacing:-0.3px}
.scene-info{position:absolute;top:44px;left:16px;font-size:12px;color:#aaa;z-index:10}
.controls-hint{position:absolute;bottom:16px;left:16px;font-size:11px;color:#666;z-index:10}
.ar-btn{position:absolute;bottom:16px;right:16px;padding:10px 20px;background:rgba(66,133,244,0.9);color:#fff;border:none;border-radius:24px;font-size:13px;font-weight:500;cursor:pointer;z-index:10}
</style>
<script type="module" src="${MODEL_VIEWER_CDN}"></script>
<div class="scene-title">${escapeHtml(title)}</div>
<div class="scene-info">Interactive 3D Scene</div>
<model-viewer
  src="${escapeHtml(model)}"
  camera-controls
  touch-action="pan-y"
  camera-orbit="${escapeHtml(orbit)}"
  shadow-intensity="1.5"
  shadow-softness="0.8"
  exposure="1.1"
  environment-image="neutral"
  tone-mapping="commerce"
  ${rotateAttr}
  ${arAttrs}
  style="width:100%;height:100%"
>
  ${ar ? `<button slot="ar-button" class="ar-btn">View in AR</button>` : ""}
</model-viewer>
<div class="controls-hint">Drag to orbit &bull; Scroll to zoom &bull; Two-finger to pan</div>
${BRANDING}`;

  return {
    html: htmlShell(title, body),
    title,
    type: "scene",
  };
}

// ── product-360 ──────────────────────────────────────────────────────────────

function generateProduct360(input: ArtifactInput): ArtifactResult {
  const model = input.modelUrl || DEFAULT_MODEL;
  const title = input.title || "Product 360\u00B0 View";
  const opts = input.options || {};
  const bg = opts.backgroundColor || "#1a1a2e";
  const orbit = opts.cameraOrbit || "0deg 75deg 105%";
  const ar = opts.ar !== false;
  const hotspots = input.hotspots || [];

  const hotspotHtml = hotspots
    .map(
      (h, i) => `
  <button class="hotspot" slot="hotspot-${i}"
    data-position="${escapeHtml(h.position)}"
    data-normal="${escapeHtml(h.normal)}"
    data-visibility-attribute="visible">
    <div class="hotspot-annotation">
      <div class="hotspot-title">${escapeHtml(h.label)}</div>
      ${h.description ? `<div class="hotspot-desc">${escapeHtml(h.description)}</div>` : ""}
    </div>
  </button>`
    )
    .join("\n");

  const body = `
<style>
model-viewer{width:100%;height:100%;background:${bg};--poster-color:${bg}}
.product-header{position:absolute;top:0;left:0;right:0;padding:16px 20px;background:linear-gradient(180deg,rgba(0,0,0,0.6),transparent);z-index:10}
.product-title{font-size:20px;font-weight:700;color:#fff}
.product-subtitle{font-size:12px;color:#aaa;margin-top:2px}
.hotspot{display:block;width:24px;height:24px;border-radius:50%;border:2px solid #fff;background:rgba(66,133,244,0.8);cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,0.4);transition:transform 0.2s}
.hotspot:hover{transform:scale(1.3)}
.hotspot-annotation{position:absolute;bottom:32px;left:50%;transform:translateX(-50%);background:rgba(30,30,50,0.95);border:1px solid rgba(255,255,255,0.1);border-radius:8px;padding:8px 12px;white-space:nowrap;pointer-events:none;opacity:0;transition:opacity 0.2s}
.hotspot:hover .hotspot-annotation{opacity:1}
.hotspot-title{font-size:13px;font-weight:600;color:#fff}
.hotspot-desc{font-size:11px;color:#aaa;margin-top:2px}
.ar-strip{position:absolute;bottom:0;left:0;right:0;padding:12px 20px;background:linear-gradient(0deg,rgba(0,0,0,0.6),transparent);display:flex;align-items:center;justify-content:space-between;z-index:10}
.ar-btn{padding:10px 24px;background:rgba(66,133,244,0.9);color:#fff;border:none;border-radius:24px;font-size:14px;font-weight:500;cursor:pointer}
.ar-btn:hover{background:rgba(66,133,244,1)}
.rotate-hint{font-size:11px;color:#666}
</style>
<script type="module" src="${MODEL_VIEWER_CDN}"></script>
<div class="product-header">
  <div class="product-title">${escapeHtml(title)}</div>
  <div class="product-subtitle">Drag to rotate &bull; Pinch to zoom${hotspots.length > 0 ? " &bull; Tap hotspots for details" : ""}</div>
</div>
<model-viewer
  src="${escapeHtml(model)}"
  camera-controls
  touch-action="pan-y"
  camera-orbit="${escapeHtml(orbit)}"
  auto-rotate
  auto-rotate-delay="3000"
  shadow-intensity="1"
  shadow-softness="0.5"
  exposure="1"
  environment-image="neutral"
  tone-mapping="commerce"
  interaction-prompt="auto"
  ${ar ? `ar ar-modes="webxr scene-viewer quick-look"` : ""}
  style="width:100%;height:100%"
>
  ${hotspotHtml}
  ${ar ? `<button slot="ar-button" class="ar-btn">View in Your Space</button>` : ""}
</model-viewer>
<div class="ar-strip">
  <div class="rotate-hint">360&deg; interactive view</div>
</div>
${BRANDING}`;

  return {
    html: htmlShell(title, body),
    title,
    type: "product-360",
  };
}

// ─── Utilities ───────────────────────────────────────────────────────────────

function formatNumber(n: number): string {
  if (n >= 1_000_000) return (n / 1_000_000).toFixed(1).replace(/\.0$/, "") + "M";
  if (n >= 1_000) return (n / 1_000).toFixed(1).replace(/\.0$/, "") + "K";
  return n.toLocaleString("en-US");
}

/** Darken a hex colour by a fraction (0..1). */
function darken(hex: string, amount: number): string {
  const rgb = hexToRgb(hex);
  return rgbToHex(
    Math.round(rgb.r * (1 - amount)),
    Math.round(rgb.g * (1 - amount)),
    Math.round(rgb.b * (1 - amount)),
  );
}

/** Lighten a hex colour by a fraction (0..1). */
function lighten(hex: string, amount: number): string {
  const rgb = hexToRgb(hex);
  return rgbToHex(
    Math.min(255, Math.round(rgb.r + (255 - rgb.r) * amount)),
    Math.min(255, Math.round(rgb.g + (255 - rgb.g) * amount)),
    Math.min(255, Math.round(rgb.b + (255 - rgb.b) * amount)),
  );
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const h = hex.replace("#", "");
  return {
    r: parseInt(h.substring(0, 2), 16),
    g: parseInt(h.substring(2, 4), 16),
    b: parseInt(h.substring(4, 6), 16),
  };
}

function rgbToHex(r: number, g: number, b: number): string {
  return "#" + [r, g, b].map((c) => c.toString(16).padStart(2, "0")).join("");
}

// ─── Format response ─────────────────────────────────────────────────────────

export function formatArtifactResponse(result: ArtifactResult): string {
  const lines: string[] = [
    `## ${result.title}`,
    ``,
    `Here is your interactive 3D ${result.type === "chart-3d" ? "chart" : "content"}:`,
    ``,
    `\`\`\`html`,
    result.html,
    `\`\`\``,
    ``,
    `**How to use this:**`,
    `- Copy the HTML above into a file and open in a browser`,
    `- Or paste into any HTML preview tool`,
    `- On mobile: tap "View in AR" to see it in your space`,
    `- Drag to orbit, scroll/pinch to zoom`,
    ``,
    `---`,
    `*Powered by SceneView \u2014 3D & AR for every platform*`,
  ];
  return lines.join("\n");
}
