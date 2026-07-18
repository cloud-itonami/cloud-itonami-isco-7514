# Security Policy

This project handles fruit, vegetable and related preservers operating
workflows. Treat vulnerabilities as potentially high impact even when the
demo data is synthetic — this domain's failure modes include real food-safety
risk from contamination and improper sterilization/canning protocol failures
(including botulism risk), alongside physical worker-safety risk.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real preserver, shop or operator data exposure
- authorization bypass
- Preservation Shop Coordination Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- any path that lets a proposal reach a processing-execution decision,
  a sterilization-clearance decision, a food-safety-clearance
  decision, or a shop-safety-officer-override decision

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on preserver/shop data, policy enforcement or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real preserver/shop/operator data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
