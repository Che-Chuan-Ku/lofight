@command
Feature: 修改暱稱
  排行榜名稱由使用者自填或系統預設，不取用第三方回傳的姓名（D05）。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 阿明 | MEMBER     | 砲台大師 |          | false            | false    |
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

  Rule: 後置（狀態）- 玩家暱稱應更新為新值且允許與他人重名
    # 依據 G02「暱稱只是顯示欄位，允許重名」；排行榜去重鍵為帳號（澄清 ASM-06 採 A）

    Scenario Outline: 暱稱更新為新值（含 12 字上限與他人重名）
      When 修改暱稱：
        | nickname   |
        | <nickname> |
      Then 操作成功
      And 玩家 "小明" 應為：
        | nickname   |
        | <nickname> |

      Examples:
        | nickname                 | 說明         |
        | 小明                     | 一般值       |
        | 一二三四五六七八九十一二 | 12 字邊界    |
        | 砲台大師                 | 與阿明重名   |
