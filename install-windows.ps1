<#
.SYNOPSIS
Installs and configures Remote Canon Print on Windows.

.DESCRIPTION
Installs Java 21, Git, LibreOffice and PostgreSQL 18 only when they are absent,
creates the remote_print database, and saves the small set of private settings
needed to run the service. Secrets are stored with Windows DPAPI for the account
that runs this script; they are never written to Git or to plain-text config.

Run from a cloned repository, or download this file and run it by itself. In the
latter case it clones the public repository into %LOCALAPPDATA%\RemoteCanonPrint.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\install-windows.ps1
#>

[CmdletBinding()]
param(
    [string]$InstallDirectory = (Join-Path $env:LOCALAPPDATA 'RemoteCanonPrint'),
    [string]$RepositoryUrl = 'https://github.com/h1llop13/remote-print-service.git',
    [string]$PrinterName = 'Canon LBP2900',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

function Write-Step {
    param([string]$Message)
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Write-Ok {
    param([string]$Message)
    Write-Host "    OK  $Message" -ForegroundColor Green
}

function Write-WarningMessage {
    param([string]$Message)
    Write-Host "    !   $Message" -ForegroundColor Yellow
}

function Test-Administrator {
    $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = [Security.Principal.WindowsPrincipal]::new($identity)
    return $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Invoke-WingetInstall {
    param(
        [Parameter(Mandatory)] [string]$PackageId,
        [string[]]$AdditionalArguments = @()
    )

    if (-not (Get-Command winget.exe -ErrorAction SilentlyContinue)) {
        throw 'winget (App Installer) is required to install missing components. Install or update "App Installer" from Microsoft Store, then run this script again.'
    }

    Write-Host "    Installing $PackageId ..."
    & winget.exe install --id $PackageId --exact --source winget `
        --accept-source-agreements --accept-package-agreements `
        --disable-interactivity @AdditionalArguments

    if ($LASTEXITCODE -ne 0) {
        throw "winget could not install $PackageId (exit code $LASTEXITCODE)."
    }
}

function Get-Java21Path {
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) {
        $version = (& $command.Source --version 2>&1 | Select-Object -First 1)
        if ($LASTEXITCODE -eq 0 -and $version -match '(?m)^(openjdk|java) 21[.\s]') {
            return $command.Source
        }
    }

    $candidates = @()
    $candidates += Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Filter java.exe -Recurse -ErrorAction SilentlyContinue
    $candidates += Get-ChildItem 'C:\Program Files\Java' -Filter java.exe -Recurse -ErrorAction SilentlyContinue
    $candidates = $candidates | Where-Object { $_.FullName -match '\\jdk-?21' }

    return ($candidates | Sort-Object FullName -Descending | Select-Object -First 1).FullName
}

function Get-GitPath {
    $command = Get-Command git.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidate = 'C:\Program Files\Git\cmd\git.exe'
    if (Test-Path $candidate) {
        return $candidate
    }

    return $null
}

function Get-LibreOfficePath {
    $candidates = @(
        'C:\Program Files\LibreOffice\program\soffice.exe',
        'C:\Program Files (x86)\LibreOffice\program\soffice.exe'
    )

    return ($candidates | Where-Object { Test-Path $_ } | Select-Object -First 1)
}

function Get-PsqlPath {
    $command = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $candidates = Get-ChildItem 'C:\Program Files\PostgreSQL' -Filter psql.exe -Recurse -ErrorAction SilentlyContinue
    return ($candidates | Sort-Object FullName -Descending | Select-Object -First 1).FullName
}

function Read-RequiredSecret {
    param([Parameter(Mandatory)] [string]$Prompt)

    do {
        $value = Read-Host -Prompt $Prompt -AsSecureString
        if ($value.Length -eq 0) {
            Write-WarningMessage 'A value is required.'
        }
    } while ($value.Length -eq 0)

    return $value
}

function ConvertTo-PlainText {
    param([Parameter(Mandatory)] [Security.SecureString]$Value)

    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Get-InstalledPrinterNames {
    if (Get-Command Get-Printer -ErrorAction SilentlyContinue) {
        return @(Get-Printer -ErrorAction Stop | ForEach-Object Name)
    }

    return @(Get-CimInstance Win32_Printer -ErrorAction Stop | ForEach-Object Name)
}

function Invoke-Psql {
    param(
        [Parameter(Mandatory)] [string]$PsqlPath,
        [Parameter(Mandatory)] [string]$Password,
        [Parameter(Mandatory)] [string]$Sql
    )

    $oldPassword = $env:PGPASSWORD
    try {
        $env:PGPASSWORD = $Password
        $result = & $PsqlPath --no-password --host localhost --port 5432 `
            --username postgres --dbname postgres -v ON_ERROR_STOP=1 -Atqc $Sql 2>&1
        if ($LASTEXITCODE -ne 0) {
            throw "PostgreSQL command failed: $result"
        }
        return $result
    }
    finally {
        if ($null -eq $oldPassword) {
            Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
        }
        else {
            $env:PGPASSWORD = $oldPassword
        }
    }
}

if ($env:OS -ne 'Windows_NT') {
    throw 'This installer can only be run on Windows.'
}

if (-not (Test-Administrator)) {
    Write-Host 'Administrator permission is required to install Windows components.' -ForegroundColor Yellow
    Write-Host 'Approve the Windows UAC prompt to continue the installation.'
    $elevatedArguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', ('"{0}"' -f $PSCommandPath)
    )
    if ($PSBoundParameters.ContainsKey('InstallDirectory')) {
        $elevatedArguments += '-InstallDirectory', ('"{0}"' -f $InstallDirectory)
    }
    if ($PSBoundParameters.ContainsKey('RepositoryUrl')) {
        $elevatedArguments += '-RepositoryUrl', ('"{0}"' -f $RepositoryUrl)
    }
    if ($PSBoundParameters.ContainsKey('PrinterName')) {
        $elevatedArguments += '-PrinterName', ('"{0}"' -f $PrinterName)
    }
    if ($SkipBuild) {
        $elevatedArguments += '-SkipBuild'
    }
    Start-Process -FilePath powershell.exe -Verb RunAs -ArgumentList ($elevatedArguments -join ' ')
    exit
}

Write-Step 'Collecting the private settings'
$postgresAlreadyInstalled = [bool](Get-PsqlPath)
$databasePassword = Read-RequiredSecret 'PostgreSQL password for the postgres account'
$telegramToken = Read-RequiredSecret 'Telegram bot token'

do {
    $allowedChatIds = Read-Host 'Allowed Telegram Chat ID(s), comma-separated'
    if ($allowedChatIds -notmatch '^\s*-?\d+(\s*,\s*-?\d+)*\s*$') {
        Write-WarningMessage 'Enter numeric IDs separated by commas, for example: 1409371857,987654321'
    }
} while ($allowedChatIds -notmatch '^\s*-?\d+(\s*,\s*-?\d+)*\s*$')

# The PostgreSQL installer receives the password as an argument only on a new
# installation. A double quote would make that installer argument ambiguous.
$databasePasswordPlain = ConvertTo-PlainText $databasePassword
if (-not $postgresAlreadyInstalled -and $databasePasswordPlain.Contains('"')) {
    throw 'The PostgreSQL password cannot contain a double quote (") when PostgreSQL must be installed by this script.'
}

Write-Step 'Checking Java 21'
$javaPath = Get-Java21Path
if (-not $javaPath) {
    Invoke-WingetInstall -PackageId 'EclipseAdoptium.Temurin.21.JDK' -AdditionalArguments @('--silent')
    $javaPath = Get-Java21Path
}
if (-not $javaPath) {
    throw 'Java 21 was installed but could not be found. Open a new PowerShell window and run the installer again.'
}
Write-Ok "Java: $javaPath"

Write-Step 'Checking Git'
$gitPath = Get-GitPath
if (-not $gitPath) {
    Invoke-WingetInstall -PackageId 'Git.Git' -AdditionalArguments @('--silent')
    $gitPath = Get-GitPath
}
if (-not $gitPath) {
    throw 'Git was installed but could not be found. Open a new PowerShell window and run the installer again.'
}
Write-Ok "Git: $gitPath"

Write-Step 'Checking LibreOffice'
$libreOfficePath = Get-LibreOfficePath
if (-not $libreOfficePath) {
    Invoke-WingetInstall -PackageId 'TheDocumentFoundation.LibreOffice' -AdditionalArguments @('--silent')
    $libreOfficePath = Get-LibreOfficePath
}
if (-not $libreOfficePath) {
    throw 'LibreOffice was installed but soffice.exe could not be found.'
}
Write-Ok "LibreOffice: $libreOfficePath"

Write-Step 'Checking PostgreSQL 18'
$psqlPath = Get-PsqlPath
if (-not $psqlPath) {
    $postgresInstallerArguments = @(
        '--silent',
        '--override',
        "--mode unattended --superpassword `"$databasePasswordPlain`" --serverport 5432"
    )
    Invoke-WingetInstall -PackageId 'PostgreSQL.PostgreSQL.18' -AdditionalArguments $postgresInstallerArguments
    $psqlPath = Get-PsqlPath
}
if (-not $psqlPath) {
    throw 'PostgreSQL was installed but psql.exe could not be found.'
}

$postgresService = Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue |
    Sort-Object Name -Descending | Select-Object -First 1
if ($postgresService -and $postgresService.Status -ne 'Running') {
    Start-Service -Name $postgresService.Name
}

$pgIsReadyPath = Join-Path (Split-Path $psqlPath -Parent) 'pg_isready.exe'
if (Test-Path $pgIsReadyPath) {
    $ready = $false
    foreach ($attempt in 1..30) {
        & $pgIsReadyPath --host localhost --port 5432 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            $ready = $true
            break
        }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) {
        throw 'PostgreSQL did not become ready on localhost:5432. Check that port and the PostgreSQL service.'
    }
}
Write-Ok "PostgreSQL client: $psqlPath"

Write-Step 'Preparing the remote_print database'
$databaseExists = Invoke-Psql -PsqlPath $psqlPath -Password $databasePasswordPlain `
    -Sql "SELECT 1 FROM pg_database WHERE datname = 'remote_print';"
if (($databaseExists | Out-String).Trim() -ne '1') {
    Invoke-Psql -PsqlPath $psqlPath -Password $databasePasswordPlain `
        -Sql 'CREATE DATABASE remote_print;' | Out-Null
    Write-Ok 'Created database remote_print'
}
else {
    Write-Ok 'Database remote_print already exists'
}

Write-Step 'Getting the application files'
if (Test-Path (Join-Path $PSScriptRoot 'pom.xml')) {
    $projectRoot = $PSScriptRoot
    Write-Ok "Using the current repository: $projectRoot"
}
else {
    $projectRoot = Join-Path $InstallDirectory 'remote-print-service'
    if (Test-Path (Join-Path $projectRoot 'pom.xml')) {
        Write-Ok "Using existing installation: $projectRoot"
    }
    elseif (Test-Path $projectRoot) {
        throw "Refusing to overwrite $projectRoot because it is not a Remote Canon Print repository. Choose another -InstallDirectory."
    }
    else {
        New-Item -ItemType Directory -Path $InstallDirectory -Force | Out-Null
        & $gitPath clone --depth 1 $RepositoryUrl $projectRoot
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not download Remote Canon Print from GitHub.'
        }
        Write-Ok "Downloaded application to $projectRoot"
    }
}

if (-not (Test-Path (Join-Path $projectRoot 'mvnw.cmd')) -or
    -not (Test-Path (Join-Path $projectRoot 'start-windows.ps1'))) {
    throw 'The downloaded repository is incomplete. Download install-windows.ps1 from the current Remote Canon Print release and run it again.'
}

$configDirectory = Join-Path $projectRoot 'config'
$configPath = Join-Path $configDirectory 'remote-print-secrets.clixml'
New-Item -ItemType Directory -Path $configDirectory -Force | Out-Null

$configuration = [PSCustomObject]@{
    Version            = 1
    DatabaseUrl        = 'jdbc:postgresql://localhost:5432/remote_print'
    DatabaseCredential = [PSCredential]::new('postgres', $databasePassword)
    TelegramCredential = [PSCredential]::new('telegram', $telegramToken)
    AllowedChatIds     = $allowedChatIds
    PrinterName        = $PrinterName
    LibreOfficePath    = $libreOfficePath
    JavaPath           = $javaPath
}
$configuration | Export-Clixml -Path $configPath -Force

# Only this Windows account can decrypt the credentials saved by Export-Clixml.
& icacls.exe $configDirectory /inheritance:r /grant:r "${env:USERNAME}:(OI)(CI)F" /grant:r 'Administrators:(OI)(CI)F' | Out-Null
Write-Ok 'Saved encrypted private settings for this Windows account'

Write-Step 'Checking the printer'
try {
    $installedPrinters = Get-InstalledPrinterNames
    $matchingPrinter = $installedPrinters | Where-Object { $_ -ieq $PrinterName } | Select-Object -First 1
    if ($matchingPrinter) {
        Write-Ok "Printer found: $matchingPrinter"
    }
    else {
        Write-WarningMessage "Printer '$PrinterName' was not found. Install/connect its Windows driver before printing."
        if ($installedPrinters.Count -gt 0) {
            Write-Host "    Installed printers: $($installedPrinters -join '; ')"
        }
    }
}
catch {
    Write-WarningMessage "Could not list printers: $($_.Exception.Message)"
}

if (-not $SkipBuild) {
    Write-Step 'Downloading application dependencies and building the service'
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
    Write-Ok 'Application built successfully'
}

$databasePasswordPlain = $null

Write-Host "`nInstallation complete." -ForegroundColor Green
Write-Host "Start the service with:"
Write-Host "  powershell -ExecutionPolicy Bypass -File `"$projectRoot\start-windows.ps1`""
Write-Host 'Keep that window open while the Telegram bot should receive print jobs.'
