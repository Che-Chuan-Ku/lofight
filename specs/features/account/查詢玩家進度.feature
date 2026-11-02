@query
Feature: 查詢玩家進度
  玩家選階層前讀取自己的 silver 解鎖狀態與暱稱；解鎖狀態記在帳號（guest 記裝置），重登仍有效（D14）。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
    And 玩家 "小明" 有以下 run：
      | run  | tier   | equippedBuff | boardVersion | startedAt            | runState | bossesKilled |
      | r-01 | BRONZE |              | 19.0         | 2026-10-31T10:00:00Z | SETTLED  | 3            |
      | r-02 | BRONZE |              | 19.0         | 2026-10-31T11:00:00Z | SETTLED  | 5            |
      | r-03 | BRONZE |              | 19.0         | 2026-10-31T12:00:00Z | OPEN     |              |

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢玩家進度
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 後置（回應）- 應回傳玩家暱稱與身分類型（guest 或已登入）

    Scenario Outline: 回傳玩家暱稱與身分類型
      Given 目前以玩家 "<玩家>" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作成功
      And 回應資料應為：
        | nickname   | playerKind   |
        | <nickname> | <playerKind> |

      Examples:
        | 玩家 | nickname | playerKind |
        | 小明 | 勇者248  | GUEST      |
        | 阿明 | 阿明     | MEMBER     |

  Rule: 後置（回應）- 應回傳 silver 是否已解鎖
    # 原文 D14 acceptance「解鎖後重登仍有效」

    Scenario Outline: 回傳 silver 是否已解鎖
      Given 目前以玩家 "<玩家>" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作成功
      And 回應資料應為：
        | isSilverUnlocked   |
        | <isSilverUnlocked> |

      Examples:
        | 玩家 | isSilverUnlocked |
        | 小明 | false            |
        | 阿明 | true             |

  Rule: 後置（回應）- silver 未解鎖時應回傳解鎖條件與目前 bronze 最佳擊破王數
    # 依據 G20；澄清 ASM-07（2026-10-29）採 A：silver 上鎖可見，顯示解鎖條件與目前 bronze 最佳王數（取代 D14 原文「看不到入口」）

    Example: silver 未解鎖時回傳解鎖條件與目前 bronze 最佳擊破王數（只計已結算的 run）
      Given 目前以玩家 "小明" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作成功
      And 回應資料應為：
        | isSilverUnlocked | silverUnlockBossSeq | bronzeBestBossesKilled |
        | false            | 8                   | 5                      |

    Example: silver 已解鎖時不回傳解鎖條件
      Given 目前以玩家 "阿明" 的有效存取權杖操作
      When 查詢玩家進度
      Then 操作成功
      And 回應資料應為：
        | isSilverUnlocked | silverUnlockBossSeq | bronzeBestBossesKilled |
        | true             | null                | null                   |
