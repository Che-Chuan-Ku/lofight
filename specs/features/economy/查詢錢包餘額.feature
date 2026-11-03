@query
Feature: 查詢錢包餘額
  金幣綁帳號（guest 綁裝置）（D08）。
  CHANGE 2026-11-03（D38）：回應另附最近一次賽季獎勵，供首頁一次性提示（澄清 AMB-S5＝a）。

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

  Rule: 後置（回應）- 應附最近一次賽季獎勵的賽季與各榜名次、金幣，從未得獎時為空
    # 澄清 AMB-S5＝a（2026-11-03）：首頁一次性提示「上賽季 NORMAL 第 N 名，獲得 X 金幣」，資料取自本回應；前端以賽季為本機旗標鍵、同一賽季只提示一次（lock exception：change 報告 Δ-Flow-2 已預告 +1 回應 Rule）
    # 推論 INF-S13：最近一次＝本人賽季獎勵帳本中賽季（大, 中）數值最大者；兩榜皆得時依 NORMAL（BRONZE）→ EXTRA（SILVER）列出；guest 恆為空

    Example: 從未獲得賽季獎勵時最近一次賽季獎勵為空
      Given 目前以玩家 "小明" 的有效存取權杖操作
      When 查詢錢包餘額
      Then 操作成功
      And 回應的最近一次賽季獎勵應為空

    Example: 回傳賽季最新的一次賽季獎勵，兩榜皆得時各列一筆
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿凱 | MEMBER     | 阿凱     |          | true             | false    |
      And 玩家 "阿凱" 有以下金幣帳本：
        | ledgerSource  | coinDelta | coinsAfter | seasonBoardVersion | seasonTier | seasonRank | occurredAt           |
        | SEASON_REWARD | 800       | 800        | 19.1               | BRONZE     | 2          | 2026-12-01T00:00:00Z |
        | SEASON_REWARD | 400       | 1200       | 19.2               | BRONZE     | 3          | 2027-01-05T00:00:00Z |
        | SEASON_REWARD | 1200      | 2400       | 19.2               | SILVER     | 1          | 2027-01-05T00:00:00Z |
      And 玩家 "阿凱" 的金幣餘額為 2400
      And 目前以玩家 "阿凱" 的有效存取權杖操作
      When 查詢錢包餘額
      Then 操作成功
      And 回應資料應為：
        | coins |
        | 2400  |
      And 回應的最近一次賽季獎勵應為：
        | boardVersion | tier   | rank | coins |
        | 19.2         | BRONZE | 3    | 400   |
        | 19.2         | SILVER | 1    | 1200  |
