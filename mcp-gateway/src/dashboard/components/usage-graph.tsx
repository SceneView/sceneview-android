/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";

/** One day of usage in the sparkline. */
export interface UsageBucket {
  /** ISO date (YYYY-MM-DD) used as the x-axis label. */
  day: string;
  /** Number of successful tool calls on that day. */
  count: number;
}

/**
 * Renders an SVG sparkline of the last 30 days.
 *
 * The chart is computed server-side: we pre-compute the path `d`
 * attribute and emit it directly to the DOM. No client-side JS needed.
 * If the series is empty we draw a helpful empty-state.
 */
export const UsageGraph: FC<{ buckets: UsageBucket[] }> = ({ buckets }) => {
  const width = 720;
  const height = 160;
  const padding = 16;

  const totalCount = buckets.reduce((sum, b) => sum + b.count, 0);
  if (buckets.length === 0 || totalCount === 0) {
    return (
      <div class="card" style="text-align:center;color:var(--sv-fg-muted);">
        No usage yet. Point your MCP client at the gateway to see calls
        appear here.
      </div>
    );
  }

  const max = Math.max(1, ...buckets.map((b) => b.count));
  const stepX = (width - padding * 2) / Math.max(1, buckets.length - 1);
  const scaleY = (v: number): number =>
    height - padding - (v / max) * (height - padding * 2);

  const points = buckets.map((b, i) => ({
    x: padding + i * stepX,
    y: scaleY(b.count),
  }));

  const path = points
    .map((p, i) => `${i === 0 ? "M" : "L"}${p.x.toFixed(1)},${p.y.toFixed(1)}`)
    .join(" ");

  const areaPath =
    `${path} L${points[points.length - 1].x.toFixed(1)},${height - padding} ` +
    `L${points[0].x.toFixed(1)},${height - padding} Z`;

  const total = buckets.reduce((sum, b) => sum + b.count, 0);

  return (
    <div class="card">
      <h3 style="margin-top:0;">Last 30 days</h3>
      <p style="margin:.25rem 0 1rem 0;">
        <strong style="color:var(--sv-fg);font-size:1.5rem;">{total}</strong>{" "}
        successful tool calls
      </p>
      <svg
        class="usage-graph"
        viewBox={`0 0 ${width} ${height}`}
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-label="Usage over the last 30 days"
      >
        <path d={areaPath} fill="rgba(30,64,175,0.14)" />
        <path
          d={path}
          fill="none"
          stroke="var(--sv-primary)"
          stroke-width="2"
          stroke-linejoin="round"
          stroke-linecap="round"
        />
        {points.map((p) => (
          <circle cx={p.x.toFixed(1)} cy={p.y.toFixed(1)} r="2.5" fill="var(--sv-primary)" />
        ))}
      </svg>
    </div>
  );
};
