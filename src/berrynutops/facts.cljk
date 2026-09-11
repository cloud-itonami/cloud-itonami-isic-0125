(ns berrynutops.facts
  "Reference facts for tree- and bush-fruit and nut orchard/grove
  operations coordination: supply category cost policy and other-tree/
  bush-fruit/nut species classification. This namespace contains pure
  lookup functions for domain reference data -- the Governor and Advisor
  consult these instead of inventing thresholds. Mirrors `pomestoneops.facts`
  (cloud-itonami-isic-0124) / `citrusops.facts` (cloud-itonami-isic-0123)
  in shape.")

(def supply-categories
  "Procurement categories this actor may propose orders for, and the
  default cost threshold above which an order proposal must escalate for
  human sign-off (grower/orchard-manager)."
  {"seedling"
   {:id "seedling" :name "苗木" :cost-threshold 500}

   "fertilizer"
   {:id "fertilizer" :name "肥料" :cost-threshold 500}

   "equipment"
   {:id "equipment" :name "設備" :cost-threshold 1000}})

(defn supply-category-by-id [id]
  (get supply-categories id))

(def default-cost-threshold
  "Fallback escalation threshold used when a supply-order proposal doesn't
  cite a known category (never invent a lower bar than this)."
  500)

(def fruit-classes
  "End-use classes this actor's orchard/block records may cover (ISIC
  0125: growing of other tree and bush fruits and nuts). Bush fruits
  (berries): blueberry, raspberry, blackcurrant. Tree nuts: almond,
  walnut, hazelnut, pecan."
  {"blueberry"    {:id "blueberry" :name "ブルーベリー" :group :bush}
   "raspberry"    {:id "raspberry" :name "ラズベリー" :group :bush}
   "blackcurrant" {:id "blackcurrant" :name "カシス" :group :bush}
   "almond"       {:id "almond" :name "アーモンド" :group :nut}
   "walnut"       {:id "walnut" :name "クルミ" :group :nut}
   "hazelnut"     {:id "hazelnut" :name "ヘーゼルナッツ" :group :nut}
   "pecan"        {:id "pecan" :name "ピーカン" :group :nut}})

(defn fruit-class-by-id [id]
  (get fruit-classes id))
