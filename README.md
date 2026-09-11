# cloud-itonami-isic-0125

Open Occupation Blueprint for **ISIC Rev. 4 0125**: Growing of other
tree and bush fruits and nuts.

This repository implements a forkable OSS **tree/bush-fruit-and-nut
orchard/grove operations coordinator**: a facility-management robot
manages orchard/grove-block record logging, field-operation (pruning/
spraying/harvest) scheduling, and supply procurement under a
governor-gated actor, so a tree/bush-fruit-and-nut-growing operation
(blueberry, raspberry, blackcurrant, almond, walnut, hazelnut, pecan
orchards and groves) keeps its own operational records and maintains
full transparency over decisions.

**Maturity: `:implemented`.** `src/berrynutops/` implements the
`BerryNutOpsAdvisor` (`berrynutops.advisor`) and the independent
`BerryNutOperationsGovernor` (`berrynutops.governor`), composed by
`berrynutops.operation` into a **genuinely compiled `langgraph-clj`
`StateGraph`** (`langgraph.graph/state-graph` + `compile-graph`,
`interrupt-before #{:request-approval}` for real checkpointed
human-in-the-loop resume): `intake -> advise -> govern -> decide -+->
commit / request-approval -> commit / hold`. Every committed/held/
approval-rejected decision fact lands in `berrynutops.store`'s
append-only audit ledger (`ledger` / `append-ledger!`), genuinely wired
into the graph's `:commit`/`:hold` terminal nodes. See
[Testing](#testing) below for the current green test count
(`kbb -M:test`).

An earlier version of this repository claimed `:implemented` while
`operation.cljc`'s own docstring admitted the StateGraph integration was
"deferred" (a hand-rolled closure with zero `langgraph.graph` calls), the
real `langgraph` dependency sat unused under the `:dev :override-deps`
alias (`deps.edn`'s main `:deps` was `{}`), and `store.cljc` had no
ledger concept anywhere in `src/`. All three are fixed now — see
`blueprint.edn`'s `:itonami.blueprint/implemented-slice` for the full
diff summary.

## What this does NOT do

This actor coordinates **back-office logistics only**. It explicitly does **NOT**:

- **Direct field-equipment operation** — remains the grower's exclusive authority
- **Spray-application decisions** — remains the agronomist/grower authority
- **Harvest-timing / economic decisions** — economic authority remains human
- **Direct execution of any kind** — any proposal for direct actuation is a hard block

## HARD invariants (always hold, never overridable)

1. **orchard-not-registered** — the request's `orchard-id` must resolve to a
   registered orchard/grove block in the Store before any proposal can proceed
2. **no-execution** — every proposal's `:effect` must be `:propose` (the governor
   never directly operates field equipment, never finalizes a spray application)
3. **field-equipment-or-spray-blocked** — `:operate-field-equipment` and
   `:finalize-spray-application` proposals are unconditionally, permanently blocked
4. **op-not-allowed** — any op outside the closed allowlist below is rejected
5. **orchard-count-invalid** — `:log-orchard-record` with a non-positive logged
   quantity (trees/bushes counted / harvest weight / yield estimate / quality-grade
   reading) is rejected

## Always-escalate operations (human sign-off, regardless of confidence)

- `:flag-crop-health-concern` — any pest (e.g. spotted wing drosophila)/disease
  (e.g. walnut blight)/frost-damage concern → automatic escalation
- `:order-supplies` over its category cost threshold (default 500 currency
  units; see `berrynutops.facts/supply-categories`)
- Any proposal with confidence below the Governor's floor (0.7)

## Operational requests (closed allowlist, all `:effect :propose`)

```text
:log-orchard-record
  — record planting/harvest-yield/quality-grade data
  — requires a registered orchard/grove block; non-positive quantities are rejected

:schedule-field-operation
  — propose pruning/spraying/harvest scheduling
  — does NOT make or finalize a spray-application decision

:flag-crop-health-concern
  — surface a pest (e.g. spotted wing drosophila), disease (e.g. walnut
    blight), or frost-damage concern
  — ALWAYS escalates for human review

:order-supplies
  — procurement for seedlings, fertilizer, equipment
  — escalates if cost exceeds its category threshold
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs the
physical domain work**. Here a facility-management robot handles:

- Orchard/grove-block record logging and entry
- Field-operation scheduling and reminders
- Supply inventory and ordering
- Audit ledger maintenance

The **BerryNutOperationsGovernor** is the independent safety layer that gates all
proposals before a robot action is executed. The governor never dispatches hardware
directly; `:high`/`:safety-critical` actions (such as escalated crop-health concerns
or high-cost supply orders) require human sign-off.

## Core Contract

```text
operational request (log, schedule, concern, order)
        |
        v
BerryNutOpsAdvisor -> BerryNutOperationsGovernor -> phase gate -> commit, or escalate for human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated operation can dispatch a robot action the governor refuses, suppress an
operating record, or hide a crop-health concern without governor approval and audit
evidence.

## Module structure

Mirrors `cloud-itonami-isic-0124` (`pomestoneops.*`) module-for-module:

- `berrynutops.facts` — reference data: supply-category cost thresholds, fruit classes
- `berrynutops.registry` — pure independent verification functions (cost/count/confidence)
- `berrynutops.store` — `Store` protocol + in-memory `MemStore` (orchard/grove-block registration lookup, append-only audit ledger)
- `berrynutops.advisor` — `Advisor` protocol + `MockAdvisor` (the sealed LLM/decision node)
- `berrynutops.governor` — `BerryNutOperationsGovernor`: hard invariants + escalation gates
- `berrynutops.phase` — 0→3 rollout phase gate
- `berrynutops.operation` — compiles advisor → governor → phase into a real `langgraph-clj` `StateGraph` (`build`), with checkpointed `interrupt-before` human-in-the-loop resume
- `berrynutops.sim` — demo runner (`kbb -M:run`), drives the compiled graph end-to-end via `langgraph.graph/run*`

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISIC Rev. 4 `0125`). Required capabilities:

- :robotics
- :identity
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Testing

```bash
kbb -M:test   # run the suite (see raw output for tests/assertions)
kbb -M:lint   # clj-kondo, 0 errors / 0 warnings
kbb -M:run    # demo runner
```

## License

AGPL-3.0-or-later.
