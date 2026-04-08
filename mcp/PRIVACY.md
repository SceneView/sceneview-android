# Privacy Policy — sceneview-mcp

**Last updated:** April 8, 2026

## Summary

**Free tier:** sceneview-mcp does not collect, store, or process any personal data.

**Pro tier:** Billing data is processed by Stripe and Polar.sh. The MCP server itself only validates your API key — no personal data is stored locally.

## Details

### Data Collection — Free Tier

- **No personal data collected** — We do not collect names, email addresses, IP addresses, or any other personally identifiable information.
- **No cookies** — The Service does not use cookies of any kind.
- **No tracking** — No analytics, telemetry, fingerprinting, or behavioral tracking.
- **No logging** — API requests are stateless and not logged. No request content, metadata, or usage patterns are recorded.

### Data Collection — Pro Tier

When you subscribe to SceneView MCP Pro:
- **Billing data** (email, payment method) is collected and processed by **Stripe** and **Polar.sh** under their respective privacy policies. We do not store this data ourselves.
- **API key validation** — Your API key (a Stripe subscription ID) is sent to the Stripe API to verify your subscription status. The result is cached in-memory for 5 minutes and then discarded. No validation results are persisted to disk.
- **Usage logging** — Tool invocations are logged to stderr for debugging only. These logs are ephemeral (process-lifetime) and contain only a truncated key prefix and the tool name — no personal data.

### Data Processing

- All MCP tool calls are processed in-memory and discarded immediately after the response is returned.
- No data is persisted to disk, database, or cloud storage by the MCP server itself.

### Third-Party Services

| Service | Purpose | Data Sent |
|---|---|---|
| **GitHub public API** | Fetch open issues for `sceneview://known-issues` | None (public API, no auth) |
| **Stripe API** | Validate Pro subscription status | API key (subscription ID) only |

- GitHub API results are cached in-memory for 10 minutes
- Stripe validation results are cached in-memory for 5 minutes
- Both caches are discarded when the process ends

### GDPR Compliance

**Free tier:** Compliant by design — no personal data is collected or processed.

**Pro tier:** Billing data is processed by Stripe (data processor) and Polar.sh (payment platform). Thomas Gorisse acts as data controller for the billing relationship. Your rights under GDPR:
- **Right to access**: Contact us for details on your billing data held by Stripe/Polar.
- **Right to deletion**: Cancel your subscription and contact us to request data erasure.
- **Right to portability**: Available through Stripe/Polar dashboards.
- **Data Protection Officer**: Not required under current processing scale.

### CCPA Compliance

This Service is compliant with the California Consumer Privacy Act (CCPA) by design: no personal information is collected, sold, or shared.

### Children's Privacy

This Service does not knowingly collect any data from anyone, including children under 13.

### Changes to This Policy

We may update this Privacy Policy from time to time. Changes will be reflected in the "Last updated" date above.

### Contact

Thomas Gorisse — [https://github.com/sceneview/sceneview](https://github.com/sceneview/sceneview)
