@command
Feature: 換發存取權杖
  access token 過期後以 refresh token 換發（D04，assumed）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿明 | MEMBER     | 阿明     |          | false            | false    |
    And 玩家 "阿明" 有以下換發權杖：
      | token      | expiresAt            | revokedAt            |
      | rt-ok      | 2026-11-30T00:00:00Z |                      |
      | rt-revoked | 2026-11-30T00:00:00Z | 2026-10-31T00:00:00Z |
      | rt-expired | 2026-11-01T11:59:59Z |                      |

  Rule: 前置（參數）- 換發權杖必須提供

    @error:400001
    Example: 未提供換發權杖時操作失敗
      When 以換發權杖換發存取權杖：
        | refreshToken |
        |              |
      Then 操作失敗，錯誤代碼為 "400001"

  Rule: 前置（狀態）- 換發權杖必須未被撤銷且未過期
    # 原文 D04 acceptance「登出後舊 token 失效」
    # 澄清 GAP-07（2026-10-29）採 A：換發權杖有效期 30 天（自核發時起算），值放在後端設定檔

    Scenario Outline: 換發權杖已撤銷、已過期或不存在時操作失敗
      When 以換發權杖換發存取權杖：
        | refreshToken   |
        | <refreshToken> |
      Then 操作失敗，錯誤代碼為 "<code>"

      @error:401001
      Examples: 已撤銷或不認得
        | refreshToken | code   |
        | rt-revoked   | 401001 |
        | rt-unknown   | 401001 |

      @error:401002
      Examples: 已過期
        | refreshToken | code   |
        | rt-expired   | 401002 |

  Rule: 後置（回應）- 應回傳新的短效存取權杖
    # 原文 D04 acceptance「過期後自動換發」
    # 澄清 GAP-07（2026-10-29）採 A：新存取權杖有效期 15 分鐘，值放在後端設定檔
    # 澄清 AMB-02（2026-10-29）採 A：同時回傳新的換發權杖（效期 30 天自本次核發重新起算）

    Example: 回傳新的 15 分鐘存取權杖與 30 天新換發權杖
      When 以換發權杖換發存取權杖：
        | refreshToken |
        | rt-ok        |
      Then 操作成功
      And 回應資料應為：
        | accessTokenExpiresIn | refreshTokenExpiresIn |
        | 900                  | 2592000               |
      And 回應中的存取權杖應可識別為玩家 "阿明"

  Rule: 後置（狀態）- 舊換發權杖應立即作廢，新換發權杖取代之
    # 澄清 AMB-02（2026-10-29）採 A：每次換發輪替換發權杖，舊權杖作廢後再用即被「未被撤銷」前置 Rule 拒絕
    # Scope Lock 例外：此 Rule 為 CiC 選項 A 事先宣告之內容，使用者裁決採納，非新增行為
    # 推論（使用者未裁決）：作廢舊權杖與核發新權杖同一交易；換發權杖資料表需記錄撤銷時間（可選記取代者 id），不做重用偵測連坐撤銷

    @error:401001
    Example: 換發後舊換發權杖立即作廢，新換發權杖可用
      When 以換發權杖換發存取權杖：
        | refreshToken |
        | rt-ok        |
      Then 操作成功
      And 換發權杖 "rt-ok" 應已撤銷，撤銷時間為 "2026-11-01T12:00:00Z"
      And 回應中的新換發權杖到期時間應為 "2026-12-01T12:00:00Z"
      And 以換發權杖 "rt-ok" 再次換發應失敗，錯誤代碼為 "401001"
