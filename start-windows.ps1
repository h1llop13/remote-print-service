<#
.SYNOPSIS
Starts a Remote Canon Print installation prepared by install-windows.ps1.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$configPath = Join-Path $projectRoot 'config\remote-print-secrets.clixml'

if (-not (Test-Path $configPath)) {
    throw 'Installation settings are missing. Run install-windows.ps1 first.'
}

$configuration = Import-Clixml -Path $configPath
if ($configuration.Version -ne 1) {
    throw 'The installation settings format is not supported. Run install-windows.ps1 again.'
}

if (-not (Test-Path $configuration.JavaPath)) {
    throw 'Java 21 is not available at the configured path. Run install-windows.ps1 again.'
}

$jar = Get-ChildItem (Join-Path $projectRoot 'target') -Filter 'remote-print-service-*.jar' `
    -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notlike '*.original' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $jar) {
    Write-Host 'No packaged application was found. Building it now...' -ForegroundColor Yellow
    Push-Location $projectRoot
    try {
        & .\mvnw.cmd -DskipTests package
        if ($LASTEXITCODE -ne 0) {
            throw "Maven build failed (exit code $LASTEXITCODE)."
        }
    }
    finally {
        Pop-Location
    }
    $jar = Get-ChildItem (Join-Path $projectRoot 'target') -Filter 'remote-print-service-*.jar' |
        Where-Object { $_.Name -notlike '*.original' } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

if (-not $jar) {
    throw 'The application JAR was not created.'
}

$databasePassword = $configuration.DatabaseCredential.GetNetworkCredential().Password
$telegramToken = $configuration.TelegramCredential.GetNetworkCredential().Password

$previousEnvironment = @{}
$environmentValues = @{
    DB_URL                    = $configuration.DatabaseUrl
    DB_USERNAME               = $configuration.DatabaseCredential.UserName
    DB_PASSWORD               = $databasePassword
    TELEGRAM_BOT_TOKEN        = $telegramToken
    TELEGRAM_ALLOWED_CHAT_IDS = $configuration.AllowedChatIds
    SPRING_PROFILES_ACTIVE    = 'windows'
    LIBREOFFICE_PATH          = $configuration.LibreOfficePath
    PRINTER_NAME              = $configuration.PrinterName
}

foreach ($name in $environmentValues.Keys) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
    [Environment]::SetEnvironmentVariable($name, $environmentValues[$name], 'Process')
}

try {
    Set-Location $projectRoot
    Write-Host "Starting Remote Canon Print with the Windows printer profile..." -ForegroundColor Cyan
    & $configuration.JavaPath -jar $jar.FullName
}
finally {
    foreach ($name in $environmentValues.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    $databasePassword = $null
    $telegramToken = $null
}
