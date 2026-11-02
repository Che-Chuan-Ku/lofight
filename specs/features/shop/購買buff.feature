@command
Feature: 購買buff
  以金幣購買 buff；同種可買多份；餘額不足拒絕（D09、D13）。

  Background:
    Given 現在時間為 "2026-11-01T12:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿銀 | GUEST      | 老兵512  | dev-0003 | true             | false    |
    And 玩家 "阿銀" 的金幣餘額為 500
    And 玩家 "阿銀" 的 buff 庫存為：
      | buffItem | quantity |
      | CANNON   | 1        |
    And 目前以玩家 "阿銀" 的有效存取權杖操作

  Rule: 前置（參數）- buff 品項必須存在
    # 原文 D10–D12 三種 buff

    @error:404001
    Example: buff 品項不存在時操作失敗
      When 購買 buff：
        | buffItem |
        | SHIELD   |
      Then 操作失敗，錯誤代碼為 "404001"
      And 玩家 "阿銀" 的金幣餘額應為 500

  Rule: 前置（狀態）- 玩家必須已解鎖 silver
    # 原文 D09「bronze 玩家可看不可用」；澄清 ASM-07（2026-10-29）採 A：未解鎖不能購買 buff

    @error:shp002
    Example: 未解鎖 silver 的玩家購買時操作失敗且不記事件
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      And 玩家 "小明" 的金幣餘額為 500
      And 目前以玩家 "小明" 的有效存取權杖操作
      When 購買 buff：
        | buffItem     |
        | ATTACK_SPEED |
      Then 操作失敗，錯誤代碼為 "shp002"
      And 玩家 "小明" 的金幣餘額應為 500
      And 事件數應為 0

  Rule: 前置（狀態）- 玩家金幣餘額必須不少於品項價格
    # 原文 D09 acceptance「餘額不足拒絕」
    # 澄清 GAP-02（2026-10-29）採 A：品項價格取自後端設定檔（攻速 200、砲台 400、雷射 600），以購買當下設定值判定
    # 澄清 BDY-07（2026-10-29）採 A：同一玩家的購買依序判斷（條件式扣款：餘額 ≥ 價格才扣），併發時餘額不足那筆回 shp 錯誤碼；Phase 03 補併發 Example（餘額 500 同時買兩個砲台 → 一筆成功、一筆餘額不足）

    @error:shp001
    Example: 餘額少於品項價格時操作失敗，不扣款、不記帳本與事件
      When 購買 buff：
        | buffItem |
        | LASER    |
      Then 操作失敗，錯誤代碼為 "shp001"
      And 玩家 "阿銀" 的金幣餘額應為 500
      And 玩家 "阿銀" 的金幣帳本筆數應為 0
      And 事件數應為 0

    @error:shp001
    Example: 餘額 500 同時購買兩個砲台時一筆成功、一筆餘額不足
      When 同時送出以下購買：
        | buffItem |
        | CANNON   |
        | CANNON   |
      Then 應有 1 筆成功、1 筆失敗，失敗錯誤代碼為 "shp001"
      And 玩家 "阿銀" 的金幣餘額應為 100
      And 玩家 "阿銀" 的 buff 庫存應為：
        | buffItem | quantity |
        | CANNON   | 2        |

  Rule: 後置（狀態）- 玩家金幣餘額應扣除品項價格並記入帳本
    # 原文 D09 acceptance「購買扣金幣」；D08「帳本可查」
    # 澄清 GAP-02（2026-10-29）推論：扣款金額與帳本、購買事件記錄的價格皆為購買當下設定檔價格（日後調價不回溯）
    # 澄清 BDY-07（2026-10-29）採 A：扣款、記帳本、buff 庫存 +1、記購買事件在同一交易內完成，任一失敗整筆回滾；同一玩家的購買依序處理（鎖餘額列或條件式扣款），不會超扣

    Scenario Outline: 扣除購買當下價格並記入帳本（餘額恰等於價格可購買）
      Given 玩家 "阿銀" 的金幣餘額為 <coins>
      When 購買 buff：
        | buffItem   |
        | <buffItem> |
      Then 操作成功
      And 玩家 "阿銀" 的金幣餘額應為 <coinsAfter>
      And 玩家 "阿銀" 的金幣帳本應依序包含：
        | ledgerSource  | buffItem   | coinDelta | coinsAfter   |
        | BUFF_PURCHASE | <buffItem> | <delta>   | <coinsAfter> |

      Examples:
        | coins | buffItem     | delta | coinsAfter |
        | 500   | ATTACK_SPEED | -200  | 300        |
        | 400   | CANNON       | -400  | 0          |
        | 600   | LASER        | -600  | 0          |

  Rule: 後置（狀態）- 該 buff 庫存數量應加 1
    # 原文 D13「可購買多份（同種可囤）」

    Example: 該 buff 庫存數量加 1
      When 購買 buff：
        | buffItem |
        | CANNON   |
      Then 操作成功
      And 玩家 "阿銀" 的 buff 庫存應為：
        | buffItem | quantity |
        | CANNON   | 2        |
      And 回應資料應為：
        | walletCoins | buffItem | quantity |
        | 100         | CANNON   | 2        |

  Rule: 後置（狀態）- 應記錄一筆購買事件
    # 原文 D21「另記錄商店瀏覽、購買事件；事件可查」；澄清 ASM-09（2026-10-29）採 B：購買成功時寫一筆事件（玩家、buff 品項、價格、發生時間），供 查詢事件統計.feature 彙總；被前置 Rule 拒絕的購買不記事件

    Example: 購買成功記錄一筆購買事件（含購買當下價格）
      When 購買 buff：
        | buffItem     |
        | ATTACK_SPEED |
      Then 操作成功
      And 應記錄以下事件：
        | eventKind     | 玩家 | isSilverUnlocked | buffItem     | paidCoins | occurredAt           |
        | BUFF_PURCHASE | 阿銀 | true             | ATTACK_SPEED | 200       | 2026-11-01T12:00:00Z |
