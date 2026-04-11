/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import { Layout } from "./layout.js";
import { renderToHtml } from "./render.js";

/** `/login` — email magic link form. */
export const LoginPage: FC<{
  message?: string | null;
  next?: string | null;
}> = ({ message, next }) => (
  <Layout
    title="Sign in"
    description="Sign in to your SceneView MCP dashboard via a magic link."
    active="login"
  >
    <div class="form-card">
      <h1 style="text-align:center;margin-top:0;">Sign in</h1>
      <p style="text-align:center;">
        Enter your email and we'll send you a sign-in link.
      </p>
      {message ? (
        <div class="alert" role="status">
          {message}
        </div>
      ) : null}
      <form method="post" action="/login">
        <label for="email">Email</label>
        <input
          id="email"
          type="email"
          name="email"
          placeholder="you@example.com"
          required
          autocomplete="email"
        />
        {next ? <input type="hidden" name="next" value={next} /> : null}
        <button type="submit" class="btn btn-primary">
          Send magic link
        </button>
      </form>
      <p
        style="font-size:.75rem;color:var(--sv-fg-muted);margin-top:1.5rem;text-align:center;"
      >
        We use a passwordless sign-in flow. No password to remember or
        leak.
      </p>
    </div>
  </Layout>
);

/** Renders the login page to a full HTML document string. */
export function renderLoginPage(
  message: string | null,
  next: string | null,
): Promise<string> {
  return renderToHtml(<LoginPage message={message} next={next} />);
}
