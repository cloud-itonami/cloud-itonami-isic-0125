(ns berrynutops.store
  "Store abstraction for tree- and bush-fruit and nut orchard/grove block/
  planting records. Current implementation is an in-memory map;
  production should migrate to Datomic/kotoba-server (the same seam point
  all cloud-itonami actors use). Mirrors `pomestoneops.store`
  (cloud-itonami-isic-0124) / `citrusops.store` (cloud-itonami-isic-0123)
  in shape.

  A registered orchard/grove block is the minimal unit of authority: an
  orchard/block must be registered before ANY proposal referencing it can
  be considered by the Governor (see `berrynutops.governor`'s
  `orchard-registered` invariant). Orchard data is opaque to this
  namespace -- callers/backends decide what an orchard record contains
  (name, location, bush-fruit/nut species, planted area, etc); this
  Store only answers \"is this orchard-id registered, and if so what's on
  file\".

  FIX (this commit): the append-only audit ledger (`ledger`/
  `append-ledger!`) is this actor's core missing plumbing before this
  fix -- no such function existed anywhere in the codebase at all, worse
  than the usual dead-code-ledger pattern in some sibling actors: here
  even the concept was entirely absent from `src/`, despite the README's
  \"robot actions (gated) + operating records + audit ledger\" line and
  `blueprint.edn`'s `:required-technologies [... :audit-ledger]` implying
  one should exist. `berrynutops.operation`'s `:commit`/`:hold` graph
  nodes now append every committed/held/approval-rejected decision fact
  here, so an orchard/grove block's operating history (every
  `:log-orchard-record`/`:schedule-field-operation`/
  `:flag-crop-health-concern`/`:order-supplies` decision) is always a
  query over an immutable log -- the same discipline every sibling
  `cloud-itonami-isic-*` actor's ledger provides. The ledger stays
  append-only; all pre-existing accessors below (`registered-orchard`,
  `mem-store`, `add-orchard`) are UNCHANGED.")

;; Protocol for swappable store implementations
(defprotocol Store
  (registered-orchard [store orchard-id]
    "Retrieve a registered orchard/grove block record by ID. Returns nil
    if the orchard-id is nil or not registered.")
  (ledger [store]
    "The append-only audit ledger: every committed/held/approval-rejected
    decision fact, in append order.")
  (append-ledger! [store fact]
    "Append one immutable decision fact to the ledger. Returns the fact."))

;; In-memory implementation (MemStore) for development/testing
(defrecord MemStore [orchards audit-ledger]
  Store
  (registered-orchard [_store orchard-id]
    (when orchard-id
      (get @orchards orchard-id)))

  (ledger [_store]
    @audit-ledger)

  (append-ledger! [_store fact]
    (swap! audit-ledger conj fact)
    fact))

(defn mem-store
  "Create an in-memory store. `initial-orchards` is an optional map of
  orchard-id -> orchard-record."
  [& [{:keys [initial-orchards] :or {initial-orchards {}}}]]
  (MemStore. (atom initial-orchards) (atom [])))

(defn add-orchard
  "Register or update an orchard/grove block in the store. Used by tests
  and simulation."
  [^MemStore store orchard-id orchard-data]
  (swap! (:orchards store) assoc orchard-id orchard-data)
  orchard-data)
