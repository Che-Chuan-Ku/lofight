@command
Feature: 結算對局
  一局結束（勝或敗）後玩家提交結算；後端校驗後依擊破王入帳金幣、記錄成績，首次通關 bronze 第 8 王時解鎖 silver（D06、D08、D14、D17、D21、D22）。
  # 澄清 ASM-04（2026-10-29）：D06「API：提交成績」併入本 feature，結算即上榜、成績綁 run_id，不另立提交成績 feature 與 endpoint

  # 澄清 ASM-02（2026-10-29）：結算以 run_id 為鍵、每個 run 只能結算一次；金幣僅於結算時入帳，未結算的 run 不入帳

  Background:
    Given 現在時間為 "2026-11-01T12:10:00Z"
    And 後端設定的目前版本前兩碼為 "19.0"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
    And 玩家 "小明" 的金幣餘額為 10
    And 玩家 "小明" 有以下 run：
      | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
      | r-b1 | BRONZE |              | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
    And 目前以玩家 "小明" 的有效存取權杖操作
    # bossKills 記法：bossSeq@killedAtSecond 以逗號分隔；"[]" 表示提供了空清單；空白儲存格表示未提供

  Rule: 前置（參數）- run_id、遊戲秒數、擊破王序號清單、勝負必須提供
    # 原文 D21「記錄…秒數、擊破王數、勝負」

    @error:400001
    Scenario Outline: 必要參數缺漏時操作失敗且 run 維持未結算
      When 結算 run "r-b1"：
        | gameSeconds   | isVictory   | bossKills   | deathCause | deathBossSeq |
        | <gameSeconds> | <isVictory> | <bossKills> | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "400001"
      And run "r-b1" 應為：
        | runState |
        | OPEN     |

      Examples:
        | gameSeconds | isVictory | bossKills |
        |             | false     | 1@14      |
        | 30          |           | 1@14      |
        | 30          | false     |           |

    @error:404001
    Example: run 不存在時操作失敗
      When 結算 run "r-none"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "404001"

  Rule: 前置（參數）- 敗局必須提供死因
    # 原文 D22「失敗畫面含死因」；澄清 ASM-10（2026-10-29）採 C：前端將死因回報後端存入 run（可做統計）

    @error:400001
    Example: 敗局未提供死因時操作失敗
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      |            |              |
      Then 操作失敗，錯誤代碼為 "400001"

  Rule: 前置（狀態）- run 必須屬於提交者，且狀態不得為「檢查失敗」或「已放棄」
    # 原文 D21 acceptance「結算只能一次」；澄清 ASM-02（2026-10-29）
    # 澄清 BDY-02（2026-10-29）採 A：已標記「檢查失敗」的 run 視同已結束，不能再結算（回 gam 錯誤碼；重送仍回同一錯誤碼）
    # 澄清 BDY-06（2026-10-29）採 B：run 狀態四種 未結算／已結算／檢查失敗／已放棄；「已結算」的 run 重送不視為錯誤，改走下方冪等回應 Rule

    @error:403002
    Example: 結算他人的 run 時操作失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小華 | GUEST      | 新兵777  | dev-0002 | false            | false    |
      And 玩家 "小華" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-h1 | BRONZE |              | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      When 結算 run "r-h1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "403002"
      And run "r-h1" 應為：
        | runState |
        | OPEN     |

    @error:gam003
    Example: 已標記檢查失敗的 run 重送結算仍回本局成績無效
      Given 玩家 "小明" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState     |
        | r-cf | BRONZE |              | 19.0         | 2026-11-01T12:00:00Z | CHECK_FAILED |
      When 結算 run "r-cf"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And 玩家 "小明" 的金幣餘額應為 10

    @error:gam004
    Example: 已放棄的 run 結算時操作失敗
      Given 玩家 "小明" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState  |
        | r-ab | BRONZE |              | 19.0         | 2026-10-30T12:00:00Z | ABANDONED |
      When 結算 run "r-ab"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam004"
      And 玩家 "小明" 的金幣餘額應為 10

  Rule: 前置（狀態）- 未結算的 run 自開局起必須未超過放棄逾時時間
    # 澄清 BDY-06（2026-10-29）採 B：未結算的 run 超過逾時時間標記「已放棄」，不入帳、不上榜，結算回 gam 錯誤碼
    # 推論（使用者未裁決數值）：逾時採推薦值 24 小時、後端設定檔可調；於結算時判定並標記，不另立排程 feature；已放棄 run 不退還開局扣的 buff 庫存（D20）

    @error:gam004
    Example: 開局超過 24 小時才結算時標記為已放棄且不入帳
      Given 玩家 "小明" 有以下 run：
        | run   | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-old | BRONZE |              | 19.0         | 2026-10-31T12:09:59Z | OPEN     |
      When 結算 run "r-old"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam004"
      And run "r-old" 應為：
        | runState  |
        | ABANDONED |
      And 玩家 "小明" 的金幣餘額應為 10

    Example: 開局恰好 24 小時結算仍可入帳
      Given 玩家 "小明" 有以下 run：
        | run    | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-edge | BRONZE |              | 19.0         | 2026-10-31T12:10:00Z | OPEN     |
      When 結算 run "r-edge"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 30          | false     | 1@14      | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的金幣餘額應為 11

  Rule: 前置（參數）- 擊破第 k 王的遊戲秒數必須不早於該王出場秒數
    # 原文 D17「成績由後端做合理性校驗（秒數範圍、關卡順序）」；澄清 ASM-03（2026-10-29）採 A
    # 澄清 BDY-02（2026-10-29）採 A（適用本 Rule 及以下各條檢查 Rule，含審核修正新增的擊破王清單三條）：任一檢查未通過即整筆拒絕並回 gam 錯誤碼；run 標記為「檢查失敗」、不得再結算；金幣不入帳、成績不上榜
    # bronze 第 1–7 王出場秒數 14/28/42/56/70/84/98（BOSS_PLAN；第 8 王在休息選門後出場）
    # 澄清 GAP-03（2026-10-29）採 A：silver 第 1–6 王出場秒數門檻沿用 bronze 前六王 14/28/42/56/70/84；門檻表依階層（以 run 上的階層判定）放在後端設定檔

    @error:gam003
    Example: 擊破第 2 王的秒數早於其出場秒數 28 時整筆拒絕並標記檢查失敗
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 40          | false     | 1@14,2@27 | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-b1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "小明" 的金幣餘額應為 10
      And 玩家 "小明" 的排行榜成績應為空

  Rule: 前置（參數）- 勝利時遊戲秒數必須大於等於該階層的勝利秒數門檻（bronze 98、silver 84）
    # 依據 G05；澄清 ASM-03（2026-10-29）採 A（bronze 門檻 98）
    # 澄清 GAP-03（2026-10-29）採 A：silver 勝利秒數必須大於 84（第 6 王出場秒數）；門檻值放在後端設定檔
    # 2026-11-02 D-09 裁決改為 ≥（含等於）：第 6 王進場秒即被擊破時 floor(t)=84 的勝局應被接受

    @error:gam003
    Example: bronze 勝局秒數小於 98 時整筆拒絕
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills                                 | deathCause | deathBossSeq |
        | 97          | true      | 1@14,2@28,3@42,4@56,5@70,6@84,7@98,8@98   |            |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-b1" 應為：
        | runState     |
        | CHECK_FAILED |

    @error:gam003
    Example: silver 勝局秒數小於 84 時整筆拒絕
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-s1 | SILVER | LASER        | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-s1"：
        | gameSeconds | isVictory | bossKills                     | deathCause | deathBossSeq |
        | 83          | true      | 1@14,2@28,3@42,4@56,5@70,6@84 |            |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-s1" 應為：
        | runState     |
        | CHECK_FAILED |

    Example: silver 勝局秒數恰為 84 且 1–6 王皆不早於進場秒時接受
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 的金幣餘額為 0
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-s1 | SILVER | LASER        | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-s1"：
        | gameSeconds | isVictory | bossKills                     | deathCause | deathBossSeq |
        | 84          | true      | 1@14,2@28,3@42,4@56,5@70,6@84 |            |              |
      Then 操作成功
      And run "r-s1" 應為：
        | runState |
        | SETTLED  |
      And 玩家 "阿銀" 的金幣餘額應為 252
      And 玩家 "阿銀" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | SILVER | r-s1 | true      | 84          |

  Rule: 前置（狀態）- 伺服器經過時間（結算時間 − 開局時間）必須不小於回報的遊戲秒數
    # 依據 G05；澄清 ASM-03（2026-10-29）採 A；開局時間取自開局.feature 建立的 run
    # 澄清 AMB-01（2026-10-29）採 A：中途重新開始沿用同一 run、開局時間不重設；回報的遊戲秒數為最後一次重新開始後的局內秒數，本檢查照常適用

    @error:gam003
    Example: 伺服器經過 60 秒卻回報 61 秒時整筆拒絕
      Given 玩家 "小明" 有以下 run：
        | run    | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-fast | BRONZE |              | 19.0         | 2026-11-01T12:09:00Z | OPEN     |
      When 結算 run "r-fast"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 61          | false     | 1@14      | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-fast" 應為：
        | runState     |
        | CHECK_FAILED |

  Rule: 前置（參數）- 擊破王序號必須在該階層範圍內（bronze 1–8、silver 1–6）
    # 原文 D17「成績由後端做合理性校驗（秒數範圍、關卡順序）」
    # 審核修正（2026-10-30，人裁決）：序號越界統一視為合理性檢查未通過，回 gam003、run 標記「檢查失敗」、不入帳、不上榜（同 BDY-02=A），不回 400007

    @error:gam003
    Scenario Outline: bronze 擊破王序號超出 1–8 時整筆拒絕並標記檢查失敗
      When 結算 run "r-b1"：
        | gameSeconds   | isVictory | bossKills   | deathCause | deathBossSeq |
        | <gameSeconds> | false     | <bossKills> | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-b1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "小明" 的金幣餘額應為 10
      And 玩家 "小明" 的排行榜成績應為空

      Examples:
        | gameSeconds | bossKills  |
        | 30          | 0@14       |
        | 130         | 1@14,9@120 |

    @error:gam003
    Example: silver 擊破王序號 7 超出 1–6 時整筆拒絕並標記檢查失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 的金幣餘額為 0
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-s1 | SILVER | LASER        | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-s1"：
        | gameSeconds | isVictory | bossKills  | deathCause | deathBossSeq |
        | 110         | false     | 1@14,7@100 | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-s1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "阿銀" 的金幣餘額應為 0
      And 玩家 "阿銀" 的排行榜成績應為空

  Rule: 前置（參數）- 同一擊破王序號在清單中不得重複出現
    # 審核修正（2026-10-30，人裁決）：重複序號視為合理性檢查未通過，回 gam003、run 標記「檢查失敗」、不入帳、不上榜
    # 後端須於寫入逐王紀錄前自行檢查重複，不得依賴 run_boss_kills 唯一索引（uk_run_boss_kills_run_id_boss_seq）的 DB 錯誤

    @error:gam003
    Example: 第 1 王在清單中出現兩次時整筆拒絕並標記檢查失敗
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills      | deathCause | deathBossSeq |
        | 40          | false     | 1@14,2@28,1@30 | RED_BREACH |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-b1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "小明" 的金幣餘額應為 10
      And 玩家 "小明" 的金幣帳本筆數應為 0
      And 玩家 "小明" 的排行榜成績應為空

  Rule: 前置（參數）- 勝局必須擊破該階層全部王（bronze 8 王、silver 6 王）
    # 原文 D17「關卡順序」校驗；審核修正（2026-10-30，人裁決）：勝局擊破王數不足視為合理性檢查未通過，回 gam003、run 標記「檢查失敗」、不入帳、不上榜
    # 敗局不受本 Rule 限制（擊破王數可為 0 至全部）

    @error:gam003
    Example: bronze 勝局只擊破 7 王時整筆拒絕並標記檢查失敗
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills                          | deathCause | deathBossSeq |
        | 130         | true      | 1@14,2@28,3@42,4@56,5@70,6@84,7@98 |            |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-b1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "小明" 的金幣餘額應為 10
      And 玩家 "小明" 的排行榜成績應為空

    @error:gam003
    Example: silver 勝局只擊破 5 王時整筆拒絕並標記檢查失敗
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 的金幣餘額為 0
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-s1 | SILVER | LASER        | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-s1"：
        | gameSeconds | isVictory | bossKills                | deathCause | deathBossSeq |
        | 100         | true      | 1@14,2@28,3@42,4@56,5@70 |            |              |
      Then 操作失敗，錯誤代碼為 "gam003"
      And run "r-s1" 應為：
        | runState     |
        | CHECK_FAILED |
      And 玩家 "阿銀" 的金幣餘額應為 0
      And 玩家 "阿銀" 的排行榜成績應為空

  Rule: 後置（狀態）- 本局每擊破一王應依其出場序號對應的階層金幣入帳一筆（勝負皆適用）
    # 原文 D08「bronze 八關依序 1,2,4,8,16,24,48,96；silver 六關依序 4,8,16,32,64,128」（clarify 裁決，合計 bronze 199、silver 252）
    # 澄清 ASM-01（2026-10-29）：「過第 N 關」＝本局擊破第 N 王，以魔王出場序號計（非擊殺順序）；勝敗皆依已擊破的王逐王入帳
    # 澄清 ASM-03（2026-10-29）：金幣一律由後端依擊破王序號自算，不接受前端回報金幣

    Example: bronze 敗局依出場序號逐王入帳（出場秒數邊界即可通過）
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills      | deathCause | deathBossSeq |
        | 50          | false     | 1@14,2@28,3@42 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的金幣餘額應為 17
      And 玩家 "小明" 的金幣帳本應依序包含：
        | ledgerSource | bossSeq | coinDelta | coinsAfter |
        | BOSS_REWARD  | 3       | 4         | 17         |
        | BOSS_REWARD  | 2       | 2         | 13         |
        | BOSS_REWARD  | 1       | 1         | 11         |

    Example: silver 局依 silver 金幣表逐王入帳
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-s1 | SILVER | LASER        | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-s1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 40          | false     | 1@14,2@28 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "阿銀" 的金幣餘額應為 12
      And 玩家 "阿銀" 的金幣帳本應依序包含：
        | ledgerSource | bossSeq | coinDelta | coinsAfter |
        | BOSS_REWARD  | 2       | 8         | 12         |
        | BOSS_REWARD  | 1       | 4         | 4          |

  Rule: 後置（狀態）- 重玩同一階層再次擊破同一王應再次入帳
    # 原文 D08「每次通關都給金幣（可重複刷）」

    Example: 重玩同一階層再次擊破第 1 王再次入帳
      Given 玩家 "小明" 有以下金幣帳本：
        | ledgerSource | bossSeq | coinDelta | coinsAfter | occurredAt           |
        | BOSS_REWARD  | 1       | 1         | 10         | 2026-11-01T11:00:00Z |
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 20          | false     | 1@14      | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的金幣帳本應依序包含：
        | ledgerSource | bossSeq | coinDelta | coinsAfter |
        | BOSS_REWARD  | 1       | 1         | 11         |
        | BOSS_REWARD  | 1       | 1         | 10         |

  Rule: 後置（狀態）- run 應記錄秒數、擊破王數、勝負、死因（敗局）與入帳金幣並標記為已結算
    # 原文 D21；死因欄位依澄清 ASM-10（2026-10-29）採 C
    # 澄清 BDY-02（2026-10-29）採 A：僅檢查全數通過時套用本 Rule；檢查未通過時 run 改標記為「檢查失敗」
    # 澄清 BDY-06（2026-10-29）採 B：run 狀態共四種 未結算／已結算／檢查失敗／已放棄

    Example: run 記錄秒數、擊破王數、勝負、死因與入帳金幣並標記為已結算
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause   | deathBossSeq |
        | 45          | false     | 1@14,2@28 | BOSS_REACHED | 3            |
      Then 操作成功
      And run "r-b1" 應為：
        | runState | gameSeconds | bossesKilled | isVictory | deathCause   | deathBossSeq | coinsAwarded | settledAt            |
        | SETTLED  | 45          | 2            | false     | BOSS_REACHED | 3            | 3            | 2026-11-01T12:10:00Z |

  Rule: 後置（狀態）- 成績應以該階層排行榜規則保存（同帳號留最佳）並關聯本局 run_id
    # 原文 D06「同暱稱/同帳號留最佳」；澄清 ASM-06（2026-10-29）採 A：分榜鍵為 (版本前兩碼, 階層, 帳號)，guest 以 guest id 為帳號
    # 澄清 GAP-16（2026-10-29）採 B：版本前兩碼取自本局 run（開局時由後端設定檔寫入），不以結算當下的設定檔版本為準
    # 澄清 ASM-04（2026-10-29）：結算即上榜、成績綁 run_id；澄清 BDY-02（2026-10-29）採 A：檢查未通過的 run 不保存成績、不上榜
    # 澄清 GAP-12（2026-10-29）採 A：最佳＝勝局在前、勝局秒數少者在前、敗局存活秒數多者在前；新成績未嚴格優於既有最佳時保留既有成績（同分先結算者留榜）；成績保存勝敗、秒數、結算時間

    Example: 該榜尚無成績時以 run 上的版本前兩碼保存成績
      Given 後端設定的目前版本前兩碼為 "19.1"
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 45          | false     | 1@14,2@28 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-b1 | false     | 45          |

    Example: 新成績優於既有最佳時取代
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 小明 | 19.0         | BRONZE | r-b0 | false     | 30          | 2026-11-01T11:00:00Z |
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 45          | false     | 1@14,2@28 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-b1 | false     | 45          |

    Example: 既有勝局最佳優於新的敗局時保留既有成績
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 小明 | 19.0         | BRONZE | r-b0 | true      | 150         | 2026-11-01T11:00:00Z |
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 45          | false     | 1@14,2@28 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-b0 | true      | 150         |

    Example: 新成績與既有最佳同分時保留先結算的既有成績
      Given 系統中有以下排行榜成績：
        | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
        | 小明 | 19.0         | BRONZE | r-b0 | false     | 45          | 2026-11-01T11:00:00Z |
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 45          | false     | 1@14,2@28 | RED_BREACH |              |
      Then 操作成功
      And 玩家 "小明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-b0 | false     | 45          |

  Rule: 後置（狀態）- 首次擊破 bronze 第 8 王時玩家應永久解鎖 silver
    # 原文 D14

    Example: 首次擊破 bronze 第 8 王時永久解鎖 silver
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills                                | deathCause | deathBossSeq |
        | 130         | true      | 1@14,2@28,3@42,4@56,5@70,6@84,7@98,8@120 |            |              |
      Then 操作成功
      And 玩家 "小明" 應為：
        | isSilverUnlocked | coins |
        | true             | 209   |

  Rule: 後置（回應）- 應回傳本局逐王入帳金幣明細與結算後錢包餘額
    # 原文 D22「本局金幣」；澄清 ASM-10（2026-10-29）採 C（回應內容同選項 A）

    Example: 回傳本局逐王入帳金幣明細與結算後錢包餘額
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills      | deathCause | deathBossSeq |
        | 50          | false     | 1@14,2@28,3@42 | RED_BREACH |              |
      Then 操作成功
      And 回應資料應為：
        | runState | coinsAwarded | walletCoins |
        | SETTLED  | 7            | 17          |
      And 回應中的 "bossRewards" 應為：
        | bossSeq | coins |
        | 1       | 1     |
        | 2       | 2     |
        | 3       | 4     |

  Rule: 後置（回應）- 應回傳本局是否新解鎖 silver
    # 原文 D14、D22；澄清 ASM-10（2026-10-29）採 C：死因由前端回報並存入 run、不回傳；下一個目標（G09）由前端依既有資料計算，後端不回傳

    Example: 本局首次通關 bronze 時回傳新解鎖 silver
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills                                | deathCause | deathBossSeq |
        | 130         | true      | 1@14,2@28,3@42,4@56,5@70,6@84,7@98,8@120 |            |              |
      Then 操作成功
      And 回應資料應為：
        | isSilverNewlyUnlocked |
        | true                  |

    Example: 已解鎖 silver 的玩家再次通關時不回傳新解鎖
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
      And 玩家 "阿銀" 有以下 run：
        | run  | tier   | equippedBuff | boardVersion | startedAt            | runState |
        | r-b2 | BRONZE |              | 19.0         | 2026-11-01T12:00:00Z | OPEN     |
      And 目前以玩家 "阿銀" 的有效存取權杖操作
      When 結算 run "r-b2"：
        | gameSeconds | isVictory | bossKills                                | deathCause | deathBossSeq |
        | 130         | true      | 1@14,2@28,3@42,4@56,5@70,6@84,7@98,8@120 |            |              |
      Then 操作成功
      And 回應資料應為：
        | isSilverNewlyUnlocked |
        | false                 |

  Rule: 後置（回應）- 已結算的 run 重送結算時應回傳第一次的結算結果，且不重複入帳、不重複上榜
    # 澄清 BDY-06（2026-10-29）採 B：重送結算冪等（以 run_id 為鍵），帳本、成績、silver 解鎖皆不變動

    Example: 已結算的 run 重送時回傳第一次結果，不重複入帳也不重複上榜
      Given 玩家 "小明" 已結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills      | deathCause | deathBossSeq |
        | 50          | false     | 1@14,2@28,3@42 | RED_BREACH |              |
      When 結算 run "r-b1"：
        | gameSeconds | isVictory | bossKills | deathCause | deathBossSeq |
        | 20          | false     | 1@14      | RED_BREACH |              |
      Then 操作成功
      And 回應資料應為：
        | runState | coinsAwarded | walletCoins | isSilverNewlyUnlocked |
        | SETTLED  | 7            | 17          | false                 |
      And 玩家 "小明" 的金幣餘額應為 17
      And 玩家 "小明" 的金幣帳本筆數應為 3
      And 玩家 "小明" 的排行榜成績應為：
        | boardVersion | tier   | run  | isVictory | gameSeconds |
        | 19.0         | BRONZE | r-b1 | false     | 50          |
