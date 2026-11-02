@command
Feature: 建立guest身分
  玩家首次開啟遊戲、不登入即可完整遊玩；系統發放裝置層級的 guest 身分（D03）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統預設暱稱產生器下一個值為 "勇者248"

  Rule: 前置（參數）- 裝置識別必須提供
    # 澄清 GAP-10（2026-10-29）採 A：裝置識別為前端產生的隨機 device id，存在前端本機，每次呼叫皆帶入
    # 澄清 GAP-15（2026-10-29）採 A：登出後前端產生新的 device id 再呼叫本 feature，取得全新 guest
    # 推論（使用者未裁決）：device id 對應的 guest 已因合併作廢時，後端拒絕並回 acc 錯誤碼（不復活、不重建）；前端於登入合併成功時即丟棄原 device id，收到此錯誤碼時改產生新 device id 重試

    @error:400001
    Example: 未提供裝置識別時操作失敗
      When 以裝置識別建立guest身分：
        | deviceId |
        |          |
      Then 操作失敗，錯誤代碼為 "400001"

    @error:acc003
    Example: 裝置識別對應已合併作廢的 guest 時操作失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 舊客 | GUEST      | 砲手001  | dev-old  | false            | true     |
      When 以裝置識別建立guest身分：
        | deviceId |
        | dev-old  |
      Then 操作失敗，錯誤代碼為 "acc003"
      And 系統中裝置識別 "dev-old" 對應的玩家數應為 1

  Rule: 後置（狀態）- 系統應建立一個 guest 身分並給予系統預設暱稱
    # 澄清 GAP-10（2026-10-29）採 A：同一個 device id 永遠對應同一個 guest；該 device id 尚無 guest 時才建立，已有則沿用既有 guest（不重建、暱稱與錢包不變）
    # 推論（使用者未裁決）：guest 資料表需保存 device id（唯一）；清除瀏覽器資料後 device id 遺失即無法找回原 guest
    # 原文 D05「首次進入給系統預設暱稱（如 勇者248），使用者可改」

    Example: 新裝置識別建立 guest 並給予系統預設暱稱
      When 以裝置識別建立guest身分：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 裝置識別 "dev-0001" 對應的玩家應為：
        | playerKind | nickname | isSilverUnlocked | isVoided |
        | GUEST      | 勇者248  | false            | false    |

    Example: 已存在的裝置識別沿用原 guest，不重建且暱稱與錢包不變
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小明 | GUEST      | 小明     | dev-0001 | true             | false    |
      And 玩家 "小明" 的金幣餘額為 150
      When 以裝置識別建立guest身分：
        | deviceId |
        | dev-0001 |
      Then 操作成功
      And 裝置識別 "dev-0001" 對應的玩家應為：
        | playerKind | nickname | isSilverUnlocked | isVoided | coins |
        | GUEST      | 小明     | true             | false    | 150   |
      And 系統中裝置識別 "dev-0001" 對應的玩家數應為 1

  Rule: 後置（狀態）- guest 應擁有一個餘額為 0 的金幣錢包
    # 原文 D08「金幣綁帳號（guest 綁裝置）」

    Example: 新建 guest 擁有餘額為 0 的金幣錢包
      When 以裝置識別建立guest身分：
        | deviceId |
        | dev-0002 |
      Then 操作成功
      And 裝置識別 "dev-0002" 對應的玩家應為：
        | playerKind | coins |
        | GUEST      | 0     |

  Rule: 後置（回應）- 應回傳 guest 的存取權杖
    # 原文 D03「系統發放 guest 身分（裝置層級 token）」
    # 澄清 GAP-10（2026-10-29）採 A：guest 存取權杖有效期 15 分鐘（後端設定檔）；過期後以同一 device id 再呼叫本 feature 重新取得，guest 不發換發權杖

    Example: 回傳 15 分鐘的 guest 存取權杖且不發換發權杖
      When 以裝置識別建立guest身分：
        | deviceId |
        | dev-0003 |
      Then 操作成功
      And 回應資料應為：
        | playerKind | accessTokenExpiresIn | hasRefreshToken |
        | GUEST      | 900                  | false           |
      And 回應中的存取權杖應可識別為裝置識別 "dev-0003" 對應的玩家
