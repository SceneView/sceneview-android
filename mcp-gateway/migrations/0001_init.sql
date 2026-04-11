-- 0001_init.sql
-- Core accounts table and Stripe-backed subscription records.
--
-- All timestamps are stored as INTEGER unix seconds (or ms) so D1
-- reads do not pay for SQLite date-parsing overhead. Prefer `Date.now()`
-- on the write side.

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,                  -- usr_xxx (nanoid)
  email TEXT NOT NULL UNIQUE,
  stripe_customer_id TEXT UNIQUE,
  tier TEXT NOT NULL DEFAULT 'free',    -- 'free' | 'pro' | 'team'
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_stripe ON users(stripe_customer_id);

CREATE TABLE IF NOT EXISTS subscriptions (
  id TEXT PRIMARY KEY,                  -- sub_xxx
  user_id TEXT NOT NULL REFERENCES users(id),
  stripe_subscription_id TEXT NOT NULL UNIQUE,
  stripe_price_id TEXT NOT NULL,
  tier TEXT NOT NULL,
  status TEXT NOT NULL,                 -- active, trialing, past_due, canceled
  current_period_end INTEGER NOT NULL,
  cancel_at_period_end INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_subs_user ON subscriptions(user_id);
