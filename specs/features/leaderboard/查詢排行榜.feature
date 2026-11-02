@query
Feature: 查詢排行榜
  成績由後端儲存；以 (版本前兩碼, 階層) 分 bronze/silver 兩榜查前 15；同帳號留最佳；另回傳查詢者自己與上下各 2 名（D06、G01–G03）。
  # 澄清 ASM-06（2026-10-29）：A 全部採用——分階層兩榜、以帳號（guest 用 guest id）去重、回傳自己與鄰近名次、silver 列顯示裝備 buff
  # 澄清 GAP-06（2026-10-30）採 A：不保留 Google Sheet 過渡，上線即切後端排行榜；舊 Sheet 停用、舊成績不搬移；不新增 @tool feature，infrastructure 無 Google Sheet 依賴

  Background:
    Given 後端設定的目前版本前兩碼為 "19.0"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 甲   | GUEST      | 勇者248  | dev-a    | true             | false    |
      | 乙   | GUEST      | 勇者248  | dev-b    | false            | false    |
      | 丙   | MEMBER     | 丙       |          | true             | false    |
      | 丁   | GUEST      | 丁       | dev-d    | false            | false    |
      | 戊   | GUEST      | 戊       | dev-e    | false            | false    |
      | 無名 | GUEST      | 新兵777  | dev-z    | false            | false    |
    And 系統中有以下排行榜成績：
      | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
      | 甲   | 19.0         | BRONZE | r-a1 | true      | 120         | 2026-11-01T10:02:00Z |
      | 乙   | 19.0         | BRONZE | r-b1 | true      | 120         | 2026-11-01T10:01:00Z |
      | 丙   | 19.0         | BRONZE | r-c1 | true      | 110         | 2026-11-01T10:05:00Z |
      | 丁   | 19.0         | BRONZE | r-d1 | false     | 95          | 2026-11-01T10:00:00Z |
      | 戊   | 19.0         | BRONZE | r-e1 | false     | 90          | 2026-11-01T10:00:00Z |
      | 甲   | 18.6         | BRONZE | r-a0 | true      | 100         | 2026-10-01T10:00:00Z |
    And 玩家 "甲" 有以下 run：
      | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
      | r-a2 | SILVER | LASER        | 19.0         | 2026-11-01T11:00:00Z | SETTLED  |
    And 玩家 "丙" 有以下 run：
      | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
      | r-c2 | SILVER |              | 19.0         | 2026-11-01T11:00:00Z | SETTLED  |
    And 系統中有以下排行榜成績：
      | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
      | 甲   | 19.0         | SILVER | r-a2 | true      | 100         | 2026-11-01T11:05:00Z |
      | 丙   | 19.0         | SILVER | r-c2 | false     | 70          | 2026-11-01T11:05:00Z |
    And 目前以玩家 "丁" 的有效存取權杖操作

  Rule: 前置（參數）- 版本前兩碼未提供時以後端設定檔的目前版本前兩碼為準
    # 澄清 GAP-16（2026-10-29）採 B 衍生推論：前端不持有版本號；回應應帶出實際所查的版本前兩碼供排行榜頁分組標題顯示

    Example: 未提供版本前兩碼時以後端設定的目前版本為準並回傳所查版本
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應資料應為：
        | boardVersion | tier   |
        | 19.0         | BRONZE |
      And 回應中的 "entries" 應有 5 筆

    Example: 指定舊版本前兩碼時回傳該版本榜單
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 18.6         |
      Then 操作成功
      And 回應中的 "entries" 應為：
        | rank | nickname | isVictory | gameSeconds |
        | 1    | 勇者248  | true      | 100         |

  Rule: 前置（參數）- 階層必須為 bronze 或 silver
    # 澄清 ASM-06（2026-10-29）採 A：分階層兩榜

    Scenario Outline: 階層缺漏或不是 bronze／silver 時操作失敗
      When 查詢排行榜：
        | tier   | boardVersion |
        | <tier> |              |
      Then 操作失敗，錯誤代碼為 "<code>"

      @error:400001
      Examples: 缺漏
        | tier | code   |
        |      | 400001 |

      @error:400012
      Examples: 不在允許清單
        | tier | code   |
        | GOLD | 400012 |

  Rule: 後置（回應）- 應回傳該分組前 15 名，依勝局在前、勝局秒數少者在前、敗局存活秒數多者在前、同分先結算者在前排序
    # 原文 D06「查前 15；排序規則同現行」
    # 澄清 GAP-12（2026-10-29）採 A：沿用現行 tools/leaderboard.gs 排序並以結算時間先者打破同分；成績記勝敗與秒數（不以擊破王數排序）；每列回傳名次、暱稱、勝敗、秒數

    Example: 依勝局在前、勝局秒數少、敗局秒數多、同分先結算排序
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應中的 "entries" 應為：
        | rank | nickname | isVictory | gameSeconds |
        | 1    | 丙       | true      | 110         |
        | 2    | 勇者248  | true      | 120         |
        | 3    | 勇者248  | true      | 120         |
        | 4    | 丁       | false     | 95          |
        | 5    | 戊       | false     | 90          |

    Example: 超過 15 筆時只回前 15 名
      Given 版本 "19.5" BRONZE 榜有 20 位玩家的勝局成績，玩家 "P01" 起秒數自 101 起每位 +1
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.5         |
      Then 操作成功
      And 回應中的 "entries" 應有 15 筆
      And 回應中的 "entries" 第 15 筆應為：
        | rank | nickname | isVictory | gameSeconds |
        | 15   | P15      | true      | 115         |

  Rule: 後置（回應）- 同一帳號應只出現一次並保留最佳成績
    # 原文 D06「同暱稱/同帳號留最佳」；澄清 ASM-06（2026-10-29）採 A：以帳號去重（guest 用 guest id），暱稱允許重名不作去重鍵
    # 澄清 GAP-12（2026-10-29）採 A 衍生推論：「最佳」依本榜排序規則判定

    Example: 以帳號去重，暱稱相同的不同玩家各自上榜
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應中暱稱為 "勇者248" 的 "entries" 應有 2 筆
      And 回應中玩家 "丙" 的 "entries" 應有 1 筆

  Rule: 後置（回應）- 應只回傳所查階層的榜單
    # 澄清 ASM-06（2026-10-29）採 A：依 G01 以 (版本前兩碼, 階層) 分榜

    Example: 只回傳所查階層的榜單
      When 查詢排行榜：
        | tier   | boardVersion |
        | SILVER |              |
      Then 操作成功
      And 回應中的 "entries" 應有 2 筆

  Rule: 後置（回應）- silver 榜每列應顯示該成績所屬 run 裝填的 buff
    # 澄清 ASM-06（2026-10-29）採 A；buff 取自成績關聯 run_id 的 loadout（開局.feature 記錄）

    Example: silver 榜每列顯示該成績所屬 run 裝填的 buff
      When 查詢排行榜：
        | tier   | boardVersion |
        | SILVER |              |
      Then 操作成功
      And 回應中的 "entries" 應為：
        | rank | nickname | isVictory | gameSeconds | equippedBuff |
        | 1    | 勇者248  | true      | 100         | LASER        |
        | 2    | 丙       | false     | 70          |              |

  Rule: 後置（回應）- 應回傳查詢者自己的名次與上下各 2 名
    # 澄清 ASM-06（2026-10-29）採 A：依 G03 回傳自己名次與上下各 2 名
    # 澄清 BDY-08（2026-10-29）採 A：查詢者在該榜無成績時自己名次回空值、不回鄰近名次；已在前 15 名內仍照常回傳自己名次與上下各 2 名
    # 推論（使用者未裁決）：上下不足 2 名時有幾名回幾名；查詢者以存取權杖識別（guest 以 guest id）；依所查（版本前兩碼, 階層）分組計算

    Example: 回傳查詢者自己的名次與上下各 2 名
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應中的 "myRank" 應為：
        | rank | nickname | isVictory | gameSeconds |
        | 4    | 丁       | false     | 95          |
      And 回應中的 "neighbors" 應為：
        | rank | nickname | isVictory | gameSeconds |
        | 2    | 勇者248  | true      | 120         |
        | 3    | 勇者248  | true      | 120         |
        | 5    | 戊       | false     | 90          |

    Example: 名次在榜首時上方不足 2 名則有幾名回幾名
      Given 目前以玩家 "丙" 的有效存取權杖操作
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應中的 "myRank" 應為：
        | rank | nickname |
        | 1    | 丙       |
      And 回應中的 "neighbors" 應為：
        | rank | nickname |
        | 2    | 勇者248  |
        | 3    | 勇者248  |

    Example: 查詢者在該榜無成績時自己名次為空且不回鄰近名次
      Given 目前以玩家 "無名" 的有效存取權杖操作
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 操作成功
      And 回應中的 "myRank" 應為空
      And 回應中的 "neighbors" 應為空
