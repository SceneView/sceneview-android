/** @jsxImportSource hono/jsx */

import type { FC } from "hono/jsx";
import type { ApiKeyRow } from "../../db/schema.js";

/** Renders a single row of the API keys table. */
export const KeyRow: FC<{ row: ApiKeyRow }> = ({ row }) => {
  const revoked = row.revoked_at !== null && row.revoked_at !== undefined;
  return (
    <tr id={`key-${row.id}`}>
      <td>
        <code>{row.key_prefix}…</code>
      </td>
      <td>{row.name}</td>
      <td>{formatDate(row.created_at)}</td>
      <td>{row.last_used_at ? formatDate(row.last_used_at) : "—"}</td>
      <td>
        {revoked ? (
          <span class="tier-badge tier-badge--free">revoked</span>
        ) : (
          <button
            type="button"
            class="btn btn-secondary"
            hx-post={`/dashboard/keys/${row.id}/revoke`}
            hx-target={`#key-${row.id}`}
            hx-swap="outerHTML"
            hx-confirm="Revoke this key? Calls using it will fail within 5 minutes."
          >
            Revoke
          </button>
        )}
      </td>
    </tr>
  );
};

/**
 * Renders a freshly created key inside an alert so the plaintext can be
 * copied once and never shown again.
 */
export const NewKeyAlert: FC<{ plaintext: string; name: string }> = ({
  plaintext,
  name,
}) => (
  <div class="alert alert--success">
    <strong>API key created.</strong> Copy this value now — it will not be
    shown again.
    <pre style="margin-top:.75rem;">
      <code>{plaintext}</code>
    </pre>
    <p style="margin:0;font-size:.875rem;">Name: {name}</p>
  </div>
);

function formatDate(ms: number): string {
  const d = new Date(ms);
  return d.toISOString().slice(0, 10);
}
