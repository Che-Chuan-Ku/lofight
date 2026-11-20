@command
Feature: 接收實況主贊助通知
  綠界「實況主收款」連結付款成功後，綠界以 server POST 通知（加密 Data＋CheckMacValue）。付款頁由玩家自填金額與「贊助者名稱」，贊助者名稱對應遊戲暱稱。
  CHANGE 2026-11-17（使用者裁決）：只認會員（Google 登入）；暱稱去頭尾空白後與「未作廢會員」暱稱完全相等（英文字母不分大小寫）且恰好 1 人才入帳；入帳當下綁定該帳號，之後改暱稱不影響；
  贊助不給金幣（CHANGE 2026-11-20，綠界審查：topup.coins-per-twd = 0）、累計 ≥ 200 為第 2 級（沿用儲值設定）。入帳沿用儲值入帳路徑（建一筆 PAID 的儲值單，方案 DONATE）：事件 TOPUP＋累計贊助額＋同一交易；錢包餘額與金幣帳本不變。
  端點 POST /api/csg/v1/ecpay/donate-notify，不需 token。驗證順序（皆在寫資料前）：外層特店 → 解密 → CheckMacValue（明文或密文任一相符）→ 內層特店 → RtnCode=1 且 TradeStatus=1 → SimulatePaid。
  回應純文字：驗章失敗 0|CheckMacValueError、外層特店不符 0|MerchantIdError；驗章通過後無論入帳與否一律 1|OK（避免綠界重送）。冪等靠綠界 TradeNo／MerchantTradeNo 唯一索引。
  測試用綠界公開 stage 測試特店 3002607 與其公開測試 HashKey／HashIV。

  Background:
    Given 現在時間為 "2026-11-17T04:00:00Z"
    And 後端設定的目前版本前兩碼為 "19.0"
    And 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 阿明 | MEMBER     | 阿明     |          | true             | false    |
      | 小華 | MEMBER     | 小華     |          | false            | false    |
      | 愛麗 | MEMBER     | Alice    |          | false            | false    |
      | 路人 | GUEST      | 勇者248  | dev-0001 | false            | false    |
      | 遊客 | GUEST      | 遊客甲   | dev-0002 | false            | false    |
    And 玩家 "阿明" 的金幣餘額為 100
    And 系統中有以下排行榜成績：
      | 玩家 | boardVersion | tier   | run  | isVictory | gameSeconds | settledAt            |
      | 阿明 | 19.0         | BRONZE | r-a1 | true      | 120         | 2026-11-01T10:02:00Z |
    And 目前以玩家 "阿明" 的有效存取權杖操作

  # CHANGE 2026-11-17（安全審查）：候選＝未作廢的會員＋訪客，只有「恰好 1 人且為會員」才入帳（有訪客同名一律不入帳）；
  # 贊助者名稱為系統預設暱稱格式（勇者＋3 碼）一律 DEFAULT_NAME 不入帳；比對一律 NFKC → strip → 小寫（全形與半形視為相同）；
  # 去重只認綠界 TradeNo（MerchantTradeNo 可被收款連結重用）；入帳的儲值單號用自有前綴 DN＋TradeNo，不沿用綠界 MerchantTradeNo；
  # 驗章通過但無法使用的通知記為 INVALID；入帳失敗（例如會員沒有錢包）記為 ERROR，仍回 1|OK 以停止綠界重送。

  Rule: 前置（驗證）- 外層特店、解密、CheckMacValue 必須正確，否則不寫任何資料

    Example: 簽章錯誤時回 0|CheckMacValueError 且不入帳、不留紀錄
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 簽章 |
        | 阿明       | 錯誤 |
      Then 綠界收到的回應應為 "0|CheckMacValueError"
      And 贊助紀錄總數應為 0
      And 儲值單總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0
      And 事件數應為 0

    Example: Data 無法解密時回 0|CheckMacValueError 且不寫資料
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 資料 |
        | 阿明       | 亂碼 |
      Then 綠界收到的回應應為 "0|CheckMacValueError"
      And 贊助紀錄總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 外層特店編號不符時回 0|MerchantIdError 且不寫資料
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 外層特店 |
        | 阿明       | 9999999  |
      Then 綠界收到的回應應為 "0|MerchantIdError"
      And 贊助紀錄總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 驗章通過但內層特店編號不符時回 1|OK、不入帳，記為 INVALID
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 內層特店 |
        | 阿明       | 9999999  |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄共 1 筆且配對狀態皆為 "INVALID"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 贊助紀錄 "DN0001" 記錄的內容不應包含簽章與金鑰
      And 儲值單總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 驗章通過但內容不是 JSON 時回 1|OK、不入帳，記為 INVALID
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 資料   |
        | 阿明       | 非JSON |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄共 1 筆且配對狀態皆為 "INVALID"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 驗章通過但缺少 TradeNo 時回 1|OK、不入帳，記為 INVALID
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 缺少    |
        | 阿明       | TradeNo |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄共 1 筆且配對狀態皆為 "INVALID"
      And 儲值單總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Scenario Outline: 驗章通過但金額不是正整數時回 1|OK、不入帳，記為 INVALID
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額   |
        | 阿明       | <金額> |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄共 1 筆且配對狀態皆為 "INVALID"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

      Examples:
        | 金額     |
        | 0        |
        | -5       |
        | 99999999 |

    Example: CheckMacValue 以密文計算時同樣通過（明文或密文任一相符）
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 簽章 |
        | 阿明       | 密文 |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "MATCHED"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 前置（驗證）- 只有 RtnCode=1 且 TradeStatus=1 才處理

    Example: RtnCode 不是 1 時回 1|OK 但不入帳、不留紀錄
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | RtnCode |
        | 阿明       | 0       |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: TradeStatus 不是 1 時回 1|OK 但不入帳、不留紀錄
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | TradeStatus |
        | 阿明       | 0           |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 前置（驗證）- SimulatePaid=1 只有在非正式環境且允許測試特店時才接受

    Example: 允許測試特店的測試環境接受模擬付款並入帳
      Given 允許使用綠界公開測試特店
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | SimulatePaid |
        | 阿明       | 1            |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "MATCHED"
      And 贊助紀錄 "DN0001" 標記為模擬付款：true
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 不允許測試特店時模擬付款只記錄不入帳
      Given 不允許使用綠界公開測試特店
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | SimulatePaid |
        | 阿明       | 1            |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "SIMULATED"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 儲值單總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 0

    Example: 環境為 prod 時即使允許測試特店，模擬付款也只記錄不入帳
      Given 允許使用綠界公開測試特店
      And 綠界環境為 "prod"
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | SimulatePaid |
        | 阿明       | 1            |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "SIMULATED"
      And 玩家 "阿明" 的金幣餘額應為 100

  Rule: 後置（狀態）- 贊助者名稱恰好對應 1 位未作廢會員時入帳並累計贊助額

    Example: 配對成功時錢包不變、無帳本、事件一筆、累計贊助額增加
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 100  |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "MATCHED"
      And 贊助紀錄 "DN0001" 應綁定玩家 "阿明" 與一筆已入帳的儲值單
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 100
      And 玩家 "阿明" 的首次贊助時間應為 "2026-11-17T04:00:00Z"
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件
      And 贊助紀錄 "DN0001" 記錄的內容不應包含簽章與金鑰

    Example: JSON 格式的外層 body 同樣入帳
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 格式 |
        | 阿明       | 50   | JSON |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "MATCHED"
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 50

    Example: 頭尾空白會去除、英文字母不分大小寫
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | "  aLICE " | 30   |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 應綁定玩家 "愛麗" 與一筆已入帳的儲值單
      And 玩家 "愛麗" 的金幣餘額應為 0
      And 玩家 "愛麗" 的累計贊助額應為 30

    Example: 累計 100 為第 1 級、200 為第 2 級
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 100  |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 1
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 交易編號 |
        | 阿明       | 100  | DN0002   |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 2
      And 玩家 "阿明" 的累計贊助額應為 200
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 單筆 200 即為第 2 級
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 200  |
      And 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 2

    Example: 入帳後改暱稱，贊助累計與名框等級不變
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 200  |
      And 修改暱稱：
        | nickname |
        | 新名字   |
      Then 操作成功
      And 玩家 "阿明" 的累計贊助額應為 200
      When 查詢排行榜：
        | tier   | boardVersion |
        | BRONZE |              |
      Then 排行榜 "entries" 中玩家 "阿明" 的 sponsorTier 應為 2

  Rule: 後置（狀態）- 無法唯一對應會員時只記錄、不入帳，回 1|OK

    Example: 查無此暱稱時記為 UNMATCHED
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 不存在的人 |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "UNMATCHED"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 儲值單總數應為 0
      And 玩家 "阿明" 的金幣餘額應為 100
      And 事件數應為 0

    Example: 贊助者名稱為空白時記為 UNMATCHED
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | "   "     |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "UNMATCHED"

    Example: 兩位會員同名時記為 AMBIGUOUS，兩人都不入帳
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 分身 | MEMBER     | 阿明     |          | false            | false    |
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 阿明       |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "AMBIGUOUS"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "分身" 的金幣餘額應為 0
      And 玩家 "阿明" 的累計贊助額應為 0
      And 玩家 "分身" 的累計贊助額應為 0

    Example: 只有訪客同名時記為 GUEST_ONLY，不入帳
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 遊客甲     |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "GUEST_ONLY"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 玩家 "遊客" 的金幣餘額應為 0
      And 玩家 "遊客" 的累計贊助額應為 0

    Example: 贊助者名稱是系統預設暱稱格式時記為 DEFAULT_NAME，不入帳
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 勇者248    |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "DEFAULT_NAME"
      And 玩家 "路人" 的金幣餘額應為 0

    Example: 會員剛好使用預設格式暱稱時也不入帳（全形數字視同半形）
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 同格 | MEMBER     | 勇者500  |          | false            | false    |
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 勇者５００ |
      Then 贊助紀錄 "DN0001" 的配對狀態應為 "DEFAULT_NAME"
      And 玩家 "同格" 的金幣餘額應為 0
      And 玩家 "同格" 的累計贊助額應為 0

    Example: 會員與訪客同名時無法確定是誰，記為 AMBIGUOUS，兩人都不入帳
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 同名 | MEMBER     | 阿華     |          | false            | false    |
        | 影子 | GUEST      | 阿華     | dev-0003 | false            | false    |
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 阿華       |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄 "DN0001" 的配對狀態應為 "AMBIGUOUS"
      And 玩家 "同名" 的金幣餘額應為 0
      And 玩家 "影子" 的金幣餘額應為 0
      And 玩家 "同名" 的累計贊助額應為 0

    Example: 全形贊助者名稱配對到半形暱稱（NFKC）
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | Ａｌｉｃｅ | 30   |
      Then 贊助紀錄 "DN0001" 應綁定玩家 "愛麗" 與一筆已入帳的儲值單
      And 玩家 "愛麗" 的金幣餘額應為 0

    Example: 贊助者名稱含零寬字元時不會被當成同名
      When 綠界送出實況主贊助通知：
        | 贊助者名稱   |
        | 阿{ZWSP}明   |
      Then 贊助紀錄 "DN0001" 的配對狀態應為 "UNMATCHED"
      And 玩家 "阿明" 的金幣餘額應為 100

    Example: 作廢會員的暱稱不參與配對
      Given 系統中有以下玩家：
        | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
        | 舊號 | MEMBER     | 舊暱稱   |          | false            | true     |
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 |
        | 舊暱稱     |
      Then 贊助紀錄 "DN0001" 的配對狀態應為 "UNMATCHED"
      And 玩家 "舊號" 的金幣餘額應為 0

  Rule: 後置（狀態）- 同一筆贊助重送或並發只處理一次

    Example: 重送 3 次只入帳一次且每次都回 1|OK
      When 綠界重送 3 次相同的實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 100  |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄總數應為 1
      And 儲值單總數應為 1
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單
      And 玩家 "阿明" 應有 1 筆 TOPUP 事件

    Example: 同時送出 5 次相同通知只入帳一次
      When 綠界同時送出 5 次相同的實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 100  |
      Then 所有綠界同時送出的回應應為 "1|OK"
      And 贊助紀錄總數應為 1
      And 儲值單總數應為 1
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單

    Example: 不同交易編號的兩筆贊助各自入帳
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 交易編號 |
        | 阿明       | 30   | DN0001   |
      And 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 交易編號 |
        | 阿明       | 50   | DN0002   |
      Then 儲值單總數應為 2
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 的累計贊助額應為 80
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單

    Example: 去重只看 TradeNo：不同付款重用同一個 MerchantTradeNo 時各自入帳
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 交易編號 | 商家單號 |
        | 阿明       | 30   | DN0001   | SHARED01 |
      And 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 | 交易編號 | 商家單號 |
        | 阿明       | 50   | DN0002   | SHARED01 |
      Then 贊助紀錄總數應為 2
      And 儲值單總數應為 2
      And 玩家 "阿明" 的累計贊助額應為 80
      And 玩家 "阿明" 的金幣餘額應為 100
      And 玩家 "阿明" 應有 0 筆 TOPUP 帳本且皆綁定儲值單

    Example: 入帳的儲值單使用自有前綴單號，不沿用綠界 MerchantTradeNo
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 交易編號 | 商家單號 |
        | 阿明       | 2611171225 | DW261117035001AAAAA1 |
      Then 儲值單 "DN2611171225" 的狀態應為 "PAID"
      And 儲值單總數應為 1

  Rule: 後置（狀態）- 入帳失敗（例如會員沒有錢包）整筆回滾，記為 ERROR，仍回 1|OK

    Example: 會員沒有錢包時不入帳、不留半套資料，記為 ERROR
      Given 玩家 "阿明" 的錢包被移除
      When 綠界送出實況主贊助通知：
        | 贊助者名稱 | 金額 |
        | 阿明       | 100  |
      Then 綠界收到的回應應為 "1|OK"
      And 贊助紀錄共 1 筆且配對狀態皆為 "ERROR"
      And 贊助紀錄 "DN0001" 的錯誤類別應為 "IllegalStateException"
      And 贊助紀錄 "DN0001" 不應綁定任何玩家或儲值單
      And 儲值單總數應為 0
      And 玩家 "阿明" 的累計贊助額應為 0
      And 玩家 "阿明" 應有 0 筆 TOPUP 事件
