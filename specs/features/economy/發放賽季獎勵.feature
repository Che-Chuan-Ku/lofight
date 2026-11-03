@batch
Feature: 發放賽季獎勵
  CHANGE 2026-11-03（D38）：賽季（建置版號前兩碼「大.中」）切換後，依上一賽季排行榜最終名次發金幣給已綁定第三方身分的玩家：第 1～5 名 1,200／800／400／200／100（2026-11-03 使用者改裁決，原第 1～6 名 3,200／1,600／800／400／200／100）；guest 不發；帳本來源「賽季獎勵」；不重複發放。
  # 澄清 AMB-S2＝d（2026-11-03）：後端啟動時自動結算所有未發放的舊賽季，全域冪等；不靠玩家請求觸發 → 觸發鏈起點為系統啟動事件，標 @batch、不產 endpoint、無營運端點
  # 澄清 AMB-S6＝b（2026-11-03）：目前賽季＝後端建置版號（backend/pom.xml version）的「大.中」，不再手填設定檔；小版本變更不切賽季
  # 推論 INF-S14：每個（賽季, 榜別）一個交易（發放記錄＋帳本＋錢包＋事件）；單筆失敗只回滾該（賽季, 榜別）並記錯誤日誌、不阻擋啟動，下次啟動重試
  # 推論 INF-S15：賽季集合＝排行榜成績中出現過的版本前兩碼；待結算賽季兩榜都寫發放記錄（無人受獎時 recipientCount=0）
  # S9 改裁決 2026-11-03：19.0 起發（最早發放賽季 19.1 → 19.0）；部署 19.1.0 啟動時即結算 19.0 賽季兩榜前 5 名；18.x 與更早不發
  # 金額改裁決 2026-11-03（使用者，Hub 直接套用、不走需求模組）：前 6 名 3200／1600／800／400／200／100 → 前 5 名 1200／800／400／200／100

  Background:
    Given 現在時間為 "2026-12-01T00:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 甲   | MEMBER     | 甲       |          | true             | false    |
      | 乙   | MEMBER     | 乙       |          | true             | false    |
      | 丙   | GUEST      | 勇者248  | dev-c    | true             | false    |
      | 丁   | MEMBER     | 丁       |          | false            | false    |
      | 戊   | MEMBER     | 戊       |          | false            | false    |
      | 己   | MEMBER     | 己       |          | false            | false    |
      | 庚   | MEMBER     | 庚       |          | false            | false    |
      | 辛   | MEMBER     | 辛       |          | false            | false    |
    And 玩家 "甲" 的金幣餘額為 100
    And 系統中有以下排行榜成績：
      | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
      | 甲   | 18.7         | BRONZE | r-a0  | true      | 99          | 2026-10-01T10:00:00Z |
      | 甲   | 19.1         | BRONZE | r-a1  | true      | 100         | 2026-11-10T10:00:00Z |
      | 丙   | 19.1         | BRONZE | r-c1  | true      | 105         | 2026-11-10T10:00:00Z |
      | 乙   | 19.1         | BRONZE | r-b1  | true      | 110         | 2026-11-10T10:00:00Z |
      | 丁   | 19.1         | BRONZE | r-d1  | true      | 120         | 2026-11-10T10:00:00Z |
      | 戊   | 19.1         | BRONZE | r-e1  | false     | 90          | 2026-11-10T10:00:00Z |
      | 己   | 19.1         | BRONZE | r-f1  | false     | 80          | 2026-11-10T10:00:00Z |
      | 庚   | 19.1         | BRONZE | r-g1  | false     | 70          | 2026-11-10T10:00:00Z |
      | 辛   | 19.1         | BRONZE | r-h1  | false     | 60          | 2026-11-10T10:00:00Z |
      | 甲   | 19.1         | SILVER | r-a2  | true      | 90          | 2026-11-10T11:00:00Z |
      | 乙   | 19.1         | SILVER | r-b2  | false     | 60          | 2026-11-10T11:00:00Z |
    # 19.1 NORMAL 榜名次：甲 1、丙 2（guest）、乙 3、丁 4、戊 5、己 6、庚 7、辛 8；EXTRA 榜：甲 1、乙 2
    # 18.7 成績早於最早發放賽季 19.0，任何啟動都不結算（S9 改裁決 2026-11-03）

  Rule: 前置（狀態）- 只結算早於目前賽季的賽季，目前賽季取自建置版號前兩碼
    # 澄清 AMB-S2＝d、AMB-S6＝b（2026-11-03）；推論 INF-S12：依（大, 中）數值比較，嚴格早於目前賽季才結算；部署回退到較舊版號時，較新的賽季不提早結算（延伸 INF-S6：已發放者不撤回、不重發）
    # 審核 minor 補正（人裁決 A，2026-11-03）：結算在 Flyway 完成、應用就緒事件之後非阻塞執行（不延遲對外服務）；每（賽季, 榜別）一個獨立交易；入帳前以 SELECT … FOR UPDATE 鎖該玩家錢包列（同購買／廣告入帳，economy/句型.md 通則）；版號取前兩段數字，無法解析時啟動失敗（見 infrastructure.yml）

    Example: 小版本更新時目前賽季不變，不結算任何賽季
      When 後端以建置版號 "19.1.3" 啟動
      Then 賽季獎勵發放記錄應為空
      And 玩家 "甲" 的金幣餘額應為 100

    Example: 中版本切換後啟動時結算上一賽季
      When 後端以建置版號 "19.2.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |

    Example: 大版本切換後啟動時逐一結算所有未發放的舊賽季
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
        | 丙   | 19.2         | BRONZE | r-c3  | true      | 101         | 2026-11-20T10:00:00Z |
      When 後端以建置版號 "20.0.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |
        | 19.2         | BRONZE | 0              | 0          |
        | 19.2         | SILVER | 0              | 0          |

    Example: 部署回退到較舊版號時較新的賽季不提早結算
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
        | 乙   | 19.2         | BRONZE | r-b3  | true      | 101         | 2026-11-20T10:00:00Z |
      When 後端以建置版號 "19.1.4" 啟動
      Then 賽季獎勵發放記錄應為空
      And 玩家 "乙" 的金幣帳本筆數應為 0

    # 審核 minor 補正（人裁決 A，2026-11-03）：賽季以（大, 中）數字比較，非字串比較
    Example: 賽季以數字而非字串比較（19.10 晚於 19.9，19.9 會結算）
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
        | 乙   | 19.9         | BRONZE | r-b9  | true      | 101         | 2027-03-20T10:00:00Z |
      And 系統中有以下賽季獎勵發放記錄：
        | boardVersion | tier   | recipientCount | totalCoins | settledAt            |
        | 19.1         | BRONZE | 4              | 1900       | 2026-11-25T00:00:00Z |
        | 19.1         | SILVER | 2              | 2000       | 2026-11-25T00:00:00Z |
      When 後端以建置版號 "19.10.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |
        | 19.9         | BRONZE | 1              | 1200       |
        | 19.9         | SILVER | 0              | 0          |

  Rule: 前置（狀態）- 早於最早發放賽季（19.0）的賽季不發放
    # S9 改裁決 2026-11-03：19.0 起發（原 AMB-S9＝b「19.0 不發、19.1 起」作廢）；推論 INF-S11：最早發放賽季 "19.0" 放後端設定檔，早於者（18.x 與更早）不結算、不寫發放記錄

    Example: 以 19.1.x 啟動時發放 19.0 賽季（最早發放賽季本身）
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
        | 甲   | 19.0         | BRONZE | r-a9  | true      | 99          | 2026-11-01T10:00:00Z |
      When 後端以建置版號 "19.1.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.0         | BRONZE | 1              | 1200       |
        | 19.0         | SILVER | 0              | 0          |
      And 應有以下賽季獎勵入帳：
        | 玩家 | boardVersion | tier   | rank | coinDelta |
        | 甲   | 19.0         | BRONZE | 1    | 1200      |
      And 玩家 "甲" 的金幣餘額應為 1300

    Example: 18.x 賽季（早於 19.0）的第 1 名不發放且不寫發放記錄
      When 後端以建置版號 "20.0.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |
      And 應有以下賽季獎勵入帳：
        | 玩家 | boardVersion | tier   | rank | coinDelta |
        | 甲   | 19.1         | BRONZE | 1    | 1200      |
        | 乙   | 19.1         | BRONZE | 3    | 400       |
        | 丁   | 19.1         | BRONZE | 4    | 200       |
        | 戊   | 19.1         | BRONZE | 5    | 100       |
        | 甲   | 19.1         | SILVER | 1    | 1200      |
        | 乙   | 19.1         | SILVER | 2    | 800       |

  Rule: 後置（狀態）- NORMAL 與 EXTRA 兩榜各依最終名次發放金幣：第 1 名 1200、第 2 名 800、第 3 名 400、第 4 名 200、第 5 名 100
    # 2026-11-03 使用者改裁決：前 6 名 3200／1600／800／400／200／100 → 前 5 名 1200／800／400／200／100
    # 澄清 AMB-S1＝a（2026-11-03）：兩榜（tier=BRONZE 顯示 NORMAL、tier=SILVER 顯示 EXTRA）各發前 5 名（名次數依 2026-11-03 使用者改裁決，原前 6 名），同一人兩榜可各領一筆
    # 澄清 AMB-S3＝a（2026-11-03，確認 INF-S10）：名次依查詢排行榜排序規則（GAP-12）於發放當下判定，每人一筆最佳，含 guest 佔位
    # 推論 INF-S1：金幣表放後端設定檔，以發放當下設定值入帳；第 6 名起不發（2026-11-03 使用者改裁決，原第 7 名起）

    Example: 兩榜前 5 名的已登入玩家依名次入帳，同一人兩榜各領一筆
      When 後端以建置版號 "19.2.0" 啟動
      Then 應有以下賽季獎勵入帳：
        | 玩家 | boardVersion | tier   | rank | coinDelta |
        | 甲   | 19.1         | BRONZE | 1    | 1200      |
        | 乙   | 19.1         | BRONZE | 3    | 400       |
        | 丁   | 19.1         | BRONZE | 4    | 200       |
        | 戊   | 19.1         | BRONZE | 5    | 100       |
        | 甲   | 19.1         | SILVER | 1    | 1200      |
        | 乙   | 19.1         | SILVER | 2    | 800       |
      And 玩家 "甲" 的金幣餘額應為 2500
      And 玩家 "乙" 的金幣餘額應為 1200

    Example: 第 6 名起不發放
      When 後端以建置版號 "19.2.0" 啟動
      Then 玩家 "己" 的金幣帳本筆數應為 0
      And 玩家 "庚" 的金幣帳本筆數應為 0
      And 玩家 "辛" 的金幣帳本筆數應為 0

  Rule: 後置（狀態）- 只發給已綁定第三方身分的玩家，guest 佔的名次跳過不遞補
    # 推論 INF-S2：以發放當下身分判定（MEMBER、未作廢；本輪實質為 Google，Apple 延後 BDY-01=B）
    # 澄清 AMB-S4＝a（2026-11-03）：guest 佔名次時該名次獎勵跳過，不由後面的登入玩家遞補（仍只看前 5 名，2026-11-03 使用者改裁決，原前 6 名）
    # 澄清 AMB-S8＝a（2026-11-03）：賽季結算後才綁定的玩家不補發

    Example: guest 佔第 2 名時跳過不遞補
      When 後端以建置版號 "19.2.0" 啟動
      Then 玩家 "丙" 的金幣帳本筆數應為 0
      And 玩家 "己" 的金幣帳本筆數應為 0
      And 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |

    Example: 發放當下是 guest 而跳過的名次，之後綁定 Google 且重啟也不補發
      # 審核 minor 補正（人裁決 A）：與「已有發放記錄的賽季重新啟動時不重複入帳」區分前提——此處發放當下該名次玩家是 guest（跳過），非先有發放記錄
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 癸   | GUEST      | 癸       | dev-j    | false            | false    |
      And 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run   | isVictory | gameSeconds | settledAt            |
        | 癸   | 19.1         | BRONZE | r-k1  | true      | 103         | 2026-11-10T09:00:00Z |
      # 19.1 NORMAL 榜名次：甲 1、癸 2（guest，跳過）、丙 3（guest，跳過）、乙 4、丁 5、戊 6（第 6 名不發）
      When 後端以建置版號 "19.2.0" 啟動
      Then 玩家 "癸" 的金幣帳本筆數應為 0
      And 賽季獎勵發放記錄應包含：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 3              | 1500       |
      When 玩家 "癸" 綁定 Google 帳號
      And 後端以建置版號 "19.2.1" 重新啟動
      Then 玩家 "癸" 的金幣帳本筆數應為 0
      And 玩家 "癸" 的金幣餘額應為 0
      And 賽季獎勵發放記錄應包含：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 3              | 1500       |

  Rule: 後置（狀態）- 每筆發放應增加玩家金幣餘額並記入帳本，帳本來源為賽季獎勵並記錄賽季、榜別與名次
    # 推論 INF-S8／INF-S16：帳本列帶賽季、榜別、名次供錢包頁顯示；發放順序 BRONZE 先於 SILVER、榜內名次由小到大；帳本列 created_by＝受獎玩家（INF-S7）
    # 澄清 AMB-S7＝a（2026-11-03，確認 INF-S9）：不受任何上限；廣告獎勵每日次數只計 AD_REWARD，不受影響

    Example: 受獎玩家的帳本依發放順序新增賽季獎勵列
      When 後端以建置版號 "19.2.0" 啟動
      Then 玩家 "甲" 的金幣帳本應依序包含：
        | ledgerSource  | coinDelta | coinsAfter | seasonBoardVersion | seasonTier | seasonRank |
        | SEASON_REWARD | 1200      | 2500       | 19.1               | SILVER     | 1          |
        | SEASON_REWARD | 1200      | 1300       | 19.1               | BRONZE     | 1          |

  Rule: 後置（狀態）- 每筆發放應記錄一筆賽季獎勵事件
    # 澄清 AMB-S12＝b（2026-11-03）：新增事件類型 SEASON_REWARD，納入查詢事件統計；isSilverUnlocked 取發放當下

    Example: 每位受獎玩家每榜各記一筆賽季獎勵事件
      When 後端以建置版號 "19.2.0" 啟動
      Then 事件數應為 6
      And 應記錄以下事件：
        | eventKind     | 玩家 | isSilverUnlocked |
        | SEASON_REWARD | 甲   | true             |
        | SEASON_REWARD | 乙   | true             |
        | SEASON_REWARD | 丁   | false            |
        | SEASON_REWARD | 戊   | false            |
        | SEASON_REWARD | 甲   | true             |
        | SEASON_REWARD | 乙   | true             |

  Rule: 後置（狀態）- 同一賽季同一榜只發放一次，重複或同時觸發不得重複入帳
    # 推論 INF-S4／INF-S14：發放記錄（賽季＋榜別唯一）與帳本、錢包、事件同一交易；多實例同時啟動時後到者因唯一鍵放棄
    # 澄清 AMB-S10＝a（2026-11-03）：已發放賽季之後才結算的舊賽季 run 照常上榜，獎勵不重算（見 結算對局.feature）

    Example: 已有發放記錄的賽季重新啟動時不重複入帳
      Given 系統中有以下賽季獎勵發放記錄：
        | boardVersion | tier   | recipientCount | totalCoins | settledAt            |
        | 19.1         | BRONZE | 4              | 1900       | 2026-11-25T00:00:00Z |
        | 19.1         | SILVER | 2              | 2000       | 2026-11-25T00:00:00Z |
      When 後端以建置版號 "19.2.1" 啟動
      Then 玩家 "甲" 的金幣餘額應為 100
      And 玩家 "甲" 的金幣帳本筆數應為 0
      And 事件數應為 0

    Example: 兩個後端實例同時啟動時只發放一次
      When 兩個後端實例同時以建置版號 "19.2.0" 啟動
      Then 玩家 "甲" 的金幣餘額應為 2500
      And 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |

  Rule: 後置（狀態）- 單一（賽季, 榜別）交易失敗只回滾該榜，下次啟動只補失敗的榜
    # 審核 minor 補正（人裁決 A，2026-11-03）：每（賽季, 榜別）一個獨立交易（INF-S14）；失敗榜無發放記錄、無帳本、無事件；以測試注入「SILVER 榜入帳失敗」表達（句型待 economy/句型.md 補列）

    Example: SILVER 榜交易失敗回滾，BRONZE 榜已成功，下次啟動只補 SILVER
      Given 賽季 "19.1" 的 "SILVER" 榜入帳會失敗
      When 後端以建置版號 "19.2.0" 啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
      And 玩家 "乙" 的金幣帳本筆數應為 1
      And 事件數應為 4
      Given 賽季 "19.1" 的 "SILVER" 榜入帳不再失敗
      When 後端以建置版號 "19.2.0" 重新啟動
      Then 賽季獎勵發放記錄應為：
        | boardVersion | tier   | recipientCount | totalCoins |
        | 19.1         | BRONZE | 4              | 1900       |
        | 19.1         | SILVER | 2              | 2000       |
      And 玩家 "乙" 的金幣帳本筆數應為 2
      And 事件數應為 6
