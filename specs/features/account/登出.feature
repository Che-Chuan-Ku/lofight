@command
Feature: 登出
  登出使 refresh token 失效（D04）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿明 | MEMBER     | 阿明     |          | false            | false    |
    And 玩家 "阿明" 有以下換發權杖：
      | token | expiresAt            | revokedAt |
      | rt-ok | 2026-11-30T00:00:00Z |           |
    And 目前以玩家 "阿明" 的有效存取權杖操作

  Rule: 前置（參數）- 換發權杖必須提供

    @error:400001
    Example: 未提供換發權杖時操作失敗
      When 以換發權杖登出：
        | refreshToken |
        |              |
      Then 操作失敗，錯誤代碼為 "400001"
      And 換發權杖 "rt-ok" 應未撤銷

  Rule: 後置（狀態）- 該換發權杖應被撤銷，之後不可再用於換發
    # 原文 D04「登出使 refresh 失效」；acceptance「登出後舊 token 失效」
    # 澄清 GAP-15（2026-10-29）採 A：登出後前端丟棄存取權杖與換發權杖，產生新的 device id 並呼叫建立guest身分取得新 guest（金幣、進度從 0 起），回首頁；之後再以 Google/Apple 登入可取回原帳號，新 guest 內容依合併規則併入
    # 澄清 AMB-02（2026-10-29）採 A 衍生：換發權杖會輪替，登出撤銷的是前端當下持有的最新換發權杖
    # 推論（使用者未裁決）：後端登出只撤銷換發權杖，不另撤銷存取權杖（15 分鐘內自然過期）

    @error:401001
    Example: 登出後換發權杖被撤銷，之後不可再用於換發
      When 以換發權杖登出：
        | refreshToken |
        | rt-ok        |
      Then 操作成功
      And 換發權杖 "rt-ok" 應已撤銷，撤銷時間為 "2026-11-01T12:00:00Z"
      And 以換發權杖 "rt-ok" 再次換發應失敗，錯誤代碼為 "401001"
