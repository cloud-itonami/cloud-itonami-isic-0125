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
  file\".")

;; Protocol for swappable store implementations
(defprotocol Store
  (registered-orchard [store orchard-id]
    "Retrieve a registered orchard/grove block record by ID. Returns nil
    if the orchard-id is nil or not registered."))

;; In-memory implementation (MemStore) for development/testing
(defrecord MemStore [orchards]
  Store
  (registered-orchard [_store orchard-id]
    (when orchard-id
      (get @orchards orchard-id))))

(defn mem-store
  "Create an in-memory store. `initial-orchards` is an optional map of
  orchard-id -> orchard-record."
  [& [{:keys [initial-orchards] :or {initial-orchards {}}}]]
  (MemStore. (atom initial-orchards)))

(defn add-orchard
  "Register or update an orchard/grove block in the store. Used by tests
  and simulation."
  [^MemStore store orchard-id orchard-data]
  (swap! (:orchards store) assoc orchard-id orchard-data)
  orchard-data)
