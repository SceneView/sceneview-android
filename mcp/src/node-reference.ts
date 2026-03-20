/**
 * node-reference.ts
 *
 * Parses llms.txt to extract per-node-type API reference sections.
 * Each section starts at a `###` heading and ends just before the next `###` or `##` heading.
 */

export interface NodeSection {
  /** Canonical name as it appears in the `###` heading (e.g. "ModelNode") */
  name: string;
  /** Full heading line (e.g. "ModelNode" or "LightNode") */
  heading: string;
  /** Raw markdown content of the section (heading + body) */
  content: string;
}

// ─── Parsing ─────────────────────────────────────────────────────────────────

/**
 * Extract the bare node-type name from a `###` heading line.
 * Examples:
 *   "Scene — 3D viewport"        → "Scene"
 *   "HitResultNode — surface cursor" → "HitResultNode"
 *   "Primitive geometry nodes"   → "Primitive geometry nodes"
 */
function headingToName(heading: string): string {
  return heading.split(/\s*[—–-]\s*/)[0].trim();
}

/**
 * Parse all `###`-level sections from the full llms.txt content.
 * Returns a map keyed by the bare node-type name (case-insensitive lookup
 * handled separately).
 */
export function parseNodeSections(llmsTxt: string): Map<string, NodeSection> {
  const sections = new Map<string, NodeSection>();
  const lines = llmsTxt.split("\n");

  let i = 0;
  while (i < lines.length) {
    const line = lines[i];

    if (line.startsWith("### ")) {
      const headingText = line.slice(4).trim(); // text after "### "
      const name = headingToName(headingText);

      // Collect lines until the next `##` or `###` heading
      const bodyLines: string[] = [line];
      i++;
      while (i < lines.length && !lines[i].startsWith("## ") && !lines[i].startsWith("### ")) {
        bodyLines.push(lines[i]);
        i++;
      }

      // Trim trailing blank lines
      while (bodyLines.length > 1 && bodyLines[bodyLines.length - 1].trim() === "") {
        bodyLines.pop();
      }

      sections.set(name.toLowerCase(), {
        name,
        heading: headingText,
        content: bodyLines.join("\n"),
      });
    } else {
      i++;
    }
  }

  return sections;
}

// ─── Public API ──────────────────────────────────────────────────────────────

/**
 * Look up a node type section by name (case-insensitive).
 *
 * @param sections - The map returned by `parseNodeSections`
 * @param nodeType - User-supplied name, e.g. "ModelNode", "lightnode", "ARScene"
 * @returns The matching `NodeSection`, or `undefined` if not found.
 */
export function findNodeSection(
  sections: Map<string, NodeSection>,
  nodeType: string
): NodeSection | undefined {
  return sections.get(nodeType.toLowerCase());
}

/**
 * Returns a sorted array of all known node-type names.
 */
export function listNodeTypes(sections: Map<string, NodeSection>): string[] {
  return Array.from(sections.values())
    .map((s) => s.name)
    .sort();
}
