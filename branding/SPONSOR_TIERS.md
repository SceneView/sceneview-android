# GitHub Sponsors — Tier Reference

> **Dashboard:** https://github.com/sponsors/ThomasGorisse/dashboard/tiers
> **Public page:** https://github.com/sponsors/ThomasGorisse

## Rule: recognition-only benefits

Thomas has a full-time job (CDI). **No tier may promise any work obligation**
— no support, no bug fixes, no SLAs, no priority responses, no consulting.
All benefits are recognition-based: name/logo placement, badges, voting, and
early visibility.

## Monthly tiers

### $5/month — Supporter

```markdown
### Supporter

Thank you for supporting SceneView!

- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
```

### $15/month — Pro Developer

```markdown
### Pro Developer

For developers using SceneView in production.

- Vote on roadmap priorities
- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Early access to release notes
```

### $50/month — Team / Startup

```markdown
### Team / Startup

For teams using SceneView in production apps.

- Your company logo on the README and website
- Vote on roadmap priorities
- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
- Early access to release notes
```

### $99/month — RETIRED

This tier is redundant (between $50 and $200) and had outdated descriptions
referencing private repos and Slack. See `UPDATE_OLD_TIERS.md` for retirement
instructions.

### $200/month — Enterprise Sponsor

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

## One-time tiers

### $5 one-time — One-Time Supporter

```markdown
### One-Time Supporter

Thank you for contributing to SceneView!

- Your name listed in [SPONSORS.md](https://github.com/sceneview/sceneview/blob/main/SPONSORS.md)
- Sponsor badge on your GitHub profile
```

### $1,999 one-time — Thank You (keep as-is)

No work obligation — just a gratitude tier.

### Consulting/conference tiers — RETIRE

The $250, $350, and $1,500 one-time tiers promise consulting and conference
work obligations and should be retired.

## API notes

- GitHub GraphQL API has no `updateSponsorsTier` mutation
- `retireSponsorsTier` and `createSponsorsTier` exist but require `user` scope
- Current CLI token only has `gist`, `read:org`, `repo`, `workflow` scopes
- All tier edits must be done via the dashboard UI
- See `UPDATE_OLD_TIERS.md` for step-by-step instructions
