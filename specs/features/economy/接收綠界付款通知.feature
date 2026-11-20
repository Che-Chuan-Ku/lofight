@command
Feature: 接收綠界付款通知
  綠界以 server POST 通知付款結果（ReturnURL）與 ATM 取號結果（PaymentInfoURL）；只有驗過 CheckMacValue、特店編號、金額、會員且 RtnCode=1 的 ReturnURL 通知才入帳（D46、D47）。
  CHANGE 2026-11-17（D43-D52，documents/ecpaydemands/spec.md §4.4、§6.3.6、§6.5）。入帳＝事件 TOPUP＋累計贊助額加訂單金額＋訂單 PAID，同一交易；贊助不給金幣（CHANGE 2026-11-20，綠界審查：topup.coins-per-twd = 0），錢包餘額與金幣帳本不變；
  冪等靠訂單列鎖＋狀態判斷＋帳本 payment_order_id 唯一索引。回應必為純文字：成功與已處理一律 1|OK，驗章／特店／訂單不存在才回 0|…（綠界會重送）。
  綠界通知不帶 token；測試用綠界公開 stage 測試特店 3002607 與其公開測試 HashKey／HashIV（https://developers.ecpay.com.tw/2856/）。
  # 贊助等級（D48、D51）：累計贊助額 > 0 為第 1 級；≥ 200（設定檔 topup.sponsor-tier2-min-twd，使用者 2026-11-17 裁決）為第 2 級；只在入帳時累加，重送不重複。

  Background:
    Given 現在時間為 "2026-11-17T04:00:00Z"
    And 後端設定的目前版本前兩碼為 "19.0"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
      | 小華 | MEMBER     | 小華     |          | false            | false    |
    And 玩家 "阿明" 的金幣餘額為 100
    And 系統中有以下排行榜成績：
      | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
      | 阿明 | 19.0         | BRONZE | r-a1 | true      | 120         | 2026-11-01T10:02:00Z |
    And 系統中有以下儲值單：
      | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
      | DW261117035001AAAAA1 | 阿明 | 50        | CREATED | 2026-11-17T03:55:00Z |                 |
    And 目前以玩家 "阿明" 的有效存取權杖操作

  Rule: 前置（驗證）- CheckMacValue 必須正確（以收到的全部欄位重算），否則不寫任何資料

    Example: 簽章錯誤時回 0|CheckMacValueError 且不入帳、不改單
      When 綠界送出付款通知：
        | 訂單                 | 簽章 |
        | DW261117035001AAAAA1 | 錯誤 |
      Then 綠界收到的回應應為 "0|CheckMacValueError"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"
      And 儲值單 "DW261117035001AAAAA1" 的通知次數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 事件數應為 0

    Example: 簽章後金額被竄改時簽章不符，不入帳
      When 綠界送出付款通知：
        | 訂單                 | 竄改        |
        | DW261117035001AAAAA1 | TradeAmt=1  |
      Then 綠界收到的回應應為 "0|CheckMacValueError"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 特店編號不符時回 0|MerchantIdError 且不入帳
      When 綠界送出付款通知：
        | 訂單                 | MerchantID |
        | DW261117035001AAAAA1 | 9999999    |
      Then 綠界收到的回應應為 "0|MerchantIdError"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 訂單不存在時回 0|OrderNotFound 且不入帳
      When 綠界送出付款通知：
        | 訂單                 | MerchantTradeNo      |
        | DW261117035001AAAAA1 | DW261117035999ZZZZZ9 |
      Then 綠界收到的回應應為 "0|OrderNotFound"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（狀態）- 驗證通過且 RtnCode=1 時入帳：事件 TOPUP、累計贊助額加訂單金額、訂單 PAID；餘額不變、無 TOPUP 帳本（贊助不給金幣）

    Example: 付款成功通知入帳並回 1|OK
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件
      And 玩家 "阿明" 的累計贊助額應為 50
      And 玩家 "阿明" 的首次贊助時間應為 "2026-11-17T04:00:00Z"
      And 儲值單 "DW261117035001AAAAA1" 的通知次數應為 1

    Example: 入帳金額來自儲值單而非通知內容，記錄的通知內容不含簽章與金鑰
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 儲值單 "DW261117035001AAAAA1" 記錄的通知內容不應包含簽章與金鑰

    Example: 第二筆成功入帳時首次贊助時間不變
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035002AAAAA2 | 阿明 | 30        | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      And 現在時間為 "2026-11-17T05:00:00Z"
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035002AAAAA2 |
      Then 玩家 "阿明" 的累計贊助額應為 80
      And 玩家 "阿明" 的首次贊助時間應為 "2026-11-17T04:00:00Z"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（冪等）- 同一筆付款通知重送或並發送出，只入帳一次、累計贊助額只加一次

    Example: 同一通知送 3 次只入帳一次
      When 綠界重送 3 次相同的付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件
      And 玩家 "阿明" 的累計贊助額應為 50
      And 儲值單 "DW261117035001AAAAA1" 的通知次數應為 3

    Example: 同一通知送 5 次只入帳一次
      When 綠界重送 5 次相同的付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 50
      And 儲值單 "DW261117035001AAAAA1" 的通知次數應為 5

    Example: 同時送出 2 次相同通知只入帳一次
      When 綠界同時送出 2 次相同的付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 所有綠界同時送出的回應應為 "1|OK"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 的累計贊助額應為 50

    Example: 同時送出 5 次相同通知只入帳一次
      When 綠界同時送出 5 次相同的付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 所有綠界同時送出的回應應為 "1|OK"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件
      And 玩家 "阿明" 的累計贊助額應為 50

  Rule: 後置（狀態）- 通知內容與儲值單不符、非成功或不可信時不入帳，仍回 1|OK 讓綠界停止重送

    Example: 金額不符時訂單失敗為 AMOUNT_MISMATCH，不入帳
      When 綠界送出付款通知：
        | 訂單                 | TradeAmt |
        | DW261117035001AAAAA1 | 1        |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "FAILED"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "AMOUNT_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: CustomField1 不是儲值單會員時訂單失敗為 PLAYER_MISMATCH，不入帳
      When 綠界送出付款通知：
        | 訂單                 | CustomField1 |
        | DW261117035001AAAAA1 | 玩家:小華    |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "PLAYER_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "小華" 的金幣餘額應為 0
      And 玩家 "小華" 的累計贊助額應為 0

    Example: 儲值單的特店編號與通知不同時失敗為 MERCHANT_MISMATCH，不入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt | merchantId |
        | DW261117035002AAAAA2 | 阿明 | 30        | CREATED | 2026-11-17T03:56:00Z |                 | 1111111    |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035002AAAAA2 |
      Then 儲值單 "DW261117035002AAAAA2" 的失敗原因應為 "MERCHANT_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: RtnCode 不是 1 時訂單失敗為 RTN_CODE_x，不入帳
      When 綠界送出付款通知：
        | 訂單                 | RtnCode |
        | DW261117035001AAAAA1 | 10100058 |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "FAILED"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "RTN_CODE_10100058"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 因 RtnCode 失敗的單之後收到合法成功通知仍入帳
      When 綠界送出付款通知：
        | 訂單                 | RtnCode |
        | DW261117035001AAAAA1 | 0       |
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 50

    Example: 金額不符的單需人工處理，之後的合法通知不會改成 PAID
      When 綠界送出付款通知：
        | 訂單                 | TradeAmt |
        | DW261117035001AAAAA1 | 1        |
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "FAILED"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "AMOUNT_MISMATCH"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 已 EXPIRED 的單之後收到合法成功通知仍入帳
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035002AAAAA2 | 阿明 | 30        | EXPIRED | 2026-11-16T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035002AAAAA2 |
      Then 儲值單 "DW261117035002AAAAA2" 的狀態應為 "PAID"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 30

  Rule: 後置（狀態）- 模擬付款（SimulatePaid=1）在正式環境不可入帳，測試環境可入帳

    Example: 正式環境收到模擬付款時失敗為 SIMULATED_IN_PROD，不入帳
      Given 綠界環境為 "prod"
      When 綠界送出付款通知：
        | 訂單                 | SimulatePaid |
        | DW261117035001AAAAA1 | 1            |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "SIMULATED_IN_PROD"
      And 儲值單 "DW261117035001AAAAA1" 標記為模擬付款：true
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 未明確允許公開測試特店時，即使環境為 stage 也不接受模擬付款
      Given 不允許使用綠界公開測試特店
      When 綠界送出付款通知：
        | 訂單                 | SimulatePaid |
        | DW261117035001AAAAA1 | 1            |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的失敗原因應為 "SIMULATED_IN_PROD"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 測試環境且明確允許公開測試特店時可入帳
      Given 允許使用綠界公開測試特店
      When 綠界送出付款通知：
        | 訂單                 | SimulatePaid |
        | DW261117035001AAAAA1 | 1            |
      Then 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"

    Example: 測試環境收到模擬付款時可入帳
      When 綠界送出付款通知：
        | 訂單                 | SimulatePaid |
        | DW261117035001AAAAA1 | 1            |
      Then 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"
      And 儲值單 "DW261117035001AAAAA1" 標記為模擬付款：true
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（狀態）- 累計贊助額只走原子累加，其他寫入玩家資料的流程不會以舊值覆蓋

    Example: 玩家資料被讀取後入帳，再以舊資料改暱稱存檔，累計贊助額與首次贊助時間不被覆蓋
      When 玩家 "阿明" 的資料被讀取後綠界付款通知入帳，再以舊資料改暱稱為 "新名字"：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 玩家 "阿明" 的累計贊助額應為 50
      And 玩家 "阿明" 的首次贊助時間應為 "2026-11-17T04:00:00Z"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（狀態）- ATM 取號通知（RtnCode=2）使訂單進入 AWAITING_PAYMENT，繳費完成的 ReturnURL 通知才入帳

    Example: 取號通知記錄到期時間（台北當日 23:59:59.999）並回 1|OK
      When 綠界送出取號通知：
        | 訂單                 | ExpireDate |
        | DW261117035001AAAAA1 | 2026/11/18 |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "AWAITING_PAYMENT"
      And 儲值單 "DW261117035001AAAAA1" 的付款到期時間應為 "2026-11-18T15:59:59.999Z"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 取號後繳費完成的付款通知入帳
      When 綠界送出取號通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 50

    Example: 取號通知簽章錯誤時不改單
      When 綠界送出取號通知：
        | 訂單                 | 簽章 |
        | DW261117035001AAAAA1 | 錯誤 |
      Then 綠界收到的回應應為 "0|CheckMacValueError"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"

    Example: 已入帳的單收到取號通知不改狀態
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      And 綠界送出取號通知：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 綠界收到的回應應為 "1|OK"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "PAID"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（回應）- 瀏覽器導回端點只導回同站 /?topup=，不入帳

    Example: 瀏覽器帶付款結果導回時 303 導向 /?topup=訂單編號，且不入帳
      When 玩家瀏覽器以付款結果導回：
        | 訂單                 |
        | DW261117035001AAAAA1 |
      Then 瀏覽器應被導向 "/?topup=DW261117035001AAAAA1"
      And 儲值單 "DW261117035001AAAAA1" 的狀態應為 "CREATED"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 訂單編號格式不符時導向首頁，不導向外部網址
      When 玩家瀏覽器送出偽造的導回網址：
        | MerchantTradeNo      |
        | https://evil.example |
      Then 瀏覽器應被導向 "/"

  Rule: 後置（狀態）- 累計贊助額於每次成功入帳累加，贊助等級由累計額推導（0／1／≥200 為 2），排行榜只回等級

    Example: 無付款時等級為 0
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 操作成功
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 0
      And 排行榜每一列都應有 sponsorTier

    Example: 單筆 NT$10 累計 10、等級 1
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035010AAAA10 | 阿明 | 10        | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035010AAAA10 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 10
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1

    Example: 累計 30、50、100、20 依序為 30(1)、80(1)、180(1)、200(2)
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035030AAAA30 | 阿明 | 30        | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035031AAAA31 | 阿明 | 50        | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035032AAAA32 | 阿明 | 100       | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035033AAAA33 | 阿明 | 20        | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035030AAAA30 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 30
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035031AAAA31 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 80
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035032AAAA32 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 180
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035033AAAA33 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 200
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 2

    Example: 單筆 NT$200 即為第 2 級
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035200AAA200 | 阿明 | 200       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035200AAA200 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 200
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 2

    Example: 累計 190 為第 1 級（門檻邊界）
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035090AAAA90 | 阿明 | 90        | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035100AAA100 | 阿明 | 100       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035090AAAA90 |
      And 綠界送出付款通知：
        | 訂單                 |
        | DW261117035100AAA100 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 190
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1

    Example: 門檻來自設定檔，設為 300 時累計 200 仍為第 1 級
      Given 贊助者名框第 2 級門檻設定為 300
      And 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035200AAA200 | 阿明 | 200       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 |
        | DW261117035200AAA200 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 200
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1

    Example: 同一筆 NT$100 通知重送 3 次，累計只加 100、等級維持 1
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035100AAA100 | 阿明 | 100       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界重送 3 次相同的付款通知：
        | 訂單                 |
        | DW261117035100AAA100 |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 100
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1

    Example: 同一筆 NT$100 通知並發 2 次，累計只加 100
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035100AAA100 | 阿明 | 100       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界同時送出 2 次相同的付款通知：
        | 訂單                 |
        | DW261117035100AAA100 |
      Then 玩家 "阿明" 的累計贊助額應為 100

    Example: 失敗、金額不符、正式環境模擬付款的通知不改變累計額與等級
      Given 系統中有以下儲值單：
        | 訂單                 | 玩家 | amountTwd | status  | createdAt            | paymentExpireAt |
        | DW261117035201AAA201 | 阿明 | 200       | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035202AAA202 | 阿明 | 200       | CREATED | 2026-11-17T03:56:00Z |                 |
        | DW261117035203AAA203 | 阿明 | 200       | CREATED | 2026-11-17T03:56:00Z |                 |
      When 綠界送出付款通知：
        | 訂單                 | RtnCode |
        | DW261117035201AAA201 | 0       |
      And 綠界送出付款通知：
        | 訂單                 | TradeAmt |
        | DW261117035202AAA202 | 1        |
      And 綠界環境為 "prod"
      And 綠界送出付款通知：
        | 訂單                 | SimulatePaid |
        | DW261117035203AAA203 | 1            |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 玩家 "阿明" 的累計贊助額應為 0
      And 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 0

    Example: 排行榜回應不含累計贊助金額
      Given 玩家 "阿明" 的累計贊助額為 80
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE | 19.0         |
      Then 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1
      And 回應原文不應包含 "sponsoredTotalTwd"
      And 回應原文不應包含 "sponsoredTotal"
      And 回應原文不應包含 "amountTwd"
