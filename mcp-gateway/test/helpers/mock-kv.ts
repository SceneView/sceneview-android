/**
 * Minimal in-memory KV mock compatible with the subset of
 * `KVNamespace` used by the gateway.
 *
 * The real Workers KV namespace is richer (list, metadata, cacheTtl,
 * ...) but this mock implements just enough to run unit tests without
 * Miniflare. It mirrors the semantics we depend on:
 *
 *   - `get(key)` returns `null` when the key is missing or expired.
 *   - `put(key, value, { expirationTtl })` stores the value and honors
 *     the TTL by lazily expiring entries on the next `get`.
 *   - `delete(key)` removes an entry.
 *
 * Consumers can inspect `store` directly to assert against side effects.
 */

export interface MockKvEntry {
  value: string;
  expiresAt?: number;
}

export class MockKv {
  public readonly store = new Map<string, MockKvEntry>();

  async get(key: string, _type?: "text"): Promise<string | null> {
    const entry = this.store.get(key);
    if (!entry) return null;
    if (entry.expiresAt !== undefined && Date.now() > entry.expiresAt) {
      this.store.delete(key);
      return null;
    }
    return entry.value;
  }

  async put(
    key: string,
    value: string,
    options?: { expirationTtl?: number },
  ): Promise<void> {
    const entry: MockKvEntry = { value };
    if (options?.expirationTtl) {
      entry.expiresAt = Date.now() + options.expirationTtl * 1000;
    }
    this.store.set(key, entry);
  }

  async delete(key: string): Promise<void> {
    this.store.delete(key);
  }

  /** Returns a shallow clone cast to the opaque `KVNamespace` interface. */
  asKv(): KVNamespace {
    return this as unknown as KVNamespace;
  }
}
