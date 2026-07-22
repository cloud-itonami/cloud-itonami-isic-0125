(ns berrynutops.operation-test
  "Integration tests for `berrynutops.operation/build` -- builds the REAL
  compiled `langgraph.graph` StateGraph and runs it end-to-end via
  `langgraph.graph/run*` through commit / hard-hold / phase-0-sandbox-
  escalate / escalate-approve / escalate-reject / high-stakes-cost
  routes.

  This replaces the previous version of this namespace (which did not
  exist at all -- `operation.cljc`'s OWN docstring admitted \"langgraph-clj
  StateGraph integration is deferred\" / \"Stub for building a
  langgraph-clj StateGraph\", and `build` returned a hand-rolled closure
  invoked AS A PLAIN FUNCTION (`(graph request context)`) with ZERO
  `langgraph.graph` usage anywhere, despite `blueprint.edn` claiming
  `:itonami.blueprint/maturity :implemented`). These tests are
  FALSIFIABLE on real StateGraph behavior, not hardcoded pass strings:
  the ledger stays empty until a real commit, escalated proposals
  hold-until-approved via a genuine checkpointed `interrupt-before`, and
  a governor rejection blocks commit entirely. Mirrors
  `distilling.operation-test` (cloud-itonami-isic-1101) /
  `knitwear.operation-test` (cloud-itonami-isic-1430)."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [berrynutops.operation :as op]
            [berrynutops.store :as store]))

(defn- exec [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- registered-orchard
  "A registered orchard/grove block fixture. Mirrors `berrynutops.sim`'s
  demo fixture."
  []
  {:id "orchard-001"
   :name "Test Grove Block"
   :fruit-class "blueberry"})

(deftest ledger-starts-empty
  (testing "a freshly created store's audit ledger is empty until a real
            commit lands -- no proposal, no verdict, no graph run has
            happened yet"
    (let [s (store/mem-store)]
      (is (empty? (store/ledger s))))))

(deftest commit-path-schedule-field-operation-auto-commits
  (testing ":schedule-field-operation is a routine op NOT in
            `governor/always-escalate-ops` -- a clean, registered-orchard
            request for it genuinely commits through the real compiled
            graph (no interrupt) at phase-1, and appends exactly one fact
            to the audit ledger"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          result (exec actor "t-commit"
                       {:op :schedule-field-operation :orchard-id "orchard-001"
                        :requested-date "2026-08-01"}
                       {:actor-id "test-001" :phase :phase-1})
          state (:state result)]
      (is (= :done (:status result)))
      (is (= :commit (:decision state)))
      (is (map? (:record state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :committed (:t (first ledger))))
        (is (= :schedule-field-operation (:op (first ledger))))
        (is (= "orchard-001" (:subject (first ledger))))))))

(deftest hard-hold-path-unregistered-orchard
  (testing "a proposal referencing an unregistered (or absent) orchard-id
            is a HARD, permanent governor violation -- the real graph
            routes straight to :hold (no interrupt, no human-approval
            detour) EVEN AT PHASE-3 (full autonomy), and durably records
            the hold fact -- the ledger never gets a :committed fact"
    (let [s (store/mem-store)
          actor (op/build s)
          result (exec actor "t-hold-unreg"
                       {:op :log-orchard-record :orchard-id "orchard-ghost" :count 500}
                       {:actor-id "test-001" :phase :phase-3})
          state (:state result)]
      (is (= :done (:status result)))
      (is (= :hold (:decision state)))
      (is (false? (:record state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :governor-hold (:t (first ledger))))
        (is (some #(= :orchard-not-registered (:rule %)) (:violations (first ledger))))
        (is (not-any? #(= :committed (:t %)) ledger)
            "governor rejection blocks commit -- no :committed fact ever lands")))))

(deftest hard-hold-path-orchard-count-invalid
  (testing "a non-positive logged orchard-record quantity is also a HARD
            violation, re-derived from the proposal by the Governor --
            never trusted from advisor confidence alone"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          result (exec actor "t-hold-count"
                       {:op :log-orchard-record :orchard-id "orchard-001" :count 0}
                       {:actor-id "test-001" :phase :phase-3})]
      (is (= :hold (:decision (:state result))))
      (is (some #(= :orchard-count-invalid (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest hard-hold-path-field-equipment-blocked
  (testing "direct field-equipment operation is a HARD, permanent block
            -- never escalate, never override, even at full autonomy"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          result (exec actor "t-hold-equipment"
                       {:op :operate-field-equipment :orchard-id "orchard-001"}
                       {:actor-id "test-001" :phase :phase-3})]
      (is (= :done (:status result)))
      (is (= :hold (:decision (:state result))))
      (is (some #(= :field-equipment-or-spray-blocked (:rule %))
                (:violations (first (store/ledger s))))))))

(deftest phase-0-sandbox-escalates-a-clean-proposal
  (testing "phase-0 (simulation) forces EVEN a Governor-clean, non-
            always-escalate proposal to escalate for human review -- the
            phase gate overrides the Governor's own :commit disposition,
            distinguished in the audit trail by :reason
            :phase-0-simulation-only. The real graph GENUINELY interrupts
            (checkpointed) at :request-approval; the ledger stays EMPTY
            until a human resumes the SAME compiled graph"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          held (exec actor "t-phase0"
                     {:op :schedule-field-operation :orchard-id "orchard-001"
                      :requested-date "2026-08-01"}
                     {:actor-id "test-001" :phase :phase-0})]
      (is (= :interrupted (:status held)))
      (is (= [:request-approval] (:frontier held)))
      (is (empty? (store/ledger s))
          "hold-until-approved: not yet committed -- ledger stays empty
          until a human signs off")
      (let [approved (g/run* actor {:approval {:status :approved :by "orchard-manager-01"}}
                             {:thread-id "t-phase0" :resume? true})
            approved-state (:state approved)]
        (is (= :done (:status approved)))
        (is (= :commit (:decision approved-state)))
        (let [ledger (store/ledger s)]
          (is (= 1 (count ledger)))
          (is (= :committed (:t (first ledger)))))))))

(deftest escalate-then-approve-commits-crop-health-concern
  (testing ":flag-crop-health-concern is in `governor/always-escalate-ops`
            -- ALWAYS escalates even on a fully clean, phase-2 request.
            The real graph GENUINELY interrupts (checkpointed) at
            :request-approval; the ledger stays EMPTY until a human
            grower/agronomist approve! resumes the SAME compiled graph
            and commits via the graph's own :request-approval -> :commit
            edge"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          held (exec actor "t-escalate"
                     {:op :flag-crop-health-concern :orchard-id "orchard-001"
                      :concern "spotted wing drosophila suspected"}
                     {:actor-id "test-001" :phase :phase-2})]
      (is (= :interrupted (:status held)))
      (is (= [:request-approval] (:frontier held)))
      (is (empty? (store/ledger s))
          "hold-until-approved: not yet committed -- ledger stays empty
          until a human signs off")
      (let [approved (g/run* actor {:approval {:status :approved :by "agronomist-01"}}
                             {:thread-id "t-escalate" :resume? true})
            approved-state (:state approved)]
        (is (= :done (:status approved)))
        (is (= :commit (:decision approved-state)))
        (let [ledger (store/ledger s)]
          (is (= 1 (count ledger)))
          (is (= :committed (:t (first ledger))))
          (is (= :flag-crop-health-concern (:op (first ledger))))
          (is (= "agronomist-01" (:approved-by (first ledger)))))))))

(deftest escalate-then-reject-holds
  (testing "a human agronomist rejecting an escalated crop-health-concern
            routes to :hold via the :request-approval node's own
            decision -- governor rejection blocks commit"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          _held (exec actor "t-reject"
                      {:op :flag-crop-health-concern :orchard-id "orchard-001"
                       :concern "walnut blight suspected"}
                      {:actor-id "test-001" :phase :phase-2})
          rejected (g/run* actor {:approval {:status :rejected :by "agronomist-01"}}
                           {:thread-id "t-reject" :resume? true})
          rejected-state (:state rejected)]
      (is (= :done (:status rejected)))
      (is (= :hold (:decision rejected-state)))
      (let [ledger (store/ledger s)]
        (is (= 1 (count ledger)))
        (is (= :approval-rejected (:t (first ledger))))
        (is (not-any? #(= :committed (:t %)) ledger)
            "a rejected approval never reaches :commit")))))

(deftest high-cost-supply-order-escalates-then-commits
  (testing "a supply order above its category cost threshold escalates
            even at phase-3 (full autonomy); approval resumes the SAME
            compiled graph and commits"
    (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
          actor (op/build s)
          held (exec actor "t-cost"
                     {:op :order-supplies :orchard-id "orchard-001"
                      :category "equipment" :cost 1200}
                     {:actor-id "test-001" :phase :phase-3})]
      (is (= :interrupted (:status held)))
      (is (empty? (store/ledger s)))
      (let [approved (g/run* actor {:approval {:status :approved :by "orchard-manager-01"}}
                             {:thread-id "t-cost" :resume? true})]
        (is (= :commit (:decision (:state approved))))
        (let [ledger (store/ledger s)]
          (is (= 1 (count ledger)))
          (is (= :order-supplies (:op (first ledger)))))))))

(deftest never-auto-commit-crop-health-concern-at-every-phase
  (testing "at every phase (including phase-3, full autonomy for what
            CAN auto-commit), :flag-crop-health-concern never
            auto-commits -- it always interrupts, proven against the
            real graph"
    (doseq [phase-kw [:phase-0 :phase-1 :phase-2 :phase-3]]
      (let [s (store/mem-store {:initial-orchards {"orchard-001" (registered-orchard)}})
            actor (op/build s)
            held (exec actor (str "t-safety-" (name phase-kw))
                       {:op :flag-crop-health-concern :orchard-id "orchard-001"
                        :concern "frost damage suspected"}
                       {:actor-id "test-001" :phase phase-kw})]
        (is (= :interrupted (:status held))
            (str phase-kw " should still interrupt for a crop-health concern"))))))

(deftest audit-trail-includes-advisor-proposal
  (testing "the audit trail always starts with the advisor's own
            proposal trace, whatever the eventual disposition"
    (let [s (store/mem-store)
          actor (op/build s)
          result (exec actor "t-audit"
                       {:op :log-orchard-record :orchard-id "orchard-ghost" :count 500}
                       {:actor-id "test-001" :phase :phase-3})
          audit (:audit (:state result))]
      (is (= 2 (count audit)))
      (is (= :advisor-proposal (:t (first audit))))
      (is (= :governor-hold (:t (second audit)))))))
