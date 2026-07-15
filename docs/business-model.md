# Business Model: Tree- and Bush-Fruit and Nut Orchard/Grove Operations Coordinator

## Classification

- Repository: `cloud-itonami-isic-0125`
- ISIC Rev. 4: `0125`
- Industry: Growing of other tree and bush fruits and nuts
- Social impact: food-security, rural-employment, agricultural-sustainability

## Customer

- Small-to-medium blueberry, raspberry, blackcurrant, almond, walnut,
  hazelnut, and pecan growers
- Tree/bush-fruit-and-nut orchard/grove management companies
- Cooperative packhouses/processors needing grower-side records
- Contract-farming operations coordinating multiple smallholder groves

## Offer

- Orchard/grove-block record-keeping (planting, harvest yield, quality-grade)
- Field-operation coordination (pruning/spraying/harvest scheduling)
- Crop health and pest/disease tracking (e.g. spotted wing drosophila, walnut blight)
- Supply procurement coordination
- Audit trail and transparency

## Revenue

- SaaS subscription (per-hectare-per-month pricing)
- Supply chain integration fees
- API access for agronomist partners
- Data analytics and reporting add-ons

## Trust Controls

- No direct field-equipment operation without human sign-off
- No finalizing spray-application decisions
- All field-operation recommendations are proposals, not commands
- Orchard/grove-block registration is required before any operation
- All crop health concerns are automatically escalated
- High-cost supply orders require approval
- Audit ledger is append-only and never editable

## What we NOT do

- **Spray-application decisions** — the agronomist/grower decides application
- **Crop health/welfare decisions** — the grower decides response actions
- **Economic decisions** (harvest timing, replanting) — remain human authority
- **Direct field-equipment operation** — the robot manages records and logistics only

## Supported Operations

### Orchard/Grove Record Logging
- Planting counts and block layout
- Harvest weight and yield tracking
- Quality-grade testing
- Health status notes (logging only, not decision-making)

### Field-Operation Coordination
- Schedule pruning, spraying, harvest
- Track completed field-operation results
- Propose follow-up field work (not order it directly)

### Crop Health Concern Escalation
- Flag suspected pest pressure (e.g. spotted wing drosophila)
- Report fungal/bacterial disease (e.g. walnut blight), or frost-damage observations
- Automatic escalation to grower/agronomist

### Supply Procurement
- Seedling orders
- Fertilizer orders
- Equipment procurement
- Cost threshold escalation for large orders
