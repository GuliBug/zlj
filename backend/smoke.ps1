# Smoke test for zhenglongji mall backend
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = "http://localhost:8080"

function Post($url, $body, $token) {
    $h = @{ "Content-Type" = "application/json" }
    if ($token) { $h["X-Token"] = $token }
    $json = $body | ConvertTo-Json -Depth 6
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    Invoke-RestMethod -Method Post -Uri "$base$url" -Headers $h -Body $bytes
}
function GetReq($url, $token) {
    $h = @{}
    if ($token) { $h["X-Token"] = $token }
    Invoke-RestMethod -Method Get -Uri "$base$url" -Headers $h
}
function Assert($name, $cond) {
    if ($cond) { Write-Output "PASS  $name" } else { Write-Output "FAIL  $name"; $script:failed = $true }
}
$script:failed = $false

# 1. member login
$login = Post "/api/member/login" @{ phone = "13800001114"; smsCode = "123456" } $null
Assert "member login code=200 token=MBR-" ($login.code -eq 200 -and $login.data.token.StartsWith("MBR-"))
$tk = $login.data.token

$badLogin = Post "/api/member/login" @{ phone = "13800001114"; smsCode = "000000" } $null
Assert "wrong sms code rejected" ($badLogin.code -ne 200)

# 2. products
$products = GetReq "/api/products?page=1&size=10" $null
Assert "product list total=5 (ON only)" ($products.code -eq 200 -and $products.data.total -eq 5)
$prodId = $products.data.rows[0].id
$detail = GetReq "/api/products/$prodId" $null
Assert "product detail warnText+skus" ($detail.data.warnText -ne $null -and $detail.data.skus.Count -ge 2)
$skuId = $detail.data.skus[0].id

$offDetail = GetReq "/api/products/6" $null
Assert "off product status=OFF visible" ($offDetail.data.status -eq "OFF")

# 3. cart
$add = Post "/api/cart" @{ skuId = $skuId; quantity = 2 } $tk
Assert "cart add" ($add.code -eq 200)
$cart = GetReq "/api/cart" $tk
Assert "cart has item qty=2" ($cart.data.rows.Count -ge 1 -and $cart.data.rows[0].quantity -ge 2)
$upd = Post "/api/cart/update" @{ id = $cart.data.rows[0].id; quantity = 3; checked = $true } $tk
Assert "cart update" ($upd.code -eq 200)

# 4. address
$addr = Post "/api/addresses" @{ receiver = "ZhangSan"; phone = "13800001114"; region = "Hunan Xiangtan"; detail = "No.1 Street"; isDefault = $true } $tk
Assert "address create" ($addr.code -eq 200)
$addrId = $addr.data.id

# 5. create order with idempotent key
$key = [guid]::NewGuid().ToString()
$order = Post "/api/orders" @{ idempotentKey = $key; fulfillmentType = "EXPRESS"; addressId = $addrId; usePoints = $false; items = @(@{ skuId = $skuId; quantity = 2 }) } $tk
Assert "order create" ($order.code -eq 200 -and $order.data.orderNo -ne $null)
$orderNo = $order.data.orderNo

# 6. idempotent repeat
$order2 = Post "/api/orders" @{ idempotentKey = $key; fulfillmentType = "EXPRESS"; addressId = $addrId; usePoints = $false; items = @(@{ skuId = $skuId; quantity = 2 }) } $tk
Assert "idempotent repeat returns same order" ($order2.code -eq 200 -and $order2.data.orderNo -eq $orderNo)

# freight check: goods 2*1000=2000 <9900 -> freight 1000
Assert "freight rule" ($order.data.freight -eq 1000 -and $order.data.payAmount -eq ($order.data.goodsAmount + 1000))

# 7. stock reserved
$skus = GetReq "/admin-api/skus?page=1&size=50" $null

# 8. pay
$pay = Post "/api/orders/$orderNo/pay" @{} $tk
Assert "pay -> PAID" ($pay.code -eq 200 -and $pay.data.status -eq "PAID")

# 9. duplicate pay rejected
$dupPay = Post "/api/orders/$orderNo/pay" @{} $tk
Assert "duplicate pay rejected" ($dupPay.code -ne 200)

# 10. admin login
$adminLogin = Post "/admin-api/login" @{ username = "admin"; password = "admin123" } $null
Assert "admin login" ($adminLogin.code -eq 200 -and $adminLogin.data.token.StartsWith("ADM-"))
$atk = $adminLogin.data.token

$badAdmin = Post "/admin-api/login" @{ username = "admin"; password = "wrong" } $null
Assert "admin wrong password rejected" ($badAdmin.code -ne 200)

# 11. ship
$ship = Post "/admin-api/orders/$orderNo/ship" @{ company = "SF"; no = "SF123456" } $atk
Assert "ship -> SHIPPED" ($ship.code -eq 200)

# confirm receive
$confirm = Post "/api/orders/$orderNo/confirm" @{} $tk
Assert "confirm -> COMPLETED" ($confirm.code -eq 200)

# 12. dashboard
$dash = GetReq "/admin-api/dashboard" $atk
Assert "dashboard has gmvToday" ($dash.code -eq 200 -and $dash.data.gmvToday -ge 0)

# 13. pickup flow: order -> pay -> verify-code -> verify
$sku2 = $detail.data.skus[1].id
$pu = Post "/api/orders" @{ idempotentKey = [guid]::NewGuid().ToString(); fulfillmentType = "PICKUP"; storeId = 1; usePoints = $false; items = @(@{ skuId = $sku2; quantity = 1 }) } $tk
Assert "pickup order create" ($pu.code -eq 200)
$puNo = $pu.data.orderNo
$puPay = Post "/api/orders/$puNo/pay" @{} $tk
Assert "pickup pay -> PICKUP_WAIT" ($puPay.code -eq 200 -and $puPay.data.status -eq "PICKUP_WAIT")
$vc = GetReq "/api/orders/$puNo/verify-code" $tk
Assert "verify-code 6 digits" ($vc.code -eq 200 -and $vc.data.code -match "^\d{6}$")
$verify = Post "/admin-api/verify" @{ orderNo = $puNo; code = $vc.data.code; storeId = 1 } $atk
Assert "admin verify -> COMPLETED" ($verify.code -eq 200)
$wrongVerify = Post "/admin-api/verify" @{ orderNo = $puNo; code = "000000"; storeId = 1 } $atk
Assert "verify again rejected (not PICKUP_WAIT)" ($wrongVerify.code -ne 200)

# 14. aftersale flow on express order
$reasons = GetReq "/api/aftersales/reasons" $tk
Assert "aftersale reasons" ($reasons.code -eq 200 -and $reasons.data.Count -ge 5)
$as = Post "/api/aftersales" @{ orderNo = $orderNo; type = "REFUND_ONLY"; reason = "质量问题"; description = "damaged" } $tk
Assert "aftersale apply" ($as.code -eq 200)
$asNo = $as.data.afterSaleNo
$audit = Post "/admin-api/aftersales/$asNo/audit" @{ approve = $true; opinion = "agree refund" } $atk
Assert "aftersale audit approve" ($audit.code -eq 200)
$asDetail = GetReq "/api/aftersales/$asNo" $tk
Assert "aftersale REFUNDED with flow" ($asDetail.data.status -eq "REFUNDED" -and $asDetail.data.flow.Count -ge 3)

# refund idempotent
$refundAgain = Post "/admin-api/aftersales/$asNo/refund" @{} $atk
Assert "refund idempotent returns success" ($refundAgain.code -eq 200)

# 15. member points log & level
$me = GetReq "/api/member/me" $tk
Assert "member me masked phone" ($me.data.phone -match "^\d{3}\*\*\*\*\d{4}$")
$plogs = GetReq "/api/member/points/logs?page=1&size=10" $tk
Assert "points logs exist" ($plogs.data.total -ge 1)

# 16. admin sku & stock ops
$skusList = GetReq "/admin-api/skus?page=1&size=50" $atk
Assert "admin skus list" ($skusList.code -eq 200 -and $skusList.data.total -ge 5)
$someSku = $skusList.data.rows[0].id
$inbound = Post "/admin-api/stock/inbounds" @{ skuId = $someSku; quantity = 10; batchNo = "B20260101"; supplier = "SupplierA" } $atk
Assert "inbound" ($inbound.code -eq 200)
$inboundNoBatch = Post "/admin-api/stock/inbounds" @{ skuId = $someSku; quantity = 5 } $atk
Assert "inbound without batchNo rejected" ($inboundNoBatch.code -ne 200)
$flows = GetReq "/admin-api/stock/flows?page=1&size=20" $atk
Assert "stock flows" ($flows.data.total -ge 5)
$threshold = Post "/admin-api/stock/threshold" @{ skuId = $someSku; threshold = 99 } $atk
Assert "threshold update" ($threshold.code -eq 200)

# stocktake
$st = Post "/admin-api/stock/stocktakes" @{ items = @(@{ skuId = $someSku; actual = 999 }); remark = "demo" } $atk
Assert "stocktake create" ($st.code -eq 200)
$stList = GetReq "/admin-api/stock/stocktakes?page=1&size=5" $atk
$stId = $stList.data.rows[0].id
$stConfirmSame = Post "/admin-api/stock/stocktakes/$stId/confirm" @{} $atk
Assert "stocktake self-confirm rejected" ($stConfirmSame.code -ne 200)

# 17. banned words
$bw = Post "/admin-api/check-banned-words" @{ text = "this product has 提神 功效" } $atk
Assert "banned words hits" ($bw.data.hits.Count -eq 2)
$badProduct = Post "/admin-api/products" @{ name = "新品提神槟榔"; categoryId = 3; skus = @(@{ flavor = "原味"; spec = "单包"; barcode = "ZLJ9001"; price = 1000; stock = 10 }) } $atk
Assert "banned word product rejected" ($badProduct.code -ne 200)

# 18. audit logs & alerts
$auditLogs = GetReq "/admin-api/audit-logs?page=1&size=10" $atk
Assert "audit logs" ($auditLogs.data.total -ge 1)
$alerts = GetReq "/admin-api/alerts?page=1&size=10" $atk
$dupPayAlerts = @($alerts.data.rows | Where-Object { $_.type -eq "DUPLICATE_PAY" })
Assert "alerts include DUPLICATE_PAY" ($dupPayAlerts.Count -ge 1)

# 19. level rules
$lr = GetReq "/admin-api/level-rules" $atk
Assert "level rules 4 levels" ($lr.data.Count -eq 4)

# 20. CSV export
$csv = Invoke-WebRequest -UseBasicParsing -Uri "$base/admin-api/orders/export" -Headers @{ "X-Token" = $atk }
Assert "CSV export content-type" ($csv.Headers["Content-Type"] -like "text/csv*")

# 21. members admin list masked
$members = GetReq "/admin-api/members?page=1&size=10" $atk
Assert "admin members masked" ($members.data.rows[0].phone -match "\*\*\*\*")

# 22. verify records
$vr = GetReq "/admin-api/verify-records?page=1&size=10" $atk
Assert "verify records has pickup" ($vr.data.total -ge 1)

# 23. cancel flow: new order then cancel
$c = Post "/api/orders" @{ idempotentKey = [guid]::NewGuid().ToString(); fulfillmentType = "PICKUP"; storeId = 1; usePoints = $false; items = @(@{ skuId = $skuId; quantity = 1 }) } $tk
$cNo = $c.data.orderNo
$cancel = Post "/api/orders/$cNo/cancel" @{} $tk
Assert "cancel order" ($cancel.code -eq 200)
$d = GetReq "/api/orders/$cNo" $tk
Assert "cancelled status CLOSED with timeline" ($d.data.status -eq "CLOSED" -and $d.data.timeline.Count -ge 2)

# 24. points deduction order
$po = Post "/api/orders" @{ idempotentKey = [guid]::NewGuid().ToString(); fulfillmentType = "PICKUP"; storeId = 1; usePoints = $true; items = @(@{ skuId = $skuId; quantity = 1 }) } $tk
Assert "points deduction order" ($po.code -eq 200 -and $po.data.pointsUsed -ge 0)

Write-Output "---"
if ($script:failed) { Write-Output "SMOKE FAILED" } else { Write-Output "SMOKE ALL PASSED" }
