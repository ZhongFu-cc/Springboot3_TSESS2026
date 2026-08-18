# 前端 API 調整指南 — 註冊費與常年會費補繳

> 對應分支 `update/registration-fee`。本次改動的核心是：**一張註冊訂單可能同時包含「報名費」與「補繳常年會費」兩筆錢**，因此訂單相關的回傳結構全面補上明細，並新增一支註冊前的費用試算 API。

---

## 一、背景：為什麼訂單金額不再等於報名費

學會有一份「常年會費欠繳名單」，以身分證字號 / 護照號碼比對。使用者註冊時，系統除了依照 **註冊階段 × 國籍 × 會員身分** 算出報名費，還會查這份名單，若有欠費就**併入同一張訂單**一起收。

所以：

```
訂單總金額 = 報名費 + 補繳常年會費
```

過去前端把訂單的 `totalAmount` 直接當作「報名費」顯示，現在會顯示錯誤金額，**必須改成讀明細**。

---

## 二、新增 API：註冊前費用試算

```
POST /registration-fee/preview
```

不需要登入，不會寫入任何資料，純計算。與正式註冊走的是**同一份計算邏輯**，因此試算金額與實際成立的訂單金額一致。

### Request Body

| 欄位 | 型別 | 必填 | 說明 |
|---|---|---|---|
| `country` | string | ✅ | 國家。後端只分台灣 / 外國，大小寫與前後空白自動正規化，`taiwan`、`Taiwan`、`TAIWAN` 等價 |
| `category` | number | ✅ | 會員身分。1=Member、2=Others、3=Non-Member、4=MVP、5=Speaker、6=Moderator、7=Staff |
| `idCard` | string | ❌ | 身分證字號 / 護照號碼，用於比對常年會費欠繳名單。不傳或傳 `null` 一律視為無欠費 |

欄位名稱刻意與註冊用的 `AddMemberDTO` 對齊，可直接從註冊表單物件取值。

### Response

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "registrationFee": 3000,
    "membershipDue": 1500,
    "totalAmount": 4500,
    "free": false
  }
}
```

| 欄位 | 說明 |
|---|---|
| `registrationFee` | 報名費 |
| `membershipDue` | 應補繳的常年會費，不在欠繳名單中則為 `0` |
| `totalAmount` | 本次應付總金額，等於上面兩者相加 |
| `free` | 是否完全免費。`true` 時註冊後不會產生付款流程 |

### 呼叫範例

```js
async function previewFee(form) {
  const { data } = await axios.post('/registration-fee/preview', {
    country: form.country,
    category: Number(form.category),  // 表單值常是字串, 先轉數字
    idCard: form.idCard || null
  })
  return data.data
}
```

### 錯誤情況

| 情境 | 回應 |
|---|---|
| `country` 空白 / `category` 未給 | `400`，Bean Validation 錯誤 |
| `category` 不在 1~7 | `code: 500`，訊息「無效的會員身份值: X」 |

---

## 三、異動 API：訂單回傳結構

以下端點的回傳型別由 `Orders`（資料表實體）改為 **`OrdersVO`**，多了 `itemsSummary` 與 `ordersItemList`：

| 端點 | 權限 |
|---|---|
| `GET /orders/owner/{id}` | 會員本人 |
| `GET /orders/owner` | 會員本人 |
| `GET /orders/{id}` | super-admin |
| `GET /orders` | super-admin |
| `GET /orders/pagination` | super-admin |

### OrdersVO

```json
{
  "ordersId": "1234567890",
  "natureId": "...",
  "itemsSummary": "Registration Fee",
  "totalAmount": 4500,
  "status": 0,
  "ordersItemList": [
    {
      "ordersItemId": "...",
      "productType": "Registration Fee",
      "productName": "TSESS 2026 Registration Fee",
      "quantity": 1,
      "unitPrice": 3000,
      "subtotal": 3000
    },
    {
      "ordersItemId": "...",
      "productType": "Annual Membership Dues",
      "productName": "Annual Membership Dues (2024-2026)",
      "quantity": 1,
      "unitPrice": 1500,
      "subtotal": 1500
    }
  ]
}
```

**要拆開顯示報名費與補繳會費，就靠 `ordersItemList[].productType` 判斷：**

| `productType` | 意義 |
|---|---|
| `Registration Fee` | 報名費 |
| `Annual Membership Dues` | 補繳常年會費 |

金額為 0 的項目不會產生明細。唯一例外是完全免費的訂單，會保留一筆 0 元報名費明細，確保每張訂單至少有一筆細項。

### `status` 對照

| 值 | 意義 |
|---|---|
| 0 | 未付款 |
| 1 | 已付款 — 待審核（人工匯款後的狀態） |
| 2 | 付款成功 |
| 3 | 付款失敗 |

> 註：`OrdersVO` 上的 Swagger 說明先前寫成「2為付款失敗」，已修正為上表。

### 其他受影響的回傳

| 端點 | 變更 |
|---|---|
| `GET /member/owner`、`GET /member/member-and-order` | `MemberOrderVO.ordersList` 的元素型別由 `Orders` 改為 `OrdersVO`，每張訂單開始帶 `ordersItemList` |
| `GET /member/unpaid-member`、`GET /member/tag/pagination` | `MemberTagVO.amount` **語意變更**：不再是「註冊費金額」，而是訂單應繳總額，可能含補繳的常年會費。需要拆解請另外查訂單 API |

---

## 四、註冊流程（重要）

**送出註冊資料之前，必須先呼叫試算 API，把金額顯示給使用者確認，使用者確認後才真正送出。**

原因是註冊一旦送出就會直接建立訂單、寄出註冊成功通知信、並掛上「未繳費」標籤，這些都不是前端能撤銷的。使用者若在看到金額後才發現填錯身分或國籍，善後成本很高 —— 尤其是被比對到欠繳常年會費、金額比預期多出一截的使用者，事前沒看到金額幾乎必定會來客訴。

### 流程圖

```
使用者填寫註冊表單
        │
        ▼
① POST /registration-fee/preview      ← 只帶 country / category / idCard
        │
        ▼
   顯示費用確認畫面
   「報名費 NT$3,000 + 補繳常年會費 NT$1,500 = 應付 NT$4,500」
        │
        ├── 使用者「返回修改」 ──→ 回到表單（此時系統無任何殘留資料）
        │
        ▼ 使用者按下「確認送出」
② POST /member                        ← 完整註冊資料 + 驗證碼
        │                                回傳 SaTokenInfo，會員與訂單同時成立
        ▼
③ GET /orders/owner                   ← 帶上 token，取得剛成立的訂單
        │
        ├── free === true / status === 2 ──→ 完成頁，不需付款
        │
        ▼ 需付款
④ GET /orders/payment?id={ordersId}    ← 回傳綠界付款表單 HTML 字串
        │
        ▼
   將表單塞進頁面並 submit，導向綠界付款頁
```

### 各步驟細節

**① 試算**
建議在 `country`、`category`、`idCard` 任一欄位變動後觸發，但 `idCard` 請等 blur 再打，否則打字過程會一直查欠費名單。

**② 送出註冊**
`POST /member`，body 為完整的 `AddMemberDTO`（含 `verificationKey` / `verificationCode`）。多傳的欄位後端會忽略，不會報錯，所以①和②可以共用同一個表單物件。

回傳 `SaTokenInfo`，取 `tokenValue` 作為後續會員身分憑證，放在 `Authorization-member` 請求頭，格式為 `Bearer {tokenValue}`。

**③ 取得訂單**
`POST /member` 不回傳訂單編號，需要另外呼叫 `GET /orders/owner` 取得。這支需要帶上步驟②拿到的 token。

**④ 付款**
`GET /orders/payment?id={ordersId}` 回傳的是**綠界付款表單的 HTML 字串**（在 `data` 欄位），不是 URL。前端需要把它寫進 DOM 再觸發 submit，才會跳轉到綠界付款頁。

免費訂單（`totalAmount` 為 0）在建立當下狀態就是 `2 付款成功`，不要對它呼叫付款端點。

---

## 五、注意事項

### 試算不鎖價

試算與正式註冊之間若跨越了階段切換時間點（例如早鳥截止），兩次算出的金額會不同，**實際收費以註冊當下重算的為準**。目前沒有做鎖價機制。

實務上這個窗口很短，但若使用者把確認頁開著隔天才送出，就可能發生。建議確認頁停留過久（例如超過 10 分鐘）時重新呼叫一次試算，或在送出後以 `GET /orders/owner` 拿到的訂單金額為準顯示，而不是沿用試算結果。

### 多傳欄位不會報錯

後端 Jackson 設定為忽略未知屬性，前端把整包表單丟給試算 API 是安全的。

### 空字串會被轉成 null

後端有全域的 String 反序列化處理，**所有空字串欄位都會轉成 `null`**。所以 `idCard: ""` 等同於沒傳，會被視為無欠費。

### Long 型別以字串回傳

`ordersId`、`memberId` 等 Long 欄位在 JSON 中是**字串**（避免 JS 精度丟失），比較與傳遞時請當字串處理，不要 `parseInt`。
