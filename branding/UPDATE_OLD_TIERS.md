# Update Old GitHub Sponsors Tiers

> **Time required:** ~5 minutes
> **URL:** https://github.com/sponsors/ThomasGorisse/dashboard/tiers
>
> **Why manual?** The GitHub GraphQL API has `retireSponsorsTier` and
> `createSponsorsTier` mutations, but no `updateSponsorsTier`. The current
> CLI token also lacks the `user` scope needed for any sponsor mutations.
> Updating tier descriptions must be done through the dashboard UI.

## Important rule

**No tier may promise any work obligation.** Thomas has a full-time job (CDI)
and cannot commit to support, bug fixes, SLAs, priority responses, or calls
for sponsors. All benefits must be **recognition-only** (name/logo placement,
badges, voting rights, early visibility).

---

## Tier 1: $5 one-time — EDIT description

**Tier ID:** `ST_kwDOAGSrmc4AAaLT`
**Action:** Edit (not retire — someone may have used it)

Go to the tier's edit page and replace the description with:

```markdown
### One-Time Supporter

Thank you for contributing to SceneView!

- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
```

---

## Tier 2: $5/month — EDIT description

**Tier ID:** `ST_kwDOAGSrmc4AAf8R`
**Action:** Edit description

Replace the current description (which mentions "private SceneView Android
repository" and "Premium support on Slack") with:

```markdown
### Supporter

Thank you for supporting SceneView!

- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
```

---

## Tier 3: $15/month — EDIT description (remove work promises)

**Tier ID:** `ST_kwDOAGSrmc4ACSfW`
**Action:** Edit description

The current description promises "Priority GitHub issue responses" which is a
work obligation. Replace with:

```markdown
### Pro Developer

For developers using SceneView in production.

- Vote on roadmap priorities
- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Early access to release notes
```

---

## Tier 4: $50/month — EDIT description (remove work promises)

**Tier ID:** `ST_kwDOAGSrmc4ACSfX`
**Action:** Edit description

The current description promises "Priority bug fixes (1/month)" and "Priority
GitHub issue responses" which are work obligations. Replace with:

```markdown
### Team / Startup

For teams using SceneView in production apps.

- Your company logo on the README and website
- Vote on roadmap priorities
- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Early access to release notes
```

---

## Tier 5: $99/month — RETIRE

**Tier ID:** `ST_kwDOAGSrmc4AAZxd`
**Action:** Retire

This tier is redundant — we now have $50/month (Team) and $200/month
(Enterprise). The description also references "private SceneView Android
repository" and "Premium support on Slack" which no longer exist.

1. Go to https://github.com/sponsors/ThomasGorisse/dashboard/tiers
2. Click the $99/month "Company" tier
3. Click **"Retire tier"**
4. Confirm retirement

If it has active sponsors, edit the description first to match the $50 tier
and change the price if GitHub allows it, or contact the sponsors to migrate.

---

## Tier 6: $200/month — EDIT description (remove work promises)

**Tier ID:** `ST_kwDOAGSrmc4ACSfY`
**Action:** Edit description

The current description is mostly good but should not promise any obligation.
Replace with:

```markdown
### Enterprise Sponsor

Maximum visibility for your company across the SceneView ecosystem.

- **Logo prominently displayed** on website hero section
- **Logo on README** (seen by 10k+ monthly visitors)
- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Vote on roadmap priorities
- Mentioned in release announcements
```

---

## One-time tiers: RETIRE all consulting/conference tiers

These promise work obligations (pair-programming, consulting, conference talks):

| Tier | ID | Action |
|---|---|---|
| $250 one-time (French consulting) | `ST_kwDOAGSrmc4AAS2O` | Retire |
| $350 one-time (English consulting) | `ST_kwDOAGSrmc4AAS2N` | Retire |
| $1,500 one-time (French conference) | `ST_kwDOAGSrmc4AAS2P` | Retire |
| $1,999 one-time (Thank you) | `ST_kwDOAGSrmc4AAS2b` | Keep as-is (no obligation) |

The $1,999 "Thank you" tier is fine -- it's just a gratitude tier with no
work promises.

---

## After updating

1. Verify each tier description at https://github.com/sponsors/ThomasGorisse
2. Check that no tier mentions: private repos, Slack, priority support,
   bug fixes, consulting, pair-programming, or any other work obligation
3. Update `branding/SPONSOR_TIERS.md` to remove outdated descriptions

## Future automation

If you add the `user` scope to the GitHub CLI token, you can use:
- `retireSponsorsTier(input: { tierId: "..." })` to retire tiers
- `createSponsorsTier(input: { ... })` to create new ones

There is still no `updateSponsorsTier` mutation in the GitHub GraphQL API,
so editing existing tier descriptions will always require the dashboard UI.
