# 檢查 gradle-wrapper.jar 是否存在且包含 GradleWrapperMain
$jar = Join-Path $PSScriptRoot "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $jar)) {
    Write-Host "ERROR: 找不到 $jar"
    exit 1
}
$size = (Get-Item $jar).Length
Write-Host "檔案: $jar"
Write-Host "大小: $size bytes (正常約 55000-65000)"
$content = & jar tf $jar 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: jar 無法讀取，可能已損壞。請重新執行 setup-gradle-wrapper.ps1"
    exit 1
}
if ($content -match "org/gradle/wrapper/GradleWrapperMain\.class") {
    Write-Host "OK: 包含 org.gradle.wrapper.GradleWrapperMain"
} else {
    Write-Host "ERROR: 此 jar 不包含 GradleWrapperMain，不是正確的 gradle-wrapper jar。"
    Write-Host "請刪除 gradle\wrapper\gradle-wrapper.jar 後重新執行 setup-gradle-wrapper.ps1"
    exit 1
}
