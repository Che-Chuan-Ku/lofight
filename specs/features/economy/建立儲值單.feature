@command
Feature: 建立儲值單
  會員在商店「贊助支持」選定預設金額方案後建立儲值單（贊助單，不給金幣）；後端依方案代碼決定金額，回傳送往綠界 AIO 的簽章欄位（D45）。
  CHANGE 2026-11-17（D43-D52，documents/ecpaydemands/spec.md §6.3.2）。只有會員可儲值（DEC-1）；請求只收方案代碼，不收金額與金幣；CHANGE 2026-11-20（綠界審查）：贊助不給金幣，coins 恆為 0；被拒不寫任何資料。
  # 檢查順序：存取權杖（401003）→ 方案代碼（400001／400012）→ 已開放（eco004）→ 會員（eco005）→ 開單上限（eco006）→ 建單

  Background:
    Given 現在時間為 "2026-11-17T04:00:00Z"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 小明 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
    And 玩家 "阿明" 的金幣餘額為 100
    And 目前以玩家 "阿明" 的有效存取權杖操作

  Rule: 前置（參數）- 存取權杖必須提供

    @error:401003
    Example: 未攜帶存取權杖時操作失敗且不建單
      Given 目前未攜帶存取權杖
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作失敗，錯誤代碼為 "401003"
      And 儲值單總數應為 0

  Rule: 前置（參數）- 方案代碼必須提供且為預設方案之一

    @error:400001
    Example: 未提供方案代碼時操作失敗且不建單
      When 建立儲值單：
        | packageId |
        |           |
      Then 操作失敗，錯誤代碼為 "400001"
      And 儲值單總數應為 0

    @error:400012
    Example: 方案代碼不在預設清單時操作失敗且不建單
      When 建立儲值單：
        | packageId |
        | TWD_77    |
      Then 操作失敗，錯誤代碼為 "400012"
      And 儲值單總數應為 0

  Rule: 前置（狀態）- 儲值功能必須已開放（已設定綠界特店編號）

    @error:eco004
    Example: 未設定特店編號時操作失敗且不建單
      Given 綠界特店編號未設定
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作失敗，錯誤代碼為 "eco004"
      And 儲值單總數應為 0

    @error:eco004
    Example: 實況主收款連結模式下 AIO 建單關閉且不建單
      Given 綠界實況主收款連結為 "https://p.ecpay.com.tw/43C219A"
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作失敗，錯誤代碼為 "eco004"
      And 儲值單總數應為 0

  Rule: 前置（狀態）- 只有會員可儲值

    @error:eco005
    Example: guest 建立儲值單時操作失敗且不建單
      Given 目前以玩家 "小明" 的有效存取權杖操作
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作失敗，錯誤代碼為 "eco005"
      And 儲值單總數應為 0

  Rule: 前置（狀態）- 30 分鐘內未完成（CREATED、AWAITING_PAYMENT）的儲值單不得達 3 筆
    # 以 DB 計數，不新增依賴；已完成或已失敗的單不計

    @error:eco006
    Example: 已有 3 筆未完成儲值單時操作失敗且不再建單
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status           | createdAt            | paymentExpireAt      |
        | DW261117035001AAAAA1 | 阿明 | 10        | CREATED          | 2026-11-17T03:50:00Z |                      |
        | DW261117035002AAAAA2 | 阿明 | 10        | CREATED          | 2026-11-17T03:55:00Z |                      |
        | DW261117035003AAAAA3 | 阿明 | 10        | AWAITING_PAYMENT | 2026-11-17T03:59:00Z | 2026-11-18T15:59:59Z |
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作失敗，錯誤代碼為 "eco006"
      And 儲值單總數應為 3

    Example: 3 筆單中有已付款或超過 30 分鐘者不計入上限
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035001AAAAA1 | 阿明 | 10        | CREATED | 2026-11-17T03:00:00Z |                 |
        | DW261117035002AAAAA2 | 阿明 | 10        | PAID    | 2026-11-17T03:55:00Z |                 |
        | DW261117035003AAAAA3 | 阿明 | 10        | FAILED  | 2026-11-17T03:59:00Z |                 |
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作成功
      And 儲值單總數應為 4

  Rule: 後置（回應）- 回傳伺服器依方案決定的金額（金幣恆為 0），以及簽章正確的綠界結帳欄位

    Scenario Outline: 四個預設方案的金額固定、金幣皆為 0
      When 建立儲值單：
        | packageId   |
        | <packageId> |
      Then 操作成功
      And 回應資料應為：
        | packageId   | amountTwd   | coins   | status  |
        | <packageId> | <amountTwd> | <coins> | CREATED |
      And 回應的結帳欄位 "TotalAmount" 應為 "<amountTwd>"

      Examples:
        | packageId | amountTwd | coins |
        | TWD_10    | 10        | 0     |
        | TWD_50    | 50        | 0     |
        | TWD_120   | 120       | 0     |
        | TWD_200   | 200       | 0     |

    Example: 結帳欄位依規格組成並以官方演算法簽章
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作成功
      And 回應的結帳網址應為 "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5"
      And 回應的結帳欄位簽章應正確
      And 回應的結帳欄位 "MerchantID" 應為 "3002607"
      And 回應的結帳欄位 "MerchantTradeNo" 應以 "DW261117120000" 開頭
      And 回應的結帳欄位 "MerchantTradeDate" 應為 "2026/11/17 12:00:00"
      And 回應的結帳欄位 "PaymentType" 應為 "aio"
      And 回應的結帳欄位 "ItemName" 應為 "DayWar 贊助 NT$50"
      And 回應的結帳欄位 "ChoosePayment" 應為 "ALL"
      And 回應的結帳欄位 "IgnorePayment" 應為 "Credit#ApplePay#CVS#BARCODE#BNPL#WeiXin"
      And 回應的結帳欄位 "EncryptType" 應為 "1"
      And 回應的結帳欄位 "CustomField2" 應為 "TWD_50"
      And 回應的結帳欄位 "ReturnURL" 應為 "https://daywar.net/api/csg/v1/payments/ecpay/return"
      And 回應的結帳欄位 "OrderResultURL" 應為 "https://daywar.net/api/csg/v1/payments/ecpay/order-result"
      And 回應的結帳欄位 "PaymentInfoURL" 應為 "https://daywar.net/api/csg/v1/payments/ecpay/payment-info"
      And 回應的結帳欄位 "ClientBackURL" 應以 "https://daywar.net/?topup=DW" 開頭
      And 回應的結帳欄位 "ExpireDate" 應為 "1"
      And 回應的結帳欄位不應包含 "HashKey"
      And 回應的結帳欄位不應包含 "HashIV"
      And 回應原文不應包含金鑰

    Example: 正式環境結帳網址指向綠界正式站
      Given 綠界環境為 "prod"
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作成功
      And 回應的結帳網址應為 "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5"

  Rule: 後置（狀態）- 只寫入一筆 CREATED 儲值單，不入帳、不累計贊助額

    Example: 建單成功只建立儲值單，餘額與累計贊助額不變
      When 建立儲值單：
        | packageId |
        | TWD_50    |
      Then 操作成功
      And 儲值單總數應為 1
      And 儲值單 "最新" 的狀態應為 "CREATED"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單

    Example: 請求夾帶的金額與金幣被忽略，儲值單仍為方案值
      When 建立儲值單：
        | packageId | amountTwd | coins  |
        | TWD_50    | 1         | 999999 |
      Then 操作成功
      And 回應資料應為：
        | packageId | amountTwd | coins | status  |
        | TWD_50    | 50        | 0     | CREATED |
      And 回應的結帳欄位 "TotalAmount" 應為 "50"
