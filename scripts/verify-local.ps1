param([string]$BaseUrl = 'http://127.0.0.1:5173/api/v1')
$ErrorActionPreference = 'Stop'
# Local demo accounts only. No model calls or pre-existing records are modified.
function Api([string]$Method, [string]$Path, [hashtable]$Headers = @{}, $Body = $null) {
    $options = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $Headers; TimeoutSec = 15 }
    if ($null -ne $Body) { $options.ContentType = 'application/json'; $options.Body = $Body | ConvertTo-Json -Depth 5 }
    return (Invoke-RestMethod @options).data
}
function ExpectStatus([int]$Status, [scriptblock]$Action) {
    try { & $Action | Out-Null; throw "Expected HTTP $Status" }
    catch { if ($_.Exception.Response.StatusCode.value__ -ne $Status) { throw } }
}
$demo = Api POST '/auth/login' @{} @{ username = 'demo'; password = 'demo123' }
$headers = @{ Authorization = "Bearer $($demo.token)" }
$other = Api POST '/auth/login' @{} @{ username = 'xiaomei'; password = 'demo123' }
$otherHeaders = @{ Authorization = "Bearer $($other.token)" }
$admin = Api POST '/auth/login' @{} @{ username = 'admin'; password = 'admin123' }
$adminHeaders = @{ Authorization = "Bearer $($admin.token)" }
$diaryId = $null; $sessionId = $null; $fileId = $null
try {
    if ((Api GET '/health').status -ne 'UP') { throw 'Backend is not healthy' }
    ExpectStatus 403 { Api GET '/admin/diaries' $headers }
    ExpectStatus 403 { Api DELETE '/admin/diaries/0' $headers }
    ExpectStatus 403 { Api GET '/admin/risk-events' $headers }
    'PASS: ordinary users cannot access administrative data'

    $date = [datetime]'2099-01-01'
    do { $date = $date.AddDays(1); $day = $date.ToString('yyyy-MM-dd'); $existing = Api GET "/diaries?fromDate=$day&toDate=$day" $headers } while ($existing.total -gt 0)
    $body = @{ diaryDate=$day; moodScore=6; dominantEmotion='平静'; emotionTriggers='本地接口测试'; content='临时测试记录'; sleepQuality=3; stressLevel=2 }
    $diary = Api POST '/diaries' $headers $body; $diaryId = $diary.id
    ExpectStatus 404 { Api GET "/diaries/$diaryId" $otherHeaders }
    $body.moodScore = 8
    if ((Api PUT "/diaries/$diaryId" $headers $body).moodScore -ne 8) { throw 'Diary update failed' }
    Api DELETE "/diaries/$diaryId" $headers | Out-Null
    ExpectStatus 404 { Api GET "/diaries/$diaryId" $headers }
    $diaryId = $null
    'PASS: diary create/update/delete and cross-user isolation'

    $session = Api POST '/chat/sessions' $headers @{ title='临时验证会话' }; $sessionId=$session.id
    if ((Api PATCH "/chat/sessions/$sessionId" $headers @{title='临时重命名'}).title -ne '临时重命名') { throw 'Session rename failed' }
    ExpectStatus 404 { Api GET "/chat/sessions/$sessionId" $otherHeaders }
    Api DELETE "/chat/sessions/$sessionId" $headers | Out-Null
    ExpectStatus 404 { Api GET "/chat/sessions/$sessionId" $headers }
    $sessionId = $null
    'PASS: session rename/delete and ownership checks'

    $preflight = Invoke-WebRequest -Uri "$BaseUrl/users/me" -Method Options -Headers @{Origin='http://127.0.0.1:5173';'Access-Control-Request-Method'='PATCH'} -TimeoutSec 10
    if ($preflight.Headers['Access-Control-Allow-Methods'] -notmatch 'PATCH') { throw 'PATCH is missing from CORS policy' }
    'PASS: PATCH preflight'

    $imagePath = Join-Path $PSScriptRoot '../frontend/src/assets/images/smile.png'
    $uploaded = Invoke-RestMethod -Uri "$BaseUrl/files" -Method Post -Headers $adminHeaders -Form @{file=Get-Item -LiteralPath $imagePath;businessType='ARTICLE'}
    $fileId = $uploaded.data.fileId
    $image = Invoke-WebRequest -Uri "$BaseUrl/files/$fileId"
    if ($image.Headers['Content-Type'] -notmatch 'image/png' -or $image.Headers['X-Content-Type-Options'] -ne 'nosniff') { throw 'Unsafe file response' }
    Api DELETE "/files/$fileId" $adminHeaders | Out-Null; $fileId=$null
    'PASS: validated image upload and safe response headers'

    $events = Api GET '/admin/risk-events?size=1' $adminHeaders
    if ($events.items.Count -gt 0) {
        $eventId=$events.items[0].id
        ExpectStatus 400 { Api GET "/admin/risk-events/$eventId" $adminHeaders }
        Api GET "/admin/risk-events/$eventId`?reason=local-smoke-verification" $adminHeaders | Out-Null
    }
    'PASS: risk workspace permissions and access reason'

    ExpectStatus 400 { Api POST '/auth/register' @{} @{username='missing-consent-test';password='fixture123';email='fixture@example.invalid'} }
    'PASS: registration requires current privacy consent'

    $rotated = Api POST '/auth/refresh' $headers
    ExpectStatus 401 { Api GET '/users/me' $headers }
    $headers = @{Authorization="Bearer $($rotated.token)"}
    Api GET '/users/me' $headers | Out-Null
    Api POST '/auth/logout' $headers | Out-Null
    ExpectStatus 401 { Api GET '/users/me' $headers }
    'PASS: token rotation and logout revoke old credentials'
} finally {
    if ($diaryId) { try { Api DELETE "/diaries/$diaryId" $headers | Out-Null } catch {} }
    if ($sessionId) { try { Api DELETE "/chat/sessions/$sessionId" $headers | Out-Null } catch {} }
    if ($fileId) { try { Api DELETE "/files/$fileId" $adminHeaders | Out-Null } catch {} }
    foreach ($loginHeaders in @($headers, $otherHeaders, $adminHeaders)) { try { Api POST '/auth/logout' $loginHeaders | Out-Null } catch {} }
}
