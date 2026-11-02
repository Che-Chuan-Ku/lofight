@query
Feature: 查詢buff庫存
  D09 acceptance「庫存可查」；silver 裝填畫面列出庫存 buff 與數量（D20）。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
    And 玩家 "阿銀" 的 buff 庫存為：
      | buffItem | quantity |
      | CANNON   | 2        |
      | LASER    | 0        |

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢 buff 庫存
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 後置（回應）- 應回傳每種 buff 的持有數量

    Example: 回傳每種 buff 的持有數量（未持有者為 0）
      Given 目前以玩家 "阿銀" 的有效存取權杖操作
      When 查詢 buff 庫存
      Then 操作成功
      And 回應中的 "items" 應為：
        | buffItem     | quantity |
        | ATTACK_SPEED | 0        |
        | CANNON       | 2        |
        | LASER        | 0        |
