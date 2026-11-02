@command
Feature: Google登入
  D01：前端以 Google Identity 取得 id_token；後端驗證簽章與 aud 後建立/對應帳號並核發 JWT

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統預設暱稱產生器下一個值為 "老兵512"
    And Google 驗證以下 id_token 的結果為：
      | idToken | isValid | sub     | name   |
      | g-ok-1  | true    | g-sub-1 | 王小明 |
      | g-bad   | false   |         |        |

  Rule: 前置（參數）- Google id_token 必須提供

    @error:400001
    Example: 未提供 Google id_token 時操作失敗
      When 以 Google 登入：
        | idToken | 攜帶guest |
        |         |           |
      Then 操作失敗，錯誤代碼為 "400001"

  Rule: 前置（參數）- Google id_token 的簽章與 aud 必須驗證通過
    # 澄清 BDY-04（2026-10-29）採 A：簽章錯、aud 不符、過期等任何驗證失敗一律回 401 與單一 acc 錯誤碼，訊息統一為「登入驗證失敗」；失敗原因只記在後端日誌，不回給前端

    @error:acc001
    Example: id_token 驗證失敗時回登入驗證失敗且不建立帳號
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-bad   |           |
      Then 操作失敗，錯誤代碼為 "acc001"
      And 錯誤訊息應為 "登入驗證失敗"
      And 系統中帳號（MEMBER）數應為 0

  Rule: 後置（狀態）- 第三方身分首次登入應建立新帳號，再次登入應對應同一帳號
    # 原文 D01 acceptance「登入後取得 JWT；重複登入對應同一帳號」

    Example: 首次登入建立新帳號
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  |           |
      Then 操作成功
      And 第三方身分 GOOGLE "g-sub-1" 對應的帳號應為：
        | playerKind | isSilverUnlocked | coins |
        | MEMBER     | false            | 0     |

    Example: 再次登入對應同一帳號
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿明 | MEMBER     | 阿明     |          | true             | false    |
      And 玩家 "阿明" 有以下第三方身分：
        | loginProvider | providerSubject |
        | GOOGLE        | g-sub-1         |
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  |           |
      Then 操作成功
      And 回應中的存取權杖應可識別為玩家 "阿明"
      And 第三方身分 GOOGLE "g-sub-1" 對應的帳號數應為 1

  Rule: 後置（狀態）- 帳號暱稱不得取用第三方回傳的姓名
    # 原文「排行榜名稱一樣不要拿三方登入回來的名稱」

    Example: 新帳號暱稱為系統預設，不取用 Google 回傳的姓名
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  |           |
      Then 操作成功
      And 第三方身分 GOOGLE "g-sub-1" 對應的帳號應為：
        | nickname |
        | 老兵512  |

  Rule: 後置（狀態）- 帶有 guest 身分登入且第三方帳號為新建時，guest 的金幣、buff 庫存、silver 解鎖進度、成績應整批轉移到新帳號
    # 原文 clarify「首次登入合併金幣、庫存、進度、成績到帳號，guest 身分作廢」；澄清 ASM-05（2026-10-29）採 A（G19 規則 1）

    Example: 攜帶 guest 首次登入時 guest 資產整批轉移到新帳號
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小客 | GUEST      | 勇者248  | dev-0001 | true             | false    |
      And 玩家 "小客" 的金幣餘額為 120
      And 玩家 "小客" 的 buff 庫存為：
        | buffItem | quantity |
        | LASER    | 2        |
      And 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 小客 | 19.0         | BRONZE | r-01 | true      | 150         | 2026-10-31T10:00:00Z |
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  | 小客      |
      Then 操作成功
      And 第三方身分 GOOGLE "g-sub-1" 對應的帳號應為：
        | playerKind | isSilverUnlocked | coins |
        | MEMBER     | true             | 120   |
      And 第三方身分 GOOGLE "g-sub-1" 對應帳號的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 2        |
      And 第三方身分 GOOGLE "g-sub-1" 對應帳號的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-01 | true      | 150         |
      And 第三方身分 GOOGLE "g-sub-1" 對應帳號的金幣帳本應依序包含：
        | ledgerSource | coinDelta | coinsAfter |
        | MERGE_IN     | 120       | 120        |

  Rule: 後置（狀態）- 帶有 guest 身分登入且第三方帳號已存在時，金幣與 buff 庫存應相加、silver 解鎖取兩者之一已解鎖即解鎖、各榜成績各留最佳
    # 澄清 ASM-05（2026-10-29）採 A（G19 規則 2，例如第二台裝置的 guest）；合併金幣以一筆「合併入帳」記入帳本

    Example: 攜帶 guest 登入既有帳號時金幣與庫存相加、silver 取其一已解鎖、各榜留最佳
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿明 | MEMBER     | 阿明     |          | false            | false    |
        | 小客 | GUEST      | 勇者248  | dev-0002 | true             | false    |
      And 玩家 "阿明" 有以下第三方身分：
        | loginProvider | providerSubject |
        | GOOGLE        | g-sub-1         |
      And 玩家 "阿明" 的金幣餘額為 300
      And 玩家 "小客" 的金幣餘額為 50
      And 玩家 "阿明" 的 buff 庫存為：
        | buffItem     | quantity |
        | ATTACK_SPEED | 1        |
      And 玩家 "小客" 的 buff 庫存為：
        | buffItem     | quantity |
        | ATTACK_SPEED | 2        |
        | CANNON       | 1        |
      And 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 阿明 | 19.0         | BRONZE | r-01 | false     | 80          | 2026-10-30T10:00:00Z |
        | 小客 | 19.0         | BRONZE | r-02 | true      | 140         | 2026-10-31T10:00:00Z |
        | 阿明 | 19.0         | SILVER | r-03 | true      | 120         | 2026-10-30T11:00:00Z |
        | 小客 | 19.0         | SILVER | r-04 | true      | 130         | 2026-10-31T11:00:00Z |
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  | 小客      |
      Then 操作成功
      And 玩家 "阿明" 應為：
        | nickname | isSilverUnlocked | coins |
        | 阿明     | true             | 350   |
      And 玩家 "阿明" 的 buff 庫存應為：
        | buffItem     | quantity |
        | ATTACK_SPEED | 3        |
        | CANNON       | 1        |
        | LASER        | 0        |
      And 玩家 "阿明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-02 | true      | 140         |
        | 19.0         | SILVER | r-03 | true      | 120         |
      And 玩家 "阿明" 的金幣帳本應依序包含：
        | ledgerSource | coinDelta | coinsAfter |
        | MERGE_IN     | 50        | 350        |

  Rule: 後置（狀態）- 帶有 guest 身分登入合併時，帳號的當日廣告獎勵已領取次數與冷卻不沿用 guest 的紀錄
    # CHANGE 2026-11-02（D33/D34）澄清 AMB-A2＝b：guest 合併進帳號後不沿用 guest 的當日已用次數，帳號保留自己的次數（lock exception：CiC 已預告影響本 feature）
    # 模組決定：次數與冷卻由各自帳本的廣告獎勵（AD_REWARD）筆數與時間計算；guest 的廣告獎勵帳本留在 guest，合併只記一筆合併入帳，金幣照常併入

    Example: 攜帶 guest 登入既有帳號後，帳號廣告獎勵剩餘次數只依帳號自己的領取紀錄
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿明 | MEMBER     | 阿明     |          | false            | false    |
        | 小客 | GUEST      | 勇者248  | dev-0002 | false            | false    |
      And 玩家 "阿明" 有以下第三方身分：
        | loginProvider | providerSubject |
        | GOOGLE        | g-sub-1         |
      And 玩家 "阿明" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 200        | 2026-11-01T11:00:00Z |
      And 玩家 "小客" 有以下金幣帳本：
        | ledgerSource | coinDelta | coinsAfter | occurredAt           |
        | AD_REWARD    | 200       | 200        | 2026-11-01T11:57:00Z |
        | AD_REWARD    | 200       | 400        | 2026-11-01T11:58:00Z |
        | AD_REWARD    | 200       | 600        | 2026-11-01T11:59:30Z |
      And 玩家 "阿明" 的金幣餘額為 200
      And 玩家 "小客" 的金幣餘額為 600
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  | 小客      |
      Then 操作成功
      And 玩家 "阿明" 的金幣餘額應為 800
      And 玩家 "阿明" 的金幣帳本應依序包含：
        | ledgerSource | coinDelta | coinsAfter |
        | MERGE_IN     | 600       | 800        |
        | AD_REWARD    | 200       | 200        |
      And 玩家 "阿明" 的廣告獎勵狀態應為：
        | remainingCount | nextClaimableAt |
        | 4              |                 |

  Rule: 後置（狀態）- 同一 guest 身分只能被合併一次，已合併的 guest 不得再次併入任何帳號
    # 澄清 ASM-05（2026-10-29）採 A（G19 規則 3）
    # 合併前預覽（G19 規則 4）由 查詢合併預覽.feature 承接；登入本身不另帶確認參數，確認在登入頁完成後才呼叫本 feature

    @error:401001
    Example: 已合併作廢的 guest 再次攜帶登入時操作失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 舊客 | GUEST      | 砲手001  | dev-old  | false            | true     |
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  | 舊客      |
      Then 操作失敗，錯誤代碼為 "401001"
      And 系統中帳號（MEMBER）數應為 0

  Rule: 後置（狀態）- 合併後 guest 身分應作廢，其存取權杖之後應被拒
    # 原文 D03 acceptance「作廢的 guest token 被拒」

    @error:401001
    Example: 合併後 guest 身分作廢，其存取權杖之後被拒
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小客 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  | 小客      |
      Then 操作成功
      And 玩家 "小客" 應為：
        | isVoided | mergedInto                         |
        | true     | 第三方身分 GOOGLE "g-sub-1" 的帳號 |
      And 以玩家 "小客" 原有的存取權杖查詢玩家進度應失敗，錯誤代碼為 "401001"

  Rule: 後置（回應）- 應回傳短效存取權杖與可撤銷的換發權杖
    # 原文 D04「短效 access token + 可撤銷 refresh token」
    # 澄清 GAP-07（2026-10-29）採 A：存取權杖有效期 15 分鐘、換發權杖有效期 30 天；兩個值放在後端設定檔

    Example: 回傳 15 分鐘存取權杖與 30 天換發權杖
      When 以 Google 登入：
        | idToken | 攜帶guest |
        | g-ok-1  |           |
      Then 操作成功
      And 回應資料應為：
        | playerKind | accessTokenExpiresIn | refreshTokenExpiresIn |
        | MEMBER     | 900                  | 2592000               |
