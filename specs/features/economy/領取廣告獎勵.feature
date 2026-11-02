@command
Feature: 領取廣告獎勵
  玩家在商店看完 Google 獎勵廣告（H5 Games Ads，adBreak type=reward 的 adViewed 回呼）後，前端回報看完，後端檢查限制後入帳 200 金幣並回傳新餘額與剩餘次數（D33、D34；CHANGE 2026-11-02，documents/changes/2026-11-02-rewarded-ad.md）。
  後端為最終裁判；Google 端無伺服器驗證，不做對帳（AMB-A5＝a）。Guest 與已登入玩家皆可領取，不需已解鎖 EXTRA（推論 INF-A1：D33「guest 與登入玩家皆可」，未設解鎖條件）。
  政策紅線（蓋板與關閉鈕由 Google 繪製、獎勵只綁 adViewed、只在商店內玩家主動按下時播放）由前端遵守，正本見 specs/ui/商店頁.md「廣告政策規則」；後端無法驗證是否真的看完。
  未看完或無廣告可放時前端不呼叫本功能、不計次（AMB-A3＝a）。
  # 澄清 AMB-A1＝a（2026-11-02）：每日 5 次、間隔 60 秒、UTC 日界；與獎勵金額 200 同放後端設定檔
  # 澄清 AMB-A2＝b（2026-11-02）＋模組決定：當日已領取次數＝該玩家當日（UTC）AD_REWARD 帳本筆數，冷卻起點＝最後一筆 AD_REWARD 帳本時間；不另建記錄表
  # 推論 INF-A10：檢查順序 參數 → 已開放（eco003）→ 每日上限（eco001）→ 冷卻（eco002）；同時已達上限且冷卻中回 eco001

  Background:
    Given 現在時間為 "2026-11-02T12:00:00Z"
    And 廣告發布商 ID 已設定
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
    And 玩家 "小明" 的金幣餘額為 100
    And 玩家 "阿明" 的金幣餘額為 50
    And 目前以玩家 "小明" 的有效存取權杖操作

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗且不入帳
      Given 目前未攜帶存取權杖
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作失敗，錯誤代碼為 "401003"
      And 玩家 "小明" 的金幣餘額應為 100

  Rule: 前置（參數）- 裝置識別必須提供
    # 澄清 AMB-A5＝a（2026-11-02）：不對帳；請求帶前端 device id，記入帳本與事件供稽核（lock exception：答覆直接衍生的必填參數）
    # 推論 INF-A14：格式同 建立guest身分 的 deviceId（1–64 字）；不驗證與玩家綁定（已登入玩家亦帶本機 device id）

    @error:400001
    Example: 未提供裝置識別時操作失敗，不入帳、不記帳本與事件
      When 領取廣告獎勵：
        | deviceId |
        |          |
      Then 操作失敗，錯誤代碼為 "400001"
      And 玩家 "小明" 的金幣餘額應為 100
      And 玩家 "小明" 的金幣帳本筆數應為 0
      And 事件數應為 0

  Rule: 前置（狀態）- 廣告獎勵功能必須已開放（站台已設定發布商 ID）
    # 原文 D35「發布商 ID 以環境變數注入頁面、未設定時商店不顯示此選項」
    # 澄清 AMB-A7＝a（2026-11-02）：後端讀同一環境變數（ADSENSE_PUBLISHER_ID），未設定時拒絕入帳（eco003）；瀏覽商店回應標示未開放

    @error:eco003
    Example: 站台未設定發布商 ID 時操作失敗，不入帳、不記帳本與事件
      Given 廣告發布商 ID 未設定
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作失敗，錯誤代碼為 "eco003"
      And 玩家 "小明" 的金幣餘額應為 100
      And 玩家 "小明" 的金幣帳本筆數應為 0
      And 事件數應為 0

  Rule: 前置（狀態）- 玩家當日已領取次數必須未達每日上限（5 次，UTC 日界）
    # 原文 D34「後端檢查每日次數與冷卻後入帳；超限回錯誤碼」
    # 澄清 AMB-A1＝a：每日 5 次，以 UTC 00:00 為日界
    # 澄清 AMB-A2＝b：guest 合併進帳號後不沿用 guest 的當日已用次數（見 Google登入.feature）；次數只計本人帳本

    @error:eco001
    Example: 當日已領取 5 次時操作失敗，不入帳、不記帳本與事件
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 200        | 2026-11-02T01:00:00Z |
        | AD_REWARD    | 200       | 400        | 2026-11-02T02:00:00Z |
        | AD_REWARD    | 200       | 600        | 2026-11-02T03:00:00Z |
        | AD_REWARD    | 200       | 800        | 2026-11-02T04:00:00Z |
        | AD_REWARD    | 200       | 1000       | 2026-11-02T05:00:00Z |
      And 玩家 "小明" 的金幣餘額為 1000
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作失敗，錯誤代碼為 "eco001"
      And 玩家 "小明" 的金幣餘額應為 1000
      And 玩家 "小明" 的金幣帳本筆數應為 5
      And 事件數應為 0

    @error:eco001
    Example: 已達上限且仍在冷卻中時回已達上限
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T11:55:00Z |
        | AD_REWARD    | 200       | 500        | 2026-11-02T11:56:00Z |
        | AD_REWARD    | 200       | 700        | 2026-11-02T11:57:00Z |
        | AD_REWARD    | 200       | 900        | 2026-11-02T11:58:00Z |
        | AD_REWARD    | 200       | 1100       | 2026-11-02T11:59:30Z |
      And 玩家 "小明" 的金幣餘額為 1100
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作失敗，錯誤代碼為 "eco001"
      And 玩家 "小明" 的金幣餘額應為 1100
      And 玩家 "小明" 的金幣帳本筆數應為 5
      And 事件數應為 0

    Example: 當日已領取 4 次時仍可領取第 5 次
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T01:00:00Z |
        | AD_REWARD    | 200       | 500        | 2026-11-02T02:00:00Z |
        | AD_REWARD    | 200       | 700        | 2026-11-02T03:00:00Z |
        | AD_REWARD    | 200       | 900        | 2026-11-02T04:00:00Z |
      And 玩家 "小明" 的金幣餘額為 900
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 玩家 "小明" 的金幣餘額應為 1100

    Example: 前一個 UTC 日的領取不計入當日次數
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-01T23:55:00Z |
        | AD_REWARD    | 200       | 500        | 2026-11-01T23:56:00Z |
        | AD_REWARD    | 200       | 700        | 2026-11-01T23:57:00Z |
        | AD_REWARD    | 200       | 900        | 2026-11-01T23:58:00Z |
        | AD_REWARD    | 200       | 1100       | 2026-11-01T23:59:00Z |
      And 玩家 "小明" 的金幣餘額為 1100
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 回應資料應為：
        | walletCoins | remainingCount |
        | 1300        | 4              |

  Rule: 前置（狀態）- 距玩家上次領取必須已滿冷卻時間（60 秒）
    # 原文 D34「冷卻中不入帳」；政策紅線「不得連續出現」
    # 澄清 AMB-A1＝a：間隔 60 秒；距上次領取滿 60 秒（含）即可再領；冷卻只看本人帳本（AMB-A2＝b）

    @error:eco002
    Example: 距上次領取未滿 60 秒時操作失敗，不入帳
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T11:59:01Z |
      And 玩家 "小明" 的金幣餘額為 300
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作失敗，錯誤代碼為 "eco002"
      And 玩家 "小明" 的金幣餘額應為 300
      And 玩家 "小明" 的金幣帳本筆數應為 1

    Example: 距上次領取恰滿 60 秒時可以領取
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T11:59:00Z |
      And 玩家 "小明" 的金幣餘額為 300
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 玩家 "小明" 的金幣餘額應為 500

    @error:eco002
    Example: 同時送出兩筆領取時一筆成功、一筆冷卻中
      # 推論 INF-A3：同一玩家的領取依序處理（鎖錢包列），不會重複入帳
      When 同時送出以下廣告獎勵領取：
        | deviceId |
        | dev-0001 |
        | dev-0001 |
      Then 應有 1 筆成功、1 筆失敗，失敗錯誤代碼為 "eco002"
      And 玩家 "小明" 的金幣餘額應為 300
      And 玩家 "小明" 的金幣帳本筆數應為 1

  Rule: 後置（狀態）- 玩家金幣餘額應增加 200 並記入帳本，帳本來源為廣告獎勵並記錄裝置識別
    # 原文 D33「完整看完才入帳 200」；D34 acceptance「帳本來源為廣告獎勵」
    # 推論 INF-A2：獎勵金額 200 放後端設定檔（同 GAP-02 價格作法），以領取當下設定值入帳
    # 推論 INF-A3：同一玩家的領取依序處理（同 BDY-07 作法）；入帳、帳本、事件在同一交易，任一失敗整筆回滾；被前置 Rule 拒絕不入帳、不記帳本與事件、不計次
    # 澄清 AMB-A5＝a：帳本記時間與請求帶來的裝置識別

    Scenario Outline: 看完廣告後入帳 200 並記一筆廣告獎勵帳本（guest 與已登入玩家皆可，不需已解鎖 EXTRA）
      Given 目前以玩家 "<玩家>" 的有效存取權杖操作
      When 領取廣告獎勵：
        | deviceId   |
        | <deviceId> |
      Then 操作成功
      And 玩家 "<玩家>" 的金幣餘額應為 <coinsAfter>
      And 玩家 "<玩家>" 的金幣帳本應依序包含：
        | ledgerSource | coinDelta | coinsAfter   | deviceId   |
        | AD_REWARD    | 200       | <coinsAfter> | <deviceId> |

      Examples:
        | 玩家 | deviceId | coinsAfter |
        | 小明 | dev-0001 | 300        |
        | 阿明 | dev-9001 | 250        |

  Rule: 後置（狀態）- 應記錄一筆廣告獎勵事件
    # 原文變更說明技術方案「玩家事件記錄『廣告獎勵』」
    # 推論 INF-A4：事件納入 查詢事件統計.feature 的事件類型統計（次數與不重複玩家數）
    # 澄清 AMB-A5＝a：事件記時間與裝置識別

    Example: 領取成功時記錄一筆廣告獎勵事件（含當下 EXTRA 解鎖狀態與裝置識別）
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 應記錄以下事件：
        | eventKind | 玩家 | isSilverUnlocked | deviceId | occurredAt           |
        | AD_REWARD | 小明 | false            | dev-0001 | 2026-11-02T12:00:00Z |

  Rule: 後置（回應）- 應回傳入帳金幣、新金幣餘額、當日剩餘次數與下次可領取時間
    # 原文變更說明技術方案「回傳 coins 與剩餘次數」
    # 推論 INF-A5：附下次可領取時間，供商店卡片顯示冷卻倒數（不另查詢）
    # 推論 INF-A12：剩餘次數＝5 − 當日已領取筆數；已達上限時下次可領取時間＝max(次一 UTC 日界 00:00:00, 最後一筆 AD_REWARD 時間 + 60 秒)；冷卻 60 秒跨日界仍適用

    Example: 當日第 1 次領取後剩餘 4 次，下次可領取時間為 60 秒後
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 回應資料應為：
        | coinsAwarded | walletCoins | remainingCount | nextClaimableAt      |
        | 200          | 300         | 4              | 2026-11-02T12:01:00Z |

    Example: 當日第 5 次領取後剩餘 0 次，下次可領取時間為次一 UTC 日界
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T01:00:00Z |
        | AD_REWARD    | 200       | 500        | 2026-11-02T02:00:00Z |
        | AD_REWARD    | 200       | 700        | 2026-11-02T03:00:00Z |
        | AD_REWARD    | 200       | 900        | 2026-11-02T04:00:00Z |
      And 玩家 "小明" 的金幣餘額為 900
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 回應資料應為：
        | coinsAwarded | walletCoins | remainingCount | nextClaimableAt      |
        | 200          | 1100        | 0              | 2026-11-03T00:00:00Z |

    Example: 跨日界時冷卻 60 秒仍適用，下次可領取時間為 00:00:30
      # 審核 minor 補正（2026-11-02）：nextClaimableAt＝max(次一 UTC 日界, 本次 + 60 秒)
      Given 現在時間為 "2026-11-02T23:59:30Z"
      And 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 300        | 2026-11-02T23:55:00Z |
        | AD_REWARD    | 200       | 500        | 2026-11-02T23:56:00Z |
        | AD_REWARD    | 200       | 700        | 2026-11-02T23:57:00Z |
        | AD_REWARD    | 200       | 900        | 2026-11-02T23:58:00Z |
      And 玩家 "小明" 的金幣餘額為 900
      When 領取廣告獎勵：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 回應資料應為：
        | coinsAwarded | walletCoins | remainingCount | nextClaimableAt      |
        | 200          | 1100        | 0              | 2026-11-03T00:00:30Z |
