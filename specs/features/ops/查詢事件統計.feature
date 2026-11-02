@query
Feature: 查詢事件統計
  營運者（@營運者）查詢後端記錄的事件統計（D21「事件可查」；澄清 ASM-09＝B，2026-10-29）。事件來源：瀏覽商店、購買buff、領取廣告獎勵（CHANGE 2026-11-02）的後置（狀態）Rule；登入提示事件因 ASM-08=B 由前端本機記錄，不在統計範圍。本查詢不寫入任何資料。

  # 澄清 GAP-13＝C（2026-10-30）：只在內網或本機開放，不做身分驗證，故不設營運者憑證前置 Rule。
  # 推論：以部署層限制存取——API 置於獨立路徑前綴（例 /internal/ops/），對外反向代理／防火牆不轉發，不對外暴露。
  # 澄清 GAP-14＝A（2026-10-30）：不做前端頁面，營運者以 curl／API 工具直接呼叫，回應為 JSON。

  Background:
    Given 系統中有以下玩家：
      | 玩家 | playerKind | nickname | deviceId | isSilverUnlocked | isVoided |
      | 甲   | GUEST      | 勇者248  | dev-a    | true             | false    |
      | 乙   | MEMBER     | 乙       |          | true             | false    |
    And 系統中有以下事件：
      | eventKind     | 玩家 | isSilverUnlocked | buffItem     | paidCoins | occurredAt           |
      | SHOP_VIEW     | 甲   | true             |              |           | 2026-11-01T00:00:00Z |
      | SHOP_VIEW     | 甲   | true             |              |           | 2026-11-01T05:00:00Z |
      | SHOP_VIEW     | 乙   | true             |              |           | 2026-11-01T06:00:00Z |
      | BUFF_PURCHASE | 甲   | true             | CANNON       | 400       | 2026-11-01T05:01:00Z |
      | BUFF_PURCHASE | 乙   | true             | CANNON       | 400       | 2026-11-01T06:01:00Z |
      | BUFF_PURCHASE | 乙   | true             | ATTACK_SPEED | 200       | 2026-11-01T06:02:00Z |
      | AD_REWARD     | 甲   | true             |              |           | 2026-11-01T07:00:00Z |
      | AD_REWARD     | 甲   | true             |              |           | 2026-11-01T07:05:00Z |
      | SHOP_VIEW     | 乙   | true             |              |           | 2026-11-02T00:00:00Z |
    # 營運者不攜帶任何憑證（GAP-13=C，部署層限制內網／本機）

  Rule: 前置（參數）- 查詢期間的起訖時間必須提供且起始不晚於結束

    Scenario Outline: 起訖時間缺漏或起始晚於結束時操作失敗
      When 營運者查詢事件統計：
        | from   | to   |
        | <from> | <to> |
      Then 操作失敗，錯誤代碼為 "<code>"

      @error:400001
      Examples: 缺漏
        | from                 | to                   | code   |
        |                      | 2026-11-02T00:00:00Z | 400001 |
        | 2026-11-01T00:00:00Z |                      | 400001 |

      @error:400007
      Examples: 起始晚於結束
        | from                 | to                   | code   |
        | 2026-11-02T00:00:00Z | 2026-11-01T00:00:00Z | 400007 |

  Rule: 後置（回應）- 應回傳查詢期間內各事件類型（商店瀏覽、購買buff、廣告獎勵）的次數與不重複玩家數
    # CHANGE 2026-11-02（D33/D34）推論 INF-A4：新增廣告獎勵事件 AD_REWARD（領取廣告獎勵.feature），一併統計次數與不重複玩家數；列表固定依 SHOP_VIEW、BUFF_PURCHASE、AD_REWARD 排列
    # 依設計指引 G21（事件用於計算解鎖到首次購買、各 buff 購買率等指標）
    # 澄清 AMB-04＝A（2026-10-30）：只回查詢期間總計（事件類型 × 次數、不重複玩家數），不依日分組、不算轉換率。
    # 推論：期間無事件時各類型回 0；不重複玩家以帳號或 guest id 計；起訖含起不含訖，以伺服器時間（UTC）判定。

    Example: 回傳期間內各事件類型的次數與不重複玩家數（含起不含訖）
      When 營運者查詢事件統計：
        | from                 | to                   |
        | 2026-11-01T00:00:00Z | 2026-11-02T00:00:00Z |
      Then 操作成功
      And 回應中的 "eventTotals" 應為：
        | eventKind     | count | uniquePlayers |
        | SHOP_VIEW     | 3     | 2             |
        | BUFF_PURCHASE | 3     | 2             |
        | AD_REWARD     | 2     | 1             |

    Example: 期間內無事件時各類型回 0
      When 營運者查詢事件統計：
        | from                 | to                   |
        | 2026-10-01T00:00:00Z | 2026-10-02T00:00:00Z |
      Then 操作成功
      And 回應中的 "eventTotals" 應為：
        | eventKind     | count | uniquePlayers |
        | SHOP_VIEW     | 0     | 0             |
        | BUFF_PURCHASE | 0     | 0             |
        | AD_REWARD     | 0     | 0             |

  Rule: 後置（回應）- 購買事件應依 buff 品項分組回傳次數與金幣總額
    # 澄清 AMB-04＝A：品項分組屬期間總計的一部分。推論：金幣總額以購買事件記錄的購買當下價格加總（GAP-02 推論）；期間無購買時回空列表。

    Example: 購買事件依 buff 品項分組回傳次數與金幣總額
      When 營運者查詢事件統計：
        | from                 | to                   |
        | 2026-11-01T00:00:00Z | 2026-11-02T00:00:00Z |
      Then 操作成功
      And 回應中的 "purchasesByItem" 應為：
        | buffItem     | count | totalCoins |
        | ATTACK_SPEED | 1     | 200        |
        | CANNON       | 2     | 800        |

    Example: 期間內無購買時品項分組為空列表
      When 營運者查詢事件統計：
        | from                 | to                   |
        | 2026-11-01T00:00:00Z | 2026-11-01T05:00:00Z |
      Then 操作成功
      And 回應中的 "purchasesByItem" 應為空
