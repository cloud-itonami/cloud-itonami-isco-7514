(ns preservecoord.governor
  "PreserveCoordGovernor — the independent safety/scope layer gating
  every preservation-shop scheduling/logistics proposal an advisor may
  make for a fruit-and-vegetable preservation crew. The governor never
  dispatches hardware itself, never performs canning, pickling or
  preservation work itself, and never finalizes a processing-execution
  decision (e.g. deciding to proceed with a specific canning/
  processing run), a sterilization-clearance decision (e.g. declaring
  a batch sterilized and cleared), or a food-safety-clearance decision
  (e.g. declaring a batch fit for sale), and never overrides a shop
  safety officer's judgment — those are permanently out of this
  actor's scope and remain a shop safety officer's exclusive judgment
  (README's 'Robotics premise': this actor coordinates PRESERVATION-
  SHOP SCHEDULING/LOGISTICS ONLY — it never performs canning, pickling
  or preservation work itself). Modeled closely on
  cloud-itonami-isco-7211's foundrycoord.governor.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. preserver provenance  — the crew member must be independently
                                verified/registered before any action.
    2. shop provenance        — the preservation-shop site must be
                                independently verified/registered
                                before any action.
    3. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never performs preservation work
                                itself; it only gates what the advisor
                                may coordinate).
    4. closed op-allowlist    — only :log-work-record,
                                :schedule-crew-operation,
                                :flag-safety-concern and
                                :coordinate-supply-order may ever be
                                proposed; anything else is refused.
    5. scope-excluded action  — any proposal to directly finalize a
                                processing-execution decision (e.g.
                                deciding to proceed with a specific
                                canning/processing run), a
                                sterilization-clearance decision (e.g.
                                declaring a batch sterilized and
                                cleared), or a food-safety-clearance
                                decision (e.g. declaring a batch fit
                                for sale), or to override a shop safety
                                officer's judgment, is a hard,
                                permanent block (checked both against
                                the proposed :op and, defense-in-depth,
                                against the proposal's :rationale text
                                — matched as full finalization/
                                execution ACTION phrases such as
                                \"proceed with the processing run\" /
                                \"declare the batch sterilized and
                                cleared\" / \"finalize the food-safety
                                clearance\" / \"override the shop
                                safety officer's judgment\", never as
                                bare nouns like \"canning\",
                                \"sterilization\", \"safety\" or
                                \"processing\", so the check can never
                                self-trip on the advisor's own routine
                                rationale text, e.g. \"logged work
                                record for preserver …\" or \"scheduled
                                crew operation for processing task …\"
                                or \"…routed for shop safety officer
                                review\" — all three legitimately
                                contain those bare nouns but none is a
                                finalization action, and all are
                                exercised by
                                `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off
  regardless of confidence):
    6. :op :flag-safety-concern (a contamination-risk /
                                sterilization-failure / equipment-
                                condition concern always escalates to
                                a human, never auto-commits).
    7. :op :coordinate-supply-order above `supply-cost-threshold`.
    8. low confidence (< `confidence-floor`).

  This actor coordinates preservation-shop scheduling/logistics ONLY
  — it never performs canning, pickling or preservation work itself,
  and it never makes a sterilization-clearance or food-safety-
  clearance decision itself; those decisions always route to a human
  shop safety officer, either via a hard permanent block on the
  op-allowlist (rules 4/5 above) or via a mandatory escalation
  (rule 6 above)."
  (:require [kotoba.lang.text :as str]
            [preservecoord.store :as store]))

(def confidence-floor 0.6)
(def supply-cost-threshold 2000)

(def allowed-ops
  #{:log-work-record :schedule-crew-operation
    :flag-safety-concern :coordinate-supply-order})

;; Defense-in-depth: none of these ops are ever in `allowed-ops`
;; above, so they are already refused by the closed-allowlist check
;; below; they are named again here — as explicit finalization/
;; execution ACTIONS, never bare nouns — so a future allowlist edit
;; cannot silently re-open this specific out-of-scope path without
;; also touching this list.
(def ^:private scope-excluded-ops
  #{:finalize-processing-decision :authorize-processing-run
    :proceed-with-processing-run
    :finalize-sterilization-clearance
    :declare-batch-sterilized-and-cleared
    :finalize-food-safety-clearance
    :declare-batch-fit-for-sale
    :override-shop-safety-officer-judgment
    :override-safety-officer-judgment})

;; Full finalization/execution ACTION phrases only — never bare nouns
;; ("canning", "sterilization", "safety", "processing", "shop",
;; "officer") — so this can never match inside the mock advisor's own
;; default rationale text (which legitimately contains those bare
;; nouns, e.g. "processing task" / "shop safety officer review"). See
;; `governor-test/default-mock-advisor-proposals-never-self-trip-on-scope-exclusion`.
(def ^:private scope-excluded-phrases
  ["proceed with the processing run" "proceed with the canning run"
   "authorize the processing run" "authorize the canning run"
   "finalize the processing decision"
   "declare the batch sterilized and cleared" "declare the batch sterilized"
   "finalize the sterilization clearance"
   "declare the batch fit for sale" "clear the batch for sale"
   "finalize the food safety clearance" "finalize the food-safety clearance"
   "override the shop safety officer's judgment"
   "override the safety officer's judgment"
   "override shop safety officer judgment"])

(defn- contains-excluded-phrase? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) scope-excluded-phrases))))

(defn- hard-violations [proposal preserver-record shop-record]
  (let [{:keys [op rationale]} proposal]
    (cond-> []
      (nil? preserver-record)
      (conj {:rule :no-preserver
             :detail "未登録 preserver への提案は不可（preserver record は独立して検証・登録済みでなければならない）"})

      (nil? shop-record)
      (conj {:rule :no-shop
             :detail "未登録 shop への提案は不可（shop record は独立して検証・登録済みでなければならない）"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は保存加工作業を直接実行しない）"})

      (not (contains? allowed-ops op))
      (conj {:rule :unknown-op
             :detail (str op " は closed op-allowlist に無い — 提案不可")})

      (or (contains? scope-excluded-ops op) (contains-excluded-phrase? rationale))
      (conj {:rule :scope-excluded-action
             :detail "加工実行判断・滅菌(sterilization)クリアランス判断・食品安全(food-safety)クリアランス判断の確定、および shop safety officer の判断の上書きは、この actor の権限外 — 常に永続ブロック"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `preservecoord.store/Store`. Pure — never
  mutates the store, never dispatches a preservation-shop operation."
  [request _context proposal store]
  (let [preserver-record (store/preserver store (:preserver-id request))
        shop-record (some->> (:shop-id proposal) (store/shop store))
        hard (hard-violations proposal preserver-record shop-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        supply-order-over-threshold?
        (and (= :coordinate-supply-order (:op proposal))
             (number? (:cost proposal))
             (> (:cost proposal) supply-cost-threshold))
        always-risky? (or (= :flag-safety-concern (:op proposal))
                           supply-order-over-threshold?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
