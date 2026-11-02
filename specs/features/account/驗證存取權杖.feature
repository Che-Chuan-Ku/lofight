@cross
Feature: 驗證存取權杖
  橫切關注點：除建立guest身分、Google登入、Apple登入、換發存取權杖外，所有玩家操作需帶有效存取權杖（D03/D04）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 舊客 | GUEST      | 砲手001  | dev-old  | false            | true     |
    # 以「查詢玩家進度」為受保護操作的代表載體；所有受保護 endpoint 共用同一檢查

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時被拒
      Given 目前未攜帶存取權杖
      When 查詢玩家進度
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 前置（狀態）- 存取權杖必須未過期
    # 澄清 GAP-07（2026-10-29）採 A：Google/Apple 帳號的存取權杖有效期 15 分鐘（後端設定檔）
    # 澄清 GAP-10（2026-10-29）採 A：guest 存取權杖有效期同為 15 分鐘（後端設定檔）；過期後以 device id 呼叫建立guest身分重新取得，不走換發存取權杖

    @error:401002
    Example: 存取權杖已過期時被拒
      Given 目前以玩家 "小明" 已過期的存取權杖操作
      When 查詢玩家進度
      Then 操作失敗，錯誤代碼為 "401002"

    Example: 存取權杖未過期時放行
      Given 目前以玩家 "小明" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作成功

  Rule: 前置（狀態）- 已作廢 guest 身分的存取權杖必須被拒
    # 原文 D03 acceptance「作廢的 guest token 被拒」

    @error:401001
    Example: 已作廢 guest 的存取權杖被拒
      Given 目前以玩家 "舊客" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作失敗，錯誤代碼為 "401001"
