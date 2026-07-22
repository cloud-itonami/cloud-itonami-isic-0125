(ns berrynutops.store-test
  (:require [clojure.test :refer [deftest is testing]]
            [berrynutops.store :as store]))

(deftest mem-store-creation
  (testing "Create empty store"
    (let [st (store/mem-store)]
      (is (some? st))
      (is (satisfies? store/Store st))))

  (testing "Create store with initial orchards"
    (let [orchards {"orchard-001" {:id "orchard-001" :name "Test Grove Block"}}
          st (store/mem-store {:initial-orchards orchards})]
      (is (some? st))
      (is (satisfies? store/Store st)))))

(deftest registered-orchard-retrieval
  (testing "Retrieve existing orchard"
    (let [orchard {:id "orchard-001" :name "Test Grove Block"}
          st (store/mem-store {:initial-orchards {"orchard-001" orchard}})]
      (is (= orchard (store/registered-orchard st "orchard-001")))))

  (testing "Retrieve non-existent orchard"
    (let [st (store/mem-store)]
      (is (nil? (store/registered-orchard st "no-such-orchard")))))

  (testing "nil orchard-id returns nil (never falls through to a default)"
    (let [st (store/mem-store {:initial-orchards {"orchard-001" {:id "orchard-001"}}})]
      (is (nil? (store/registered-orchard st nil))))))

(deftest add-orchard-test
  (testing "Register a new orchard"
    (let [st (store/mem-store)
          orchard-data {:id "orchard-002" :name "New Grove Block"}
          result (store/add-orchard st "orchard-002" orchard-data)]
      (is (= orchard-data result))
      (is (= orchard-data (store/registered-orchard st "orchard-002")))))

  (testing "Update an existing orchard"
    (let [st (store/mem-store {:initial-orchards {"orchard-001" {:id "orchard-001"}}})
          updated {:id "orchard-001" :name "Renamed Grove Block"}
          result (store/add-orchard st "orchard-001" updated)]
      (is (= updated result))
      (is (= updated (store/registered-orchard st "orchard-001"))))))

;; ----------------------------- audit ledger (append-only) -----------------------------
;; FIX: `ledger`/`append-ledger!` did not exist anywhere in this codebase
;; before this fix -- not dead code, the concept was entirely absent.
;; `berrynutops.operation`'s real compiled StateGraph now wires these
;; genuinely into its `:commit`/`:hold` terminal nodes (see
;; `berrynutops.operation-test`); these are unit-level tests of the Store
;; accessor itself.

(deftest ledger-starts-empty-test
  (testing "a freshly created store's ledger is empty"
    (let [st (store/mem-store)]
      (is (empty? (store/ledger st))))))

(deftest append-ledger-test
  (testing "appending a fact grows the ledger, in append order, and
            returns the appended fact"
    (let [st (store/mem-store)
          fact-1 {:t :committed :op :log-orchard-record :subject "orchard-001"}
          fact-2 {:t :governor-hold :op :order-supplies :subject "orchard-002"}
          appended (store/append-ledger! st fact-1)]
      (is (= fact-1 appended))
      (is (= [fact-1] (store/ledger st)))
      (store/append-ledger! st fact-2)
      (is (= [fact-1 fact-2] (store/ledger st)))))

  (testing "the ledger is independent per store instance"
    (let [st-a (store/mem-store)
          st-b (store/mem-store)]
      (store/append-ledger! st-a {:t :committed :op :order-supplies})
      (is (= 1 (count (store/ledger st-a))))
      (is (empty? (store/ledger st-b))))))
