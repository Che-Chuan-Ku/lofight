@query
Feature: 查詢金幣帳本
  D08 acceptance「帳本可查」。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | MEMBER     | 小明     |          | true             | false    |
      | 小華 | GUEST      | 新兵777  | dev-0002 | false            | false    |
    And 玩家 "小明" 有以下金幣帳本：
      | ledgerSource  | bossSeq | buffItem | coinDelta | coinsAfter | occurredAt           |
      | BOSS_REWARD   | 1       |          | 1         | 1          | 2026-11-01T10:00:00Z |
      | MERGE_IN      |         |          | 500       | 501        | 2026-11-01T10:30:00Z |
      | BUFF_PURCHASE |         | CANNON   | -400      | 101        | 2026-11-01T11:00:00Z |
    And 玩家 "小華" 有以下金幣帳本：
      | ledgerSource | bossSeq | buffItem | coinDelta | coinsAfter | occurredAt           |
      | BOSS_REWARD  | 1       |          | 1         | 1          | 2026-11-01T10:15:00Z |
    And 目前以玩家 "小明" 的有效存取權杖操作

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢金幣帳本：
        | page |
        |      |
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 前置（參數）- 頁碼若提供必須為正整數，未提供時視為第 1 頁
    # 澄清 GAP-11（2026-10-29）採 A：分頁查詢，每頁固定 20 筆（後端設定檔），api 加頁碼參數

    Scenario Outline: 頁碼不是正整數時操作失敗
      When 查詢金幣帳本：
        | page   |
        | <page> |
      Then 操作失敗，錯誤代碼為 "<code>"

      @error:400007
      Examples: 小於 1
        | page | code   |
        | 0    | 400007 |
        | -1   | 400007 |

      @error:400009
      Examples: 非整數
        | page | code   |
        | abc  | 400009 |

    Example: 未提供頁碼時視為第 1 頁
      When 查詢金幣帳本：
        | page |
        |      |
      Then 操作成功
      And 回應資料應為：
        | page | totalCount | hasNext |
        | 1    | 3          | false   |

  Rule: 後置（回應）- 應回傳金幣入帳與扣款明細，每筆含來源（過關入帳、合併入帳、廣告獎勵、賽季獎勵或購買扣款）、金額與時間
    # CHANGE 2026-11-03（D38）：新增來源「賽季獎勵」SEASON_REWARD（發放賽季獎勵.feature）；該來源列另帶賽季、榜別、名次（INF-S8），其他來源三欄為 null；錢包頁顯示文案「賽季獎勵 NORMAL 第 N 名 +X」屬前端（AMB-S5＝a，ui/錢包頁.md）
    # CHANGE 2026-11-02（D34）：新增來源「廣告獎勵」AD_REWARD（領取廣告獎勵.feature）；錢包頁顯示文案「廣告獎勵 +200」屬前端（AMB-A4，ui/錢包頁.md）；回應不含裝置識別（僅供後端稽核，AMB-A5）
    # 澄清 GAP-11（2026-10-29）採 A：依時間新到舊排序，每頁 20 筆；查詢範圍為該玩家全部帳本紀錄
    # 推論（Consistency Sweep）：同一時間以帳本流水號新到舊排序；回應附總筆數與是否有下一頁；頁碼超過最後一頁回空列表（不報錯）；只回查詢者本人的帳本（guest 以 guest id）

    Example: 只回本人帳本，依時間新到舊列出來源、金額與時間
      When 查詢金幣帳本：
        | page |
        | 1    |
      Then 操作成功
      And 回應中的 "items" 應為：
        | ledgerSource  | coinDelta | occurredAt           |
        | BUFF_PURCHASE | -400      | 2026-11-01T11:00:00Z |
        | MERGE_IN      | 500       | 2026-11-01T10:30:00Z |
        | BOSS_REWARD   | 1         | 2026-11-01T10:00:00Z |

    Example: 每頁 20 筆，回傳總筆數與是否有下一頁
      Given 玩家 "小華" 有 21 筆過關入帳帳本，每筆 +1 金幣，時間自 "2026-11-02T00:00:00Z" 起每分鐘一筆
      And 目前以玩家 "小華" 的有效存取權杖操作
      When 查詢金幣帳本：
        | page |
        | 2    |
      Then 操作成功
      And 回應資料應為：
        | page | totalCount | hasNext |
        | 2    | 22         | false   |
      And 回應中的 "items" 應為：
        | ledgerSource | coinDelta | occurredAt           |
        | BOSS_REWARD  | 1         | 2026-11-02T00:00:00Z |
        | BOSS_REWARD  | 1         | 2026-11-01T10:15:00Z |

    Example: 頁碼超過最後一頁時回空列表
      When 查詢金幣帳本：
        | page |
        | 5    |
      Then 操作成功
      And 回應資料應為：
        | page | totalCount | hasNext |
        | 5    | 3          | false   |
      And 回應中的 "items" 應為空

    Example: 廣告獎勵入帳以廣告獎勵來源列出
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小美 | GUEST      | 新兵888  | dev-0005 | false            | false    |
      And 玩家 "小美" 有以下金幣帳本：
        | ledgerSource | bossSeq | buffItem | coinDelta | coinsAfter | occurredAt           |
        | BOSS_REWARD  | 1       |          | 1         | 1          | 2026-11-02T02:00:00Z |
        | AD_REWARD    |         |          | 200       | 201        | 2026-11-02T03:00:00Z |
      And 目前以玩家 "小美" 的有效存取權杖操作
      When 查詢金幣帳本：
        | page |
        | 1    |
      Then 操作成功
      And 回應中的 "items" 應為：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 201        | 2026-11-02T03:00:00Z |
        | BOSS_REWARD  | 1         | 1          | 2026-11-02T02:00:00Z |

    Example: 賽季獎勵入帳以賽季獎勵來源列出並帶賽季、榜別與名次
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource  | coinDelta | coinsAfter | seasonBoardVersion | seasonTier | seasonRank | occurredAt           |
        | SEASON_REWARD | 1200      | 1301       | 19.1               | BRONZE     | 1          | 2026-12-01T00:00:00Z |
      When 查詢金幣帳本：
        | page |
        | 1    |
      Then 操作成功
      And 回應中的 "items" 應為：
        | ledgerSource  | coinDelta | coinsAfter | seasonBoardVersion | seasonTier | seasonRank | occurredAt           |
        | SEASON_REWARD | 1200      | 1301       | 19.1               | BRONZE     | 1          | 2026-12-01T00:00:00Z |
        | BUFF_PURCHASE | -400      | 101        |                    |            |            | 2026-11-01T11:00:00Z |
        | MERGE_IN      | 500       | 501        |                    |            |            | 2026-11-01T10:30:00Z |
        | BOSS_REWARD   | 1         | 1          |                    |            |            | 2026-11-01T10:00:00Z |

  Rule: 後置（回應）- guest 合併而來的金幣應以一筆合併入帳顯示
    # 依 guest 合併（D03）；澄清 ASM-05（2026-10-29）採 A：合併保留，帳本以單筆「合併入帳」追溯合併來源

    Example: guest 合併而來的金幣以單筆合併入帳顯示
      When 查詢金幣帳本：
        | page |
        | 1    |
      Then 操作成功
      And 回應中 ledgerSource 為 "MERGE_IN" 的項目應為：
        | coinDelta | occurredAt           |
        | 500       | 2026-11-01T10:30:00Z |
