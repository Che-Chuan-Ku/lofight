@query
Feature: 查詢錢包餘額
  金幣綁帳號（guest 綁裝置）（D08）。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
    And 玩家 "小明" 的金幣餘額為 199

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢錢包餘額
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 後置（回應）- 應回傳玩家目前金幣餘額

    Example: 回傳玩家目前金幣餘額
      Given 目前以玩家 "小明" 的有效存取權杖操作
      When 查詢錢包餘額
      Then 操作成功
      And 回應資料應為：
        | coins |
        | 199   |
