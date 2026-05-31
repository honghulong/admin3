$body = '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'

Write-Host "Request: $body"
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/admin3/mcp/message" -Method Post -ContentType "application/json" -Body $body -UseBasicParsing
    Write-Host "Status: $($response.StatusCode)"
    Write-Host "Headers:"
    $response.Headers | Format-Table
    Write-Host "Response:"
    Write-Host $response.Content
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    try {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body: $responseBody"
    } catch {
        Write-Host "Response Body: (could not read)"
    }
}

# Also test with curl to see raw response
Write-Host ""
Write-Host "=== Testing with curl ==="
$tempFile = [System.IO.Path]::GetTempFileName()
$body | Out-File -FilePath $tempFile -Encoding ascii
$result = & curl.exe -s -w "`n%{http_code}" -X POST "http://localhost:8080/admin3/mcp/message" -H "Content-Type: application/json" -d "@$tempFile" 2>&1
Write-Host "Curl result: $result"
Remove-Item $tempFile -Force
