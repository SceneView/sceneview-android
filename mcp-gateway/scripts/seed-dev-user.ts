/**
 * Local dev seeder.
 *
 * Creates a user row and one API key in the LOCAL D1 database so you
 * can smoke-test the gateway without going through the magic-link
 * flow. Writes the plaintext key to stdout so you can paste it into
 * your MCP client config.
 *
 * Usage:
 *
 *   # 1. Start a one-shot wrangler dev session so the D1 file exists
 *   #    (Ctrl-C after the "Ready on http://localhost:8787" message)
 *   npm run dev --silent -- --experimental-local &
 *   sleep 2
 *   kill %1 || true
 *
 *   # 2. Run the seeder — it uses sqlite directly against the
 *   #    wrangler-managed local D1 file.
 *   node --experimental-strip-types scripts/seed-dev-user.ts \
 *     dev@example.com
 *
 * The script intentionally does not touch the Workers runtime — it
 * talks to the raw SQLite file wrangler keeps under `.wrangler/state`
 * so it works even when wrangler is not running.
 */

import Database from "better-sqlite3";
import { readdirSync, statSync } from "node:fs";
import { join } from "node:path";

const WRANGLER_STATE = ".wrangler/state/v3/d1";

/** Finds the first .sqlite file under .wrangler/state/v3/d1 */
function findLocalD1(): string {
  try {
    const dbs = readdirSync(WRANGLER_STATE);
    for (const dir of dbs) {
      const full = join(WRANGLER_STATE, dir);
      const stat = statSync(full);
      if (!stat.isDirectory()) continue;
      const files = readdirSync(full);
      const sqlite = files.find((f) => f.endsWith(".sqlite"));
      if (sqlite) return join(full, sqlite);
    }
  } catch {
    // Fall through to error message below.
  }
  throw new Error(
    "No local D1 database found. Start wrangler dev once so the SQLite " +
      "file is created, then re-run this seeder.",
  );
}

async function sha256Hex(input: string): Promise<string> {
  const digest = await crypto.subtle.digest(
    "SHA-256",
    new TextEncoder().encode(input),
  );
  const bytes = new Uint8Array(digest);
  const hex: string[] = [];
  for (const b of bytes) hex.push(b.toString(16).padStart(2, "0"));
  return hex.join("");
}

function base32Encode(bytes: Uint8Array): string {
  const ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  let bits = 0;
  let value = 0;
  let out = "";
  for (const b of bytes) {
    value = (value << 8) | b;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      out += ALPHABET[(value >> bits) & 31];
    }
  }
  if (bits > 0) out += ALPHABET[(value << (5 - bits)) & 31];
  return out;
}

async function main() {
  const email = process.argv[2] ?? "dev@example.com";
  const tier = (process.argv[3] ?? "pro") as "free" | "pro" | "team";
  const dbPath = findLocalD1();
  process.stderr.write(`Using D1 file: ${dbPath}\n`);
  const sqlite = new Database(dbPath);

  const now = Date.now();
  const userIdBytes = new Uint8Array(8);
  crypto.getRandomValues(userIdBytes);
  const userId = `usr_${base32Encode(userIdBytes).slice(0, 12).toLowerCase()}`;

  const keyBytes = new Uint8Array(20);
  crypto.getRandomValues(keyBytes);
  const keyBody = base32Encode(keyBytes).slice(0, 32);
  const plaintext = `sv_live_${keyBody}`;
  const keyPrefix = plaintext.slice(0, 14);
  const keyHash = await sha256Hex(plaintext);

  const keyIdBytes = new Uint8Array(8);
  crypto.getRandomValues(keyIdBytes);
  const keyId = `key_${base32Encode(keyIdBytes).slice(0, 12).toLowerCase()}`;

  sqlite
    .prepare(
      "INSERT INTO users (id, email, stripe_customer_id, tier, created_at, updated_at) VALUES (?, ?, NULL, ?, ?, ?)",
    )
    .run(userId, email, tier, now, now);
  sqlite
    .prepare(
      "INSERT INTO api_keys (id, user_id, name, key_hash, key_prefix, created_at) VALUES (?, ?, ?, ?, ?, ?)",
    )
    .run(keyId, userId, "dev", keyHash, keyPrefix, now);
  sqlite.close();

  process.stdout.write(
    `Seeded user=${userId} email=${email} tier=${tier}\nAPI key: ${plaintext}\n`,
  );
}

main().catch((err) => {
  process.stderr.write(`${err instanceof Error ? err.message : String(err)}\n`);
  process.exit(1);
});
