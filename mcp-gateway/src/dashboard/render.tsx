/** @jsxImportSource hono/jsx */

/**
 * Helpers that turn a Hono JSX element into a full HTML document.
 *
 * Hono JSX nodes expose a `.toString()` that emits a string or, for
 * async subtrees, a `Promise<string>`. We always await before prepending
 * the doctype so every caller can rely on a flat `string` return type.
 */

/**
 * `HtmlEscapedString` is Hono's internal tagged string returned by JSX
 * rendering. It extends `String` and exposes `toString()` which may
 * return a plain `string` or a `Promise<string>` when the tree contains
 * async suspense boundaries. We type the parameter loosely to stay
 * compatible with both Hono JSX 4.x variants.
 */
type Renderable = { toString(): string | Promise<string> };

/** Renders a JSX element to a complete HTML document string. */
export async function renderToHtml(element: Renderable): Promise<string> {
  const body = await Promise.resolve(element.toString());
  return `<!doctype html>${body}`;
}
