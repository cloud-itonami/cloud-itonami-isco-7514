(ns preservecoord.store
  "SSoT for the ISCO-08 7514 fruit, vegetable and related preservers
  preservation-shop scheduling/logistics coordination actor (itonami
  actor pattern, ADR-2607121000 / CLAUDE.md Actors section; README's
  'Robotics premise' — a preservation-shop scheduling/logistics
  coordination robot performs crew scheduling, batch/inventory/
  progress-record logging and produce/jarring-materials supply-order
  coordination for a fruit-and-vegetable preservation crew under this
  advisor/governor pair, which never dispatches hardware itself, never
  performs canning, pickling or preservation work itself, and never
  finalizes a processing-execution decision, a sterilization-clearance
  decision or a food-safety-clearance decision, and never overrides a
  shop safety officer's judgment — those remain the shop safety
  officer's exclusive judgment). Modeled closely on
  cloud-itonami-isco-7211's foundrycoord.store.

  Domain:

    preserver — a registered fruit/vegetable preservation crew member
              (:preserver-id, :name)
    shop      — a registered preservation-shop site {:shop-id :name
              :max-supply-cost number}. `:max-supply-cost` is an
              informational registered ceiling used only to decide
              whether a `:coordinate-supply-order` proposal escalates
              to human sign-off (the governor never blocks a
              within-threshold order outright; it only decides
              commit vs. escalate).
    record  — a committed operating record (a logged batch/inventory/
              progress entry, a scheduled crew/processing-schedule
              operation, a flagged safety concern, or a coordinated
              produce/jarring-materials supply order) — written ONLY
              via commit-record!.
    ledger  — append-only audit trail, commit or hold.")

(defprotocol Store
  (preserver [s preserver-id])
  (shop [s shop-id])
  (records-of [s preserver-id])
  (ledger [s])
  (register-preserver! [s preserver])
  (register-shop! [s shop])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (preserver [_ preserver-id] (get-in @a [:preservers preserver-id]))
  (shop [_ shop-id] (get-in @a [:shops shop-id]))
  (records-of [_ preserver-id] (filter #(= preserver-id (:preserver-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-preserver! [s p]
    (swap! a assoc-in [:preservers (:preserver-id p)] p) s)
  (register-shop! [s f]
    (swap! a assoc-in [:shops (:shop-id f)] f) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:preservers {} :shops {} :records [] :ledger []}
                                    seed)))))
