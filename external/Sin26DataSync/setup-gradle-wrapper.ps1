# 下載 Gradle 8.5 並擷取 gradle-wrapper.jar 到 gradle/wrapper/
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$projectRoot = $PSScriptRoot
if (-not $projectRoot) { $projectRoot = (Get-Location).Path }
$projectRoot = $projectRoot.TrimEnd('\', '/')

$wrapperDir = Join-Path $projectRoot "gradle\wrapper"
$destJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
$zipUrl = "https://services.gradle.org/distributions/gradle-8.5-bin.zip"
$zipPath = Join-Path $env:TEMP "gradle-8.5-bin.zip"

if (-not (Test-Path $wrapperDir)) {
    New-Item -ItemType Directory -Path $wrapperDir -Force | Out-Null
}

Write-Host "Target JAR: $destJar"
Write-Host "Downloading Gradle 8.5 (may take 1-2 min)..."
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Invoke-WebRequest -Uri $zipUrl -OutFile $zipPath -UseBasicParsing

Write-Host "Extracting gradle-wrapper.jar from zip..."
$zip = [System.IO.Compression.ZipFile]::OpenRead($zipPath)
try {
    $entry = $zip.Entries | Where-Object { $_.Name -like "gradle-wrapper*.jar" } | Select-Object -First 1
    if (-not $entry) {
        Write-Host "ERROR: gradle-wrapper*.jar not found in zip. Entries:"
        $zip.Entries | ForEach-Object { $_.FullName } | Select-Object -First 20
        exit 1
    }
    $stream = $entry.Open()
    try {
        $fs = [System.IO.File]::Create($destJar)
        try {
            $stream.CopyTo($fs)
        } finally { $fs.Close() }
    } finally { $stream.Close() }
    $size = (Get-Item $destJar).Length
    Write-Host "Installed: $destJar ($size bytes)"
    if ($size -lt 50000) { Write-Host "WARNING: jar may be incomplete (expected ~60KB)."; exit 1 }
} finally {
    $zip.Dispose()
}
Remove-Item $zipPath -Force -ErrorAction SilentlyContinue
Write-Host "Done. Run: .\gradlew.bat build"
