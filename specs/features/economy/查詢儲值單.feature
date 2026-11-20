@query
Feature: 查詢儲值單
  會員在回站後輪詢自己的儲值單狀態（D49）；讀取時順便對綠界做 QueryTradeInfo 補償（D47）：ReturnURL 漏接時仍能入帳。
  CHANGE 2026-11-17（D43-D52，documents/ecpaydemands/spec.md §6.3.3、§6.5）。他人的單與不存在的單一律 404001。
  補償規則：CREATED 且建單滿 10 分鐘才查；同一單最少相隔 10 分鐘才再查；查到已付款且金額、會員相符才入帳（與 ReturnURL 共用同一冪等入帳）；
  未成立（10200095）或未付款在建單滿 2 小時後才失敗／逾期；AWAITING_PAYMENT 超過 payment_expire_at 再加 1 天才查一次，已付則入帳否則 EXPIRED。外部回應必須驗章。

  Background:
    Given 現在時間為 "2026-11-17T04:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
      | 小華 | MEMBER     | 小華     |          | false            | false    |
    And 玩家 "阿明" 的金幣餘額為 100
    And 目前以玩家 "阿明" 的有效存取權杖操作

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗
      Given 目前未攜帶存取權杖
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 操作失敗，錯誤代碼為 "401003"

  Rule: 前置（狀態）- 儲值單必須存在且屬於查詢者，否則一律 404001

    @error:404001
    Example: 不存在的儲值單
      When 查詢儲值單 "DW261117035999ZZZZZ9"
      Then 操作失敗，錯誤代碼為 "404001"

    @error:404001
    Example: 他人的儲值單與不存在無法區分
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 小華 | 50        | CREATED | 2026-11-17T03:55:00Z |                 |
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 操作失敗，錯誤代碼為 "404001"

  Rule: 後置（回應）- 回傳狀態、金額、金幣與目前餘額，不回傳失敗原因

    Example: 已入帳的單回傳 PAID、入帳時間與最新餘額
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:55:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 操作成功
      And 回應資料應為：
        | topupOrderId         | packageId | amountTwd | coins | status | walletCoins | paidAt                    |
        | DW261117035001AAAAA1 | TWD_50    | 50        | 0     | PAID   | 100         | 2026-11-17T12:00:00+08:00 |
      And 回應原文不應包含 "failure"

    Example: 取號後的單回傳 AWAITING_PAYMENT 與繳費期限
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:55:00Z |                 |
      When 綠界送出取號通知：
        | 訂單                 | ExpireDate |
        | DW261117035001AAAAA1 | 2026/11/18 |
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status           | paymentExpireAt               | paidAt | walletCoins |
        | AWAITING_PAYMENT | 2026-11-18T23:59:59.999+08:00 | null   | 100         |

  Rule: 後置（狀態）- 查單補償：建單未滿 10 分鐘或距上次查詢未滿 10 分鐘不呼叫綠界

    Example: 建單 5 分鐘內不查單
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:55:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |
      And 綠界查單次數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 同一單 10 分鐘內只查一次，滿 10 分鐘後才再查
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "0"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |
      And 綠界查單次數應為 1
      When 現在時間為 "2026-11-17T04:10:00Z"
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 綠界查單次數應為 2

  Rule: 後置（狀態）- 查到已付款且金額、會員相符時入帳，之後 ReturnURL 才到不重複入帳

    Example: 查單補償入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status | walletCoins |
        | PAID   | 100         |
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件
      And 玩家 "阿明" 的累計贊助額應為 50
      And 儲值單 "DW261117035001AAAAA1" 記錄的綠界交易編號為 "2611171200000001"、付款方式為 "Credit_CreditCard"，通知次數仍為 0

    Example: 查單補償入帳後 ReturnURL 才到，不重複入帳也不重複累加贊助額
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 的累計贊助額應為 50

    Example: 查單回應金額不符時失敗為 AMOUNT_MISMATCH，不入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 1
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "AMOUNT_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 查單回應的會員不符時失敗為 PLAYER_MISMATCH，不入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 50、CustomField1 為玩家 "小華"
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "PLAYER_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "小華" 的金幣餘額應為 0

    Example: 查單回應簽章錯誤時不改單、不入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應簽章錯誤
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 綠界連線失敗時不改單
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 連線失敗
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |
      And 綠界查單次數應為 1

  Rule: 後置（狀態）- 綠界查單回 HTTP 403（查太快）時，同一單 30 分鐘內不再查

    Example: 403 後 10 分鐘仍不查，滿 30 分鐘才再查
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:45:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 HTTP 403
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |
      And 綠界查單次數應為 1
      When 現在時間為 "2026-11-17T04:10:00Z"
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 綠界查單次數應為 1
      When 現在時間為 "2026-11-17T04:30:00Z"
      And 查詢儲值單 "DW261117035001AAAAA1"
      Then 綠界查單次數應為 2

  Rule: 後置（狀態）- 未成立或未付款的單在建單滿 2 小時前維持 CREATED，之後才失敗或逾期

    Example: 未成立（10200095）但建單未滿 2 小時，維持 CREATED
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:00:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "10200095"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | CREATED |

    Example: 未成立（10200095）且建單滿 2 小時，失敗為 NOT_ESTABLISHED
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T01:00:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "10200095"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status |
        | FAILED |
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "NOT_ESTABLISHED"

    Example: 未付款（0）且建單滿 2 小時，逾期為 EXPIRED
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T01:00:00Z |                 |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "0"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | EXPIRED |
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（狀態）- AWAITING_PAYMENT 超過繳費期限再加 1 天才查一次

    Example: 尚未超過期限加 1 天時不查單
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status           | createdAt            | paymentExpireAt      |
        | DW261117035001AAAAA1 | 阿明 | 50        | AWAITING_PAYMENT | 2026-11-16T03:00:00Z | 2026-11-16T15:59:59Z |
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status           |
        | AWAITING_PAYMENT |
      And 綠界查單次數應為 0

    Example: 超過期限加 1 天且綠界回未付款，逾期為 EXPIRED
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status           | createdAt            | paymentExpireAt      |
        | DW261117035001AAAAA1 | 阿明 | 50        | AWAITING_PAYMENT | 2026-11-10T03:00:00Z | 2026-11-11T15:59:59Z |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "0"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status  |
        | EXPIRED |
      And 綠界查單次數應為 1

    Example: 超過期限加 1 天但綠界回已付款，入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status           | createdAt            | paymentExpireAt      |
        | DW261117035001AAAAA1 | 阿明 | 50        | AWAITING_PAYMENT | 2026-11-10T03:00:00Z | 2026-11-11T15:59:59Z |
      And 綠界查單 "DW261117035001AAAAA1" 回應 TradeStatus 為 "1"、TradeAmt 為 50
      When 查詢儲值單 "DW261117035001AAAAA1"
      Then 回應資料應為：
        | status | walletCoins |
        | PAID   | 100         |
      And 玩家 "阿明" 的累計贊助額應為 50
