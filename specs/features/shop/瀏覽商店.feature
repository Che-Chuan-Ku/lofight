@query
Feature: 瀏覽商店
  銀牌商店販售單局消耗型 buff（攻速 +20%、ATK+1、砲台、雷射）；bronze 玩家可看不可用（D09）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 廣告發布商 ID 已設定
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗且不記事件
      Given 目前未攜帶存取權杖
      When 瀏覽商店
      Then 操作失敗，錯誤代碼為 "401003"
      And 事件數應為 0

  Rule: 後置（回應）- 應回傳全部 buff 品項及其價格
    # 原文 D09、D10–D12
    # 澄清 GAP-02（2026-10-29）採 A：攻速 +20% 200 金幣、砲台 400 金幣、雷射 600 金幣；價格放後端設定檔（可調），回應價格以設定檔當下值為準
    # CHANGE 2026-11-02（D37）使用者裁決：新增 ATTACK（ATK+1，士兵攻擊 +1）200 金幣；品項固定依 ATTACK_SPEED、ATTACK、CANNON、LASER 排列（Hub 直接套用，未開需求模組整輪）

    Example: 回傳全部 buff 品項及設定檔當下價格
      Given 目前以玩家 "阿銀" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "items" 應為：
        | buffItem     | price | isPurchasable |
        | ATTACK_SPEED | 200   | true          |
        | ATTACK       | 200   | true          |
        | CANNON       | 400   | true          |
        | LASER        | 600   | true          |

  Rule: 後置（回應）- silver 未解鎖時每個品項應標示為不可購買並附解鎖條件與目前 bronze 最佳擊破王數
    # 原文 D09「bronze 玩家可看不可用」；澄清 ASM-07（2026-10-29）採 A：上鎖可見、未解鎖不可購買，顯示解鎖條件與目前 bronze 最佳王數
    # 審核修正（2026-10-30）：商店頁需顯示 bronze 最佳王數，改由瀏覽商店回應一併提供（一次請求）；計算同 查詢玩家進度.feature（只計已結算 run）

    Example: silver 未解鎖時每個品項標示不可購買並附解鎖條件與目前 bronze 最佳擊破王數（只計已結算的 run）
      Given 玩家 "小明" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState | bossesKilled |
        | r-01 | BRONZE |              | 19.0         | 2026-10-31T10:00:00Z | SETTLED  | 3            |
        | r-02 | BRONZE |              | 19.0         | 2026-10-31T11:00:00Z | SETTLED  | 5            |
        | r-03 | BRONZE |              | 19.0         | 2026-10-31T12:00:00Z | OPEN     |              |
      And 目前以玩家 "小明" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "items" 應為：
        | buffItem     | price | isPurchasable | silverUnlockBossSeq | bronzeBestBossesKilled |
        | ATTACK_SPEED | 200   | false         | 8                   | 5                      |
        | ATTACK       | 200   | false         | 8                   | 5                      |
        | CANNON       | 400   | false         | 8                   | 5                      |
        | LASER        | 600   | false         | 8                   | 5                      |

  Rule: 後置（回應）- 應回傳看廣告選項狀態：是否開放、今日剩餘次數與下次可領取時間
    # CHANGE 2026-11-02（D33–D35）澄清 AMB-A6＝a：瀏覽商店回應附 adReward，商店卡片一次請求即可顯示（lock exception：Scope Lock 暫掛時已預告）
    # 澄清 AMB-A1＝a：每日 5 次、間隔 60 秒、UTC 日界；AMB-A7＝a：站台未設定發布商 ID 時標示未開放
    # 推論 INF-A12：可立即領取時下次可領取時間為空；冷卻中為最後一次 + 60 秒；已達上限為 max(次一 UTC 日界 00:00:00, 最後一筆 AD_REWARD 時間 + 60 秒)，冷卻 60 秒跨日界仍適用；未開放時剩餘次數與下次可領取時間皆為空
    # 看廣告選項不需已解鎖 EXTRA（推論 INF-A1）

    Example: 今日尚未領取時可立即領取，剩餘 5 次
      Given 目前以玩家 "阿銀" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "adReward" 物件應為：
        | isEnabled | remainingCount | nextClaimableAt |
        | true      | 5              |                 |

    Example: 冷卻中時回傳剩餘次數與冷卻結束時間（前一個 UTC 日的領取不計入）
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 200        | 2026-10-31T23:59:00Z |
        | AD_REWARD    | 200       | 400        | 2026-11-01T11:59:30Z |
      And 目前以玩家 "小明" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "adReward" 物件應為：
        | isEnabled | remainingCount | nextClaimableAt      |
        | true      | 4              | 2026-11-01T12:00:30Z |

    Example: 今日已達上限時剩餘 0 次，下次可領取時間為次一 UTC 日界
      Given 玩家 "阿銀" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 200        | 2026-11-01T01:00:00Z |
        | AD_REWARD    | 200       | 400        | 2026-11-01T02:00:00Z |
        | AD_REWARD    | 200       | 600        | 2026-11-01T03:00:00Z |
        | AD_REWARD    | 200       | 800        | 2026-11-01T04:00:00Z |
        | AD_REWARD    | 200       | 1000       | 2026-11-01T05:00:00Z |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "adReward" 物件應為：
        | isEnabled | remainingCount | nextClaimableAt      |
        | true      | 0              | 2026-11-02T00:00:00Z |

    Example: 站台未設定發布商 ID 時標示未開放
      Given 廣告發布商 ID 未設定
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 回應中的 "adReward" 物件應為：
        | isEnabled | remainingCount | nextClaimableAt |
        | false     |                |                 |

  Rule: 後置（狀態）- 應記錄一筆商店瀏覽事件
    # 原文 D21「另記錄商店瀏覽、購買事件；事件可查」；澄清 ASM-09（2026-10-29）採 B：瀏覽時寫一筆事件（玩家、發生時間、silver 是否已解鎖），供 查詢事件統計.feature 彙總
    # 本 feature 仍為 @query：事件寫入為附帶紀錄，不改變任何玩家可見狀態

    Scenario Outline: 每次瀏覽記錄一筆商店瀏覽事件（含當下 silver 狀態）
      Given 目前以玩家 "<玩家>" 的有效存取權杖操作
      When 瀏覽商店
      Then 操作成功
      And 應記錄以下事件：
        | eventKind | 玩家    | isSilverUnlocked   | occurredAt           |
        | SHOP_VIEW | <玩家>  | <isSilverUnlocked> | 2026-11-01T12:00:00Z |

      Examples:
        | 玩家 | isSilverUnlocked |
        | 阿銀 | true             |
        | 小明 | false            |
