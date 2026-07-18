(ns preservecoord.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [preservecoord.store :as store]
            [preservecoord.advisor :as advisor]
            [preservecoord.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-preserver! st {:preserver-id "preserver-1" :name "Aki Sato"})
    (store/register-shop! st {:shop-id "S-1" :name "Kobo Preservation Shop" :max-supply-cost 2000})
    st))

(defn- op [op-kw & {:as extra}]
  (merge {:op op-kw :effect :propose :shop-id "S-1"
          :confidence 0.9 :stake :low}
         extra))

(def ^:private req {:preserver-id "preserver-1"})

(deftest ok-log-work-record
  (let [st (fresh-store)
        v (governor/check req {} (op :log-work-record) st)]
    (is (:ok? v))))

(deftest ok-schedule-crew-operation
  (let [st (fresh-store)
        v (governor/check req {} (op :schedule-crew-operation) st)]
    (is (:ok? v))))

(deftest ok-supply-order-at-threshold-boundary
  (testing "the supply-cost threshold escalate boundary is exclusive (over, not at)"
    (let [st (fresh-store)
          v (governor/check req {} (op :coordinate-supply-order :cost 2000) st)]
      (is (:ok? v)))))

(deftest hard-on-unregistered-preserver
  (let [st (fresh-store)
        v (governor/check {:preserver-id "nobody"} {} (op :log-work-record) st)]
    (is (:hard? v))
    (is (some #(= :no-preserver (:rule %)) (:violations v)))))

(deftest hard-on-unregistered-shop
  (let [st (fresh-store)
        v (governor/check req {} (op :log-work-record :shop-id "S-ghost") st)]
    (is (:hard? v))
    (is (some #(= :no-shop (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-work-record) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest hard-on-op-outside-closed-allowlist
  (let [st (fresh-store)
        v (governor/check req {} (op :dispatch-equipment) st)]
    (is (:hard? v))
    (is (some #(= :unknown-op (:rule %)) (:violations v)))))

(deftest hard-on-scope-excluded-op-finalize-processing-decision
  (testing "finalizing a processing-execution decision is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-processing-decision) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-authorize-processing-run
  (testing "authorizing a processing run is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :authorize-processing-run) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-proceed-with-processing-run
  (testing "proceeding with a processing run is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :proceed-with-processing-run) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-finalize-sterilization-clearance
  (testing "finalizing a sterilization-clearance decision is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-sterilization-clearance) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-declare-batch-sterilized-and-cleared
  (testing "declaring a batch sterilized and cleared is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :declare-batch-sterilized-and-cleared) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-finalize-food-safety-clearance
  (testing "finalizing a food-safety-clearance decision is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :finalize-food-safety-clearance) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-declare-batch-fit-for-sale
  (testing "declaring a batch fit for sale is a permanent block, never a routine op"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :declare-batch-fit-for-sale) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-op-override-shop-safety-officer-judgment
  (testing "overriding a shop safety officer's judgment is a shop safety officer's exclusive judgment, never this actor's"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :override-shop-safety-officer-judgment) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-processing-run
  (testing "defense-in-depth: a rationale that itself attempts to proceed with the processing run is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :schedule-crew-operation)
                                           :rationale "recommend we proceed with the processing run now")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-sterilization-clearance
  (testing "defense-in-depth: a rationale that itself attempts to declare the batch sterilized and cleared is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-work-record)
                                           :rationale "we can declare the batch sterilized and cleared now")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-food-safety-clearance
  (testing "defense-in-depth: a rationale that itself attempts to finalize the food-safety clearance is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :coordinate-supply-order :cost 100)
                                           :rationale "recommend we finalize the food-safety clearance today")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest hard-on-scope-excluded-rationale-override-shop-safety-officer
  (testing "defense-in-depth: a rationale attempting to override the shop safety officer's judgment is blocked even if the op looks routine"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :log-work-record)
                                           :rationale "override the shop safety officer's judgment and proceed")
                             st)]
      (is (:hard? v))
      (is (some #(= :scope-excluded-action (:rule %)) (:violations v))))))

(deftest always-escalates-safety-concern-even-at-high-confidence
  (testing "a contamination-risk/sterilization-failure/equipment-condition concern always requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (op :flag-safety-concern :hazard-type :contamination-risk)
                                           :confidence 0.99)
                             st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-supply-order-above-threshold
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :coordinate-supply-order :cost 5000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (op :log-work-record) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest default-mock-advisor-proposals-never-self-trip-on-scope-exclusion
  (testing "the governor's scope-exclusion term list must never match the mock advisor's own default rationale text for any allowlisted op — CLAUDE.md's known self-tripping bug pattern (rationale legitimately contains bare nouns like 'processing'/'shop'/'safety officer', but never the full finalization-action phrases)"
    (let [st (fresh-store)
          adv (advisor/mock-advisor)
          ops [:log-work-record :schedule-crew-operation
               :flag-safety-concern :coordinate-supply-order]]
      (doseq [o ops]
        (let [request {:preserver-id "preserver-1" :op o :shop-id "S-1"
                        :stake :low :task "routine processing task"
                        :hazard-type :contamination-risk :cost 500}
              proposal (advisor/-advise adv st request)
              v (governor/check request {} proposal st)]
          (is (not (:hard? v))
              (str o " proposal unexpectedly hard-blocked: " (:violations v))))))))
