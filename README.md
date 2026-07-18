# cloud-itonami-isco-7514

Open Occupation Blueprint for **ISCO-08 7514**: Fruit, Vegetable and Related Preservers.

This repository designs a forkable OSS business for a preservation-shop scheduling and logistics coordination practice: a preservation-shop scheduling and supply-coordination robot manages crew/task records under a governor-gated actor, so a fruit-and-vegetable preservation crew keeps its own operating records instead of renting a closed workforce-management SaaS.

**Maturity: `:implemented`.** `src/preservecoord/` implements the
`PreserveCoordActor` as a `langgraph.graph/state-graph`
(`preservecoord.actor`) wired to a `Preservation Shop Coordination
Advisor` (`preservecoord.advisor`) and an independent
`PreserveCoordGovernor` (`preservecoord.governor`), following the
itonami actor pattern (ADR-2607121000): `:intake -> :advise -> :govern
-> :decide -+-> :commit (:ok?) +-> :request-approval (:escalate?,
human-in-the-loop interrupt) +-> :hold (:hard?)`. HARD invariants
(always hold, never overridable): preserver provenance, shop
provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist (`:log-work-record`, `:schedule-crew-operation`,
`:flag-safety-concern`, `:coordinate-supply-order` — nothing else may
ever be proposed), and a permanent, unconditional block on any
proposal that would directly finalize a processing-execution decision
(e.g. deciding to proceed with a specific canning/processing run), a
sterilization-clearance decision (e.g. declaring a batch sterilized
and cleared), or a food-safety-clearance decision (e.g. declaring a
batch fit for sale), or that would override a shop safety officer's
judgment. Always-escalate paths (human sign-off regardless of
confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:flag-safety-concern` (always) and `:coordinate-supply-order` above
the registered cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a preservation-shop scheduling/logistics coordination robot performs crew scheduling, batch/inventory/progress-record logging and produce/jarring-materials supply-order coordination for a fruit-and-vegetable preservation crew, under an actor that proposes actions and an independent **Preservation Shop Coordination Governor** that gates them. The governor never
dispatches hardware itself, never performs canning, pickling or preservation work on the shop floor, and never finalizes a processing-execution decision, a sterilization-clearance decision or a food-safety-clearance decision, and never overrides a shop safety officer's judgment; `:high`/`:safety-critical` actions (such as a flagged contamination-risk/sterilization-failure/equipment-condition concern, or an above-threshold supply order) require human sign-off. **This actor coordinates PRESERVATION-SHOP SCHEDULING/LOGISTICS ONLY — it never performs canning, pickling or preservation work itself, and it never makes a sterilization-clearance or food-safety-clearance decision itself.**

## Core Contract

```text
crew roster + shop registration + safety-reporting policy
        |
        v
Preservation Shop Coordination Advisor -> PreserveCoordGovernor -> log/schedule/coordinate, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, finalize
a processing-execution decision, finalize a sterilization-clearance or
food-safety-clearance decision, override a shop safety officer's judgment,
suppress an operating record, or disclose sensitive data without governor
approval and audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `7514`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
