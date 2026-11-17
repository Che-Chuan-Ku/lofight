@command
Feature: 修改暱稱
  排行榜名稱由使用者自填或系統預設，不取用第三方回傳的姓名（D05）。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 阿明 | MEMBER     | 砲台大師 |          | false            | false    |
      | 阿英 | MEMBER     | Alice    |          | false            | false    |
      | 路人 | GUEST      | 勇者999  | dev-0002 | false            | false    |
      | 舊號 | GUEST      | 舊暱稱   | dev-0003 | false            | true     |
    And 禁用字清單為 "禁字"
    And 目前以玩家 "小明" 的有效存取權杖操作

  Rule: 前置（參數）- 暱稱必須不是空白
    # 原文 D05「空白不接受」

    @error:400001
    Scenario Outline: 暱稱為空或僅含空白時操作失敗
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作失敗，錯誤代碼為 "400001"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

      Examples:
        | nickname |
        |          |
        | "   "    |

  Rule: 前置（參數）- 暱稱長度必須不超過 12 字
    # 原文 D05「12 字上限」

    @error:400008
    Example: 暱稱 13 字超過上限時操作失敗
      When 修改暱稱：
        | nickname                   |
        | 一二三四五六七八九十一二三 |
      Then 操作失敗，錯誤代碼為 "400008"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

  Rule: 前置（參數）- 暱稱必須不含禁用字
    # 澄清 ASM-11 採 A：後端放禁用字清單檔，暱稱含清單中任一禁用字即拒絕並回 acc 模組錯誤碼（design-guidelines G29）

    @error:acc002
    Example: 暱稱含禁用字時操作失敗
      When 修改暱稱：
        | nickname   |
        | 我是禁字王 |
      Then 操作失敗，錯誤代碼為 "acc002"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

  Rule: 前置（狀態）- 暱稱不可與其他未作廢玩家（會員或訪客）重複
    # CHANGE 2026-11-17（使用者追加）：為了讓實況主贊助的暱稱配對可靠；去頭尾空白後比對、英文字母不分大小寫；只檢核玩家主動改名（系統預設暱稱與合併流程不檢核）
    # 取代舊裁決 G02「允許重名」；既有資料的重名照舊存在、不處理

    @error:acc005
    Scenario Outline: 改成他人暱稱時操作失敗且暱稱不變
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作失敗，錯誤代碼為 "acc005"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

      Examples:
        | nickname   | 說明                   |
        | 砲台大師   | 與會員重名             |
        | 勇者999    | 與訪客重名             |
        | alice      | 英文大小寫不同也算重名 |
        | "  Alice " | 頭尾空白去除後重名     |

  Rule: 前置（參數）- 暱稱不可含控制、格式、特殊空白字元；全形與半形視為相同
    # CHANGE 2026-11-17（安全審查）：拒收 Unicode Cc／Cf（含零寬字元、BOM）、Zl／Zp、以及一般空白（U+0020）以外的 Zs（含 NBSP、全形空白），回 acc002；
    # 唯一檢核與贊助配對共用同一個比對規則：NFKC → 去頭尾空白 → 小寫（全形「Ａｌｉｃｅ」與「Alice」撞名）

    @error:acc002
    Scenario Outline: 暱稱含零寬、BOM、特殊空白字元時操作失敗
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作失敗，錯誤代碼為 "acc002"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

      Examples:
        | nickname                | 說明         |
        | 小{ZWSP}明              | 零寬空白     |
        | 小{ZWNJ}明              | 零寬不連字   |
        | 小{ZWJ}明               | 零寬連字     |
        | {BOM}小明               | BOM 開頭     |
        | 小{NBSP}明              | 不換行空白   |
        | 小{IDEOGRAPHIC_SPACE}明 | 中間全形空白 |

    @error:acc005
    Scenario Outline: 全形與半形、大小寫視為同名，撞名時操作失敗
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作失敗，錯誤代碼為 "acc005"
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

      Examples:
        | nickname | 說明                 |
        | Ａｌｉｃｅ | 全形與 Alice 撞名    |
        | ＡＬＩＣＥ | 全形大寫與 Alice 撞名 |

  Rule: 後置（狀態）- 玩家暱稱應更新為新值（去頭尾空白）；改成自己目前的暱稱或作廢玩家的暱稱允許

    Scenario Outline: 暱稱更新為新值（含 12 字上限與頭尾空白）
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作成功
      And 玩家 "小明" 應為：
        | nickname   |
        | <expected> |

      Examples:
        | nickname                 | expected                 | 說明           |
        | 小明                     | 小明                     | 一般值         |
        | 一二三四五六七八九十一二 | 一二三四五六七八九十一二 | 12 字邊界      |
        | "  新名字 "              | 新名字                   | 去頭尾空白     |

    Example: 改成自己目前的暱稱視為允許
      When 修改暱稱：
        | nickname |
        | 勇者248  |
      Then 操作成功
      And 玩家 "小明" 應為：
        | nickname |
        | 勇者248  |

    Example: 只改自己暱稱的英文大小寫視為允許
      Given 目前以玩家 "阿英" 的有效存取權杖操作
      When 修改暱稱：
        | nickname |
        | ALICE    |
      Then 操作成功
      And 玩家 "阿英" 應為：
        | nickname |
        | ALICE    |

    Example: 改成作廢玩家的暱稱視為允許
      When 修改暱稱：
        | nickname |
        | 舊暱稱   |
      Then 操作成功
      And 玩家 "小明" 應為：
        | nickname |
        | 舊暱稱   |

  Rule: 後置（狀態）- 兩位玩家同時改成同一個暱稱時只有一位成功

    Example: 同時改成同名只有一人成功
      When 玩家 "小明" 與 "阿明" 同時把暱稱改為 "搶名字"
      Then 兩人之中恰有 1 人改名成功、1 人收到錯誤代碼 "acc005"
