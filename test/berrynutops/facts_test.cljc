(ns berrynutops.facts-test
  (:require [clojure.test :refer [deftest is are testing]]
            [berrynutops.facts :as facts]))

(deftest supply-category-lookup
  (testing "Lookup valid supply category"
    (let [c (facts/supply-category-by-id "seedling")]
      (is (= "seedling" (:id c)))
      (is (= "苗木" (:name c)))))

  (testing "Lookup invalid supply category"
    (is (nil? (facts/supply-category-by-id "unknown")))))

(deftest supply-category-cost-thresholds
  (testing "Category-specific cost thresholds"
    (are [id expected] (= expected (:cost-threshold (facts/supply-category-by-id id)))
      "seedling"    500
      "fertilizer"  500
      "equipment"   1000)))

(deftest default-cost-threshold-value
  (testing "Default fallback threshold matches the conservative baseline"
    (is (= 500 facts/default-cost-threshold))))

(deftest fruit-class-lookup
  (testing "Lookup valid fruit class"
    (are [id expected-name] (= expected-name (:name (facts/fruit-class-by-id id)))
      "blueberry"    "ブルーベリー"
      "raspberry"    "ラズベリー"
      "blackcurrant" "カシス"
      "almond"       "アーモンド"
      "walnut"       "クルミ"
      "hazelnut"     "ヘーゼルナッツ"
      "pecan"        "ピーカン"))

  (testing "Bush fruits are grouped :bush, tree nuts :nut"
    (are [id expected-group] (= expected-group (:group (facts/fruit-class-by-id id)))
      "blueberry"    :bush
      "raspberry"    :bush
      "blackcurrant" :bush
      "almond"       :nut
      "walnut"       :nut
      "hazelnut"     :nut
      "pecan"        :nut))

  (testing "Lookup invalid fruit class"
    (is (nil? (facts/fruit-class-by-id "unknown")))))
