# GitHub Sponsors — Tier Setup Guide

> **Time required:** ~2 minutes
> **URL:** https://github.com/sponsors/ThomasGorisse/dashboard/tiers

## Step 1: Retire old tiers

Go to https://github.com/sponsors/ThomasGorisse/dashboard/tiers and retire (or edit)
the outdated tiers that reference private repos, Slack, and French consulting.

Old tiers to retire:
- $5 one-time "Contribution" (references private repo)
- $5/mo "Individual" (references private repo + Slack)
- $99/mo "Company" (references private repo + Slack)
- $250 one-time "French consulting"
- $350 one-time "English consulting"
- $1,500+ one-time "Conference talks"
- $1,999 one-time "Thank you"
- $3,000 one-time "English conference"
- $5,000 one-time "Large contract"

> If any tier has active sponsors, edit it instead of retiring it.

## Step 2: Create new monthly tiers

Click **"Add a tier"** for each one. Copy the description exactly.

---

### Tier 1 — $5/month

**Name:** Supporter

**Description** (paste as Markdown):
```markdown
# Supporter

Thank you for supporting SceneView!

- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Early access to release notes (1 day before public)
```

---

### Tier 2 — $15/month

**Name:** Pro Developer

**Description** (paste as Markdown):
```markdown
# Pro Developer

For developers who rely on SceneView in production.

- Everything in Supporter, plus:
- Priority responses on GitHub issues
- Vote on roadmap priorities (monthly poll)
- Access to private sponsors-only Discord channel
```

---

### Tier 3 — $50/month

**Name:** Team

**Description** (paste as Markdown):
```markdown
# Team

For teams and startups building with SceneView.

- Everything in Pro Developer, plus:
- Your logo on the README and website
- 1 priority bug fix per month
- Early access to pre-release builds
```

---

### Tier 4 — $200/month

**Name:** Enterprise Sponsor

**Description** (paste as Markdown):
```markdown
# Enterprise Sponsor

For companies that depend on SceneView at scale.

- Everything in Team, plus:
- Logo prominently displayed on website hero section
- Monthly 30-minute support call (video or async)
- Feature request priority consideration
- "Powered by SceneView" case study opportunity
```

---

## Step 3: Publish

Make sure **"Publish"** is checked for each tier, then save.

## Why not automated?

The GitHub GraphQL `createSponsorsTier` mutation requires the `user` OAuth scope,
which the CLI token does not have. The dashboard UI is the simplest path.

## API reference (for future automation)

If you add the `user` scope to your token, tiers can be created with:

```bash
gh api graphql -f query='
mutation {
  createSponsorsTier(input: {
    sponsorableLogin: "ThomasGorisse",
    amount: 5,
    isRecurring: true,
    description: "# Supporter\n\nThank you for supporting SceneView!\n\n- Your name listed in SPONSORS.md\n- Sponsor badge on your GitHub profile\n- Early access to release notes",
    publish: true
  }) {
    sponsorsTier { id name monthlyPriceInDollars }
  }
}'
```

Repeat for $15, $50, $200 with their respective descriptions.
