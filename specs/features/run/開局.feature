@command
Feature: 開局
  玩家選定階層（silver 時可裝填 0 或 1 個 buff）後開始一局，系統建立 run（D07、D13、D20、D21）。
  # 澄清 ASM-02（2026-10-29）：開局向後端取得 run_id 與伺服器開局時間；成績與金幣都綁 run_id；每個 run_id 只能結算一次；未結算的 run 不入帳
  # 澄清 AMB-01（2026-10-29）採 A：局內中途重新開始沿用同一 run_id、不再呼叫開局、不再扣庫存（D20「同局視為同一次裝填」）；run 不需 parent_run 欄位
  # 澄清 BDY-05（2026-10-29）採 A：裝填庫存為 0 時拒絕開局（gam 錯誤碼），不建立 run、不扣庫存
  # 澄清 BDY-03（2026-10-29）採 A：局末「再來一局」一律重新呼叫開局建立新 run；silver 須經裝填畫面確認，後端不提供「沿用上次 loadout」，本 feature 不新增 Rule

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 後端設定的目前版本前兩碼為 "19.0"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | true             | false    |
    And 玩家 "小明" 的 buff 庫存為：
      | buffItem | quantity |
      | LASER    | 1        |
      | CANNON   | 0        |
      | ATTACK   | 1        |
    And 目前以玩家 "小明" 的有效存取權杖操作

  Rule: 前置（參數）- 階層必須為 bronze 或 silver
    # 原文 D07「原八關成為 bronze 階層…新增 silver 階層共六關」；CHANGE 2026-11-03（D39，人裁決 H1）：silver 改為共 7 王（開局 Rule 不變，王數影響見 結算對局.feature）

    Scenario Outline: 階層缺漏或不是 bronze／silver 時操作失敗
      When 開局：
        | tier   | buffItems |
        | <tier> |           |
      Then 操作失敗，錯誤代碼為 "<code>"
      And 玩家 "小明" 的 run 數應為 0

      @error:400001
      Examples: 缺漏
        | tier | code   |
        |      | 400001 |

      @error:400012
      Examples: 不在允許清單
        | tier | code   |
        | GOLD | 400012 |

  Rule: 前置（狀態）- 開 silver 局時玩家必須已解鎖 silver
    # 原文 D14「通關 bronze 第八關一次後永久解鎖 silver」
    # CHANGE 2026-11-04（D42）澄清 AMB-N2：解鎖條件改為 NORMAL 通關＝首次擊破 bronze 第 9 王

    @error:gam001
    Example: 未解鎖 silver 的玩家開 silver 局時操作失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小華 | GUEST      | 新兵777  | dev-0002 | false            | false    |
      And 目前以玩家 "小華" 的有效存取權杖操作
      When 開局：
        | tier   | buffItems |
        | SILVER |           |
      Then 操作失敗，錯誤代碼為 "gam001"
      And 玩家 "小華" 的 run 數應為 0

  Rule: 前置（參數）- bronze 局必須不裝填 buff
    # 原文 D13「bronze 不可裝」；D07「bronze 局內無商店入口與 buff 效果」

    @error:400013
    Example: bronze 局裝填 buff 時操作失敗且庫存不變
      When 開局：
        | tier   | buffItems |
        | BRONZE | LASER     |
      Then 操作失敗，錯誤代碼為 "400013"
      And 玩家 "小明" 的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 1        |

  Rule: 前置（參數）- silver 局裝填的 buff 必須最多 1 個
    # 原文 D13「一局只能裝一個」

    @error:400007
    Example: silver 局裝填 2 個 buff 時操作失敗
      When 開局：
        | tier   | buffItems    |
        | SILVER | LASER,CANNON |
      Then 操作失敗，錯誤代碼為 "400007"
      And 玩家 "小明" 的 run 數應為 0

  Rule: 前置（狀態）- 裝填的 buff 庫存數量必須大於 0
    # 澄清 BDY-05（2026-10-29）採 A：庫存為 0（含併發兩局搶扣同一份）時拒絕開局並回 gam 錯誤碼；不建立 run、不扣庫存
    # 扣庫存以條件式更新（庫存 > 0 才扣）確保併發時不會扣成負數；條件更新未命中即視同庫存為 0

    @error:gam002
    Example: 裝填的 buff 庫存為 0 時拒絕開局，不建立 run、不扣庫存
      When 開局：
        | tier   | buffItems |
        | SILVER | CANNON    |
      Then 操作失敗，錯誤代碼為 "gam002"
      And 玩家 "小明" 的 run 數應為 0
      And 玩家 "小明" 的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 1        |
        | CANNON   | 0        |

    @error:gam002
    Example: 最後 1 份庫存被兩局同時裝填時只有一局開局成功
      When 同時送出以下開局：
        | tier   | buffItems |
        | SILVER | LASER     |
        | SILVER | LASER     |
      Then 應有 1 筆成功、1 筆失敗，失敗錯誤代碼為 "gam002"
      And 玩家 "小明" 的 run 數應為 1
      And 玩家 "小明" 的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 0        |

  Rule: 後置（狀態）- silver 局裝填 buff 時該 buff 庫存應減 1
    # 原文 D13 acceptance「裝填後庫存 −1」；D20「裝備時扣庫存並記錄本局 loadout」

    Example: silver 局裝填 buff 時該 buff 庫存減 1
      When 開局：
        | tier   | buffItems |
        | SILVER | LASER     |
      Then 操作成功
      And 玩家 "小明" 的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 0        |

  Rule: 後置（狀態）- 未裝填 buff 時庫存應不變
    # 原文 D13 acceptance「未裝不扣」

    Scenario Outline: 未裝填 buff 時庫存不變
      When 開局：
        | tier   | buffItems |
        | <tier> |           |
      Then 操作成功
      And 玩家 "小明" 的 buff 庫存應為：
        | buffItem | quantity |
        | LASER    | 1        |
        | CANNON   | 0        |

      Examples:
        | tier   |
        | BRONZE |
        | SILVER |

  Rule: 後置（狀態）- 系統應建立一筆 run，記錄階層、裝填的 buff 與目前版本前兩碼
    # 澄清 GAP-16（2026-10-29）採 B：版本前兩碼取自後端設定檔的目前版本，於開局時寫入 run；前端開局不帶版本；改版時更新設定檔即換榜
    # CHANGE 2026-11-03（D38）澄清 AMB-S6＝b：目前版本前兩碼改由後端建置版號（pom.xml version）自動取「大.中」，不再手填設定檔；測試仍以 `Given 後端設定的目前版本前兩碼為` 注入
    # 原文 D21「開局取得 run_id；記錄 tier、裝備 buff」

    Scenario Outline: 建立 run 記錄階層、裝填的 buff 與目前版本前兩碼
      When 開局：
        | tier   | buffItems   |
        | <tier> | <buffItems> |
      Then 操作成功
      And 回應 run 應為：
        | tier   | equippedBuff   | boardVersion | startedAt            | runState |
        | <tier> | <equippedBuff> | 19.0         | 2026-11-01T12:00:00Z | OPEN     |

      Examples:
        | tier   | buffItems | equippedBuff |
        | BRONZE |           |              |
        | SILVER | LASER     | LASER        |
        | SILVER | ATTACK    | ATTACK       |

  Rule: 後置（回應）- 應回傳 run_id 與伺服器開局時間
    # 依據 G05 第 1 點；澄清 ASM-02（2026-10-29）採 A

    Example: 回傳 run_id 與伺服器開局時間
      When 開局：
        | tier   | buffItems |
        | BRONZE |           |
      Then 操作成功
      And 回應資料應為：
        | runId    | startedAt            |
        | (非空白) | 2026-11-01T12:00:00Z |
