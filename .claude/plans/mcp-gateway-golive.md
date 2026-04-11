# mcp-gateway — plan de go-live

**Contexte.** Sprint `mcp-gateway` trouvé le 2026-04-11 avec 18 commits orphelins dans le worktree `agent-ae442902` (branche `worktree-agent-ae442902`). Le code est complet mais jamais mergé sur `main`. Ce plan documente ce qui reste à faire pour passer en production. Toutes les étapes sensibles (secrets, deploy, stripe) restent manuelles — aucun agent ne doit provisionner de l'infra ou charger des secrets.

## Architecture (résumé)

- Cloudflare Worker Hono hébergeant les 5 MCPs (sceneview + automotive + gaming + healthcare + interior) en un endpoint unique
- D1 pour users/api_keys/usage + KV pour rate limit + Resend pour magic-link
- Stripe checkout + portal + webhooks → upgrade tier (free/pro/team)
- Lite package `sceneview-mcp@4.0.0-beta.1` en mode proxy : le user garde `npx sceneview-mcp` dans Claude Desktop, mais les appels sont proxifiés vers le gateway après auth par clé API
- Dashboard HTMX/JSX pour gestion du compte et du billing

## Code source

Worktree complet : `/Users/thomasgorisse/Projects/sceneview/.claude/worktrees/agent-ae442902/mcp-gateway/`

Structure :
```
mcp-gateway/
├── src/
│   ├── index.ts                 # Hono app (health, mcp, dashboard, auth, billing, webhooks)
│   ├── env.ts                   # Typed env bindings
│   ├── auth/                    # Magic-link + JWT middleware + API key auth
│   ├── billing/                 # stripe-client, tiers, checkout, portal, webhook
│   ├── dashboard/               # HTMX JSX pages (login, account, billing)
│   ├── db/                      # schema, repositories
│   ├── mcp/                     # MCP streamable HTTP transport + tool registry
│   ├── middleware/              # rate-limit, error handling
│   ├── rate-limit/              # sliding window + monthly quota
│   └── routes/                  # auth.ts, billing.ts, dashboard.tsx, mcp.ts, webhooks.ts
├── migrations/                  # D1 schema migrations
├── scripts/
│   ├── bootstrap-d1.sh          # Provisionne la D1 en une commande
│   └── seed-dev-user.ts         # Crée un user dev pour test local
├── test/                        # Vitest tests (E2E + unit)
├── wrangler.toml                # Config Cloudflare (avec placeholders)
└── package.json
```

## Étapes bloquantes pour le go-live

Toutes à faire manuellement par Thomas (pas d'agent).

### 1 — Provisionner D1
```
cd mcp-gateway
wrangler d1 create sceneview-mcp
```
→ copier l'ID affiché, coller dans `wrangler.toml` à la place de `REPLACE_WITH_D1_ID`.

### 2 — Provisionner KV
```
wrangler kv namespace create RL_KV
```
→ copier l'ID, coller à la place de `REPLACE_WITH_KV_ID`.

### 3 — Appliquer les migrations D1
```
bash scripts/bootstrap-d1.sh
```
(ou `wrangler d1 migrations apply sceneview-mcp`)

### 4 — Créer les produits Stripe
Dans le dashboard Stripe (live mode) :
- **SceneView MCP Pro** — recurring — 2 prices : monthly + yearly
- **SceneView MCP Team** — recurring — 2 prices : monthly + yearly

Pour chaque price, copier le `price_id` (`price_1XYZ...`) et coller dans `wrangler.toml` :
```toml
STRIPE_PRICE_PRO_MONTHLY  = "price_..."
STRIPE_PRICE_PRO_YEARLY   = "price_..."
STRIPE_PRICE_TEAM_MONTHLY = "price_..."
STRIPE_PRICE_TEAM_YEARLY  = "price_..."
```

### 5 — Charger les secrets Cloudflare
```
wrangler secret put JWT_SECRET             # 32+ bytes random (openssl rand -base64 48)
wrangler secret put RESEND_API_KEY         # Resend dashboard → API keys
wrangler secret put STRIPE_SECRET_KEY      # sk_live_... (JAMAIS dans le repo)
wrangler secret put STRIPE_WEBHOOK_SECRET  # whsec_... — créer après le deploy (étape 8)
```

### 6 — (Optionnel) DNS custom
Si on veut `https://mcp.sceneview.dev` au lieu de `sceneview-mcp.workers.dev` : ajouter une route custom dans le dashboard Cloudflare + CNAME DNS. Sinon, sauter.

### 7 — Deploy
```
cd mcp-gateway
wrangler deploy
```
→ note la URL publique (`https://sceneview-mcp.<account>.workers.dev`).

### 8 — Configurer le webhook Stripe
Dans le dashboard Stripe :
- Ajouter un endpoint `<deploy-url>/webhooks/stripe`
- Events à écouter : `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`, `invoice.payment_failed`
- Copier le `whsec_...` généré → `wrangler secret put STRIPE_WEBHOOK_SECRET`
- Redéployer : `wrangler deploy`

### 9 — Test E2E manuel
- Ouvrir `<deploy-url>/login` → entrer email → recevoir magic link → cliquer
- Dashboard → copier l'API key `sv_live_...`
- Configurer un Claude Desktop de test avec `env: { "SCENEVIEW_API_KEY": "sv_live_..." }`
- Faire un call MCP → vérifier qu'il passe (tier free)
- Cliquer "Upgrade to Pro" → Stripe checkout → payer en carte de test
- Vérifier que le webhook arrive et que le tier passe à `pro` en D1
- Faire un call MCP sur un tool pro → doit passer maintenant

### 10 — Publier le lite package
```
cd /path/to/repo
git checkout main
# cherry-pick le commit 31c08302 (v4.0.0-beta.1 lite package) ou rebaser tout
cd mcp
npm version 4.0.0-beta.1
npm publish --tag beta
```

### 11 — Merger le sprint gateway sur main
Soit via PR classique (après rebase), soit via cherry-pick sélectif des 18 commits. **À faire seulement après que le go-live fonctionne**, sinon on pollue main avec du code non testé en prod.

## Décisions de pricing à prendre AVANT l'étape 4

Le code supporte 4 plans. Thomas doit décider les prix réels (exemples — à valider) :
- Pro monthly : 9.99 €/mo
- Pro yearly : 99 €/yr (17% off)
- Team monthly : 29 €/mo (jusqu'à 5 seats ?)
- Team yearly : 290 €/yr

Les rate limits par tier à vérifier dans `src/rate-limit/` avant deploy — ajuster si nécessaire :
- Free : ? calls/day
- Pro : ? calls/day
- Team : ? calls/day

## Risques et garde-fous

- **Ne JAMAIS commiter de secrets**. Les 4 secrets ci-dessus n'existent que dans le dashboard Cloudflare, pas dans git.
- **Tester en mode Stripe test d'abord**. Le `wrangler.toml` peut être basculé en `STRIPE_SECRET_KEY=sk_test_...` pour une passe de validation avant live.
- **Surveiller D1 quotas**. D1 a un quota gratuit — si ça explose, provisionner le plan payant avant que ça casse.
- **Le lite package proxy doit rester backward-compat**. Un user qui upgrade `sceneview-mcp` sans avoir de clé API doit continuer à marcher en tier `free` (pas de regression).

## Après go-live

- Publier `sceneview-mcp@4.0.0-beta.1` sur npm avec tag `beta` (pas `latest`) — le temps que quelques utilisateurs testent
- Garder `sceneview-mcp@3.6.2` en `latest` encore 1 semaine
- Promo discrète : un post Reddit r/androiddev + r/claudeai (pas LinkedIn, Thomas n'est pas à l'aise) annonçant le gateway hosted et les tiers
- Monitoring : créer un dashboard Cloudflare + vérifier les logs D1 régulièrement pour détecter les abus
- Si tout se passe bien après 2 semaines : promote `4.x` en `latest`.

## Ce que ce plan N'inclut PAS

- Créer de la marketing copy supplémentaire (déjà fait dans le commit `488f7819`)
- Coder de nouveaux tools pro (le code actuel a déjà une séparation free/pro)
- Setup analytics détaillé (on a déjà la télémétrie opt-out dans `mcp/src/telemetry.ts` depuis le merge de #804)
- Creator Kit €29 et Starter Kit €49 sur Polar — **séparé de ce plan**, probablement à refaire proprement dans une session dédiée car les liens actuels sont cassés
