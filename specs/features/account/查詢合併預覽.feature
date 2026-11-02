@query
Feature: 查詢合併預覽
  G19 規則 4（澄清 ASM-05＝A，2026-10-29）：guest 以 Google/Apple 登入前，先以 guest 存取權杖與第三方 token 查詢「將合併」的內容，玩家確認後才呼叫 Google登入／Apple登入。本查詢不寫入任何資料。

  Background:
    Given Google 驗證以下 id_token 的結果為：
      | idToken | isValid | sub     | name   |
      | g-ok-1  | true    | g-sub-1 | 王小明 |
      | g-ok-2  | true    | g-sub-2 |        |
      | g-bad   | false   |         |        |
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小客 | GUEST      | 勇者248  | dev-0001 | true             | false    |
      | 阿明 | MEMBER     | 阿明     |          | false            | false    |
    And 玩家 "阿明" 有以下第三方身分：
      | loginProvider | providerSubject |
      | GOOGLE        | g-sub-1         |
    And 玩家 "小客" 的金幣餘額為 50
    And 玩家 "阿明" 的金幣餘額為 300

  Rule: 前置（參數）- guest 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | GOOGLE        | g-ok-1        |
      Then 操作失敗，錯誤代碼為 "401003"

    @error:403001
    Example: 以已登入帳號的存取權杖查詢時操作失敗
      Given 目前以玩家 "阿明" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | GOOGLE        | g-ok-1        |
      Then 操作失敗，錯誤代碼為 "403001"

  Rule: 前置（參數）- 登入方式（Google 或 Apple）與對應的第三方 token 必須提供
    # 澄清 BDY-01（2026-10-30）採 B：Apple 實作延後，本輪實作僅接受 Google；Apple 隨 Apple登入.feature 延後（推論）

    Scenario Outline: 登入方式或第三方 token 缺漏或無效時操作失敗
      Given 目前以玩家 "小客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider   | providerToken   |
        | <loginProvider> | <providerToken> |
      Then 操作失敗，錯誤代碼為 "<code>"

      @error:400001
      Examples: 缺漏
        | loginProvider | providerToken | code   |
        |               | g-ok-1        | 400001 |
        | GOOGLE        |               | 400001 |

      @error:400012
      Examples: 不支援的登入方式
        | loginProvider | providerToken | code   |
        | FACEBOOK      | g-ok-1        | 400012 |

  Rule: 前置（參數）- 本輪登入方式僅接受 Google
    # 澄清 BDY-01（2026-10-30）採 B：Apple 實作延後；審核修正（2026-10-30）：送 APPLE 回 acc 錯誤碼「尚未支援的登入方式」（acc004，HTTP 400），與 Enum 無效（400012，如 FACEBOOK）區分

    @error:acc004
    Example: 以 Apple 查詢合併預覽時回尚未支援的登入方式
      Given 目前以玩家 "小客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | APPLE         | a-ok-1        |
      Then 操作失敗，錯誤代碼為 "acc004"
      And 錯誤訊息應為 "尚未支援的登入方式"
      And 玩家 "小客" 應為：
        | isVoided | coins |
        | false    | 50    |

  Rule: 前置（參數）- 第三方 token 必須驗證通過
    # 驗證方式同 Google登入／Apple登入 的前置（參數）Rule；澄清 BDY-04（2026-10-29）採 A：驗證失敗回 401 與同一個 acc 錯誤碼「登入驗證失敗」，原因只記後端日誌

    @error:acc001
    Example: 第三方 token 驗證失敗時回登入驗證失敗
      Given 目前以玩家 "小客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | GOOGLE        | g-bad         |
      Then 操作失敗，錯誤代碼為 "acc001"
      And 錯誤訊息應為 "登入驗證失敗"

  Rule: 前置（狀態）- guest 身分必須尚未被合併
    # 澄清 ASM-05（G19 規則 3）：同一 guest 只合併一次

    @error:401001
    Example: guest 已被合併作廢時操作失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 舊客 | GUEST      | 砲手001  | dev-old  | false            | true     |
      And 目前以玩家 "舊客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | GOOGLE        | g-ok-1        |
      Then 操作失敗，錯誤代碼為 "401001"

  Rule: 後置（回應）- 應回傳目標帳號為新建或既有
    # G19 規則 1／2 的分流依據

    Scenario Outline: 依第三方身分是否已有帳號回傳目標帳號為新建或既有，且不寫入任何資料
      Given 目前以玩家 "小客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken   |
        | GOOGLE        | <providerToken> |
      Then 操作成功
      And 回應資料應為：
        | targetKind   |
        | <targetKind> |
      And 玩家 "小客" 應為：
        | isVoided | coins |
        | false    | 50    |

      Examples:
        | providerToken | targetKind |
        | g-ok-2        | NEW        |
        | g-ok-1        | EXISTING   |

  Rule: 後置（回應）- 應回傳將併入的金幣、buff 庫存、silver 解鎖狀態與各榜最佳成績，以及合併後的結果
    # 既有帳號時依 G19 規則 2 計算：金幣與庫存相加、silver 取 OR、各榜留最佳

    Example: 既有帳號時回傳將併入內容與合併後結果
      Given 玩家 "小客" 的 buff 庫存為：
        | buffItem | quantity |
        | CANNON   | 1        |
      And 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 阿明 | 19.0         | BRONZE | r-01 | false     | 80          | 2026-10-30T10:00:00Z |
        | 小客 | 19.0         | BRONZE | r-02 | true      | 140         | 2026-10-31T10:00:00Z |
      And 目前以玩家 "小客" 的有效存取權杖操作
      When 查詢合併預覽：
        | loginProvider | providerToken |
        | GOOGLE        | g-ok-1        |
      Then 操作成功
      And 回應資料應為：
        | targetKind | guestCoins | guestIsSilverUnlocked | mergedCoins | mergedIsSilverUnlocked |
        | EXISTING   | 50         | true                  | 350         | true                   |
      And 回應中的 "mergedBuffInventories" 應為：
        | buffItem     | quantity |
        | ATTACK_SPEED | 0        |
        | CANNON       | 1        |
        | LASER        | 0        |
      And 回應中的 "mergedBestScores" 應為：
        | boardVersion | tier   | isVictory | gameSeconds |
        | 19.0         | BRONZE | true      | 140         |
