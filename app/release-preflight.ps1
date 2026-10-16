# Run from the Android Studio Terminal:
# powershell -NoProfile -File .\app\release-preflight.ps1
# This script reads source files only and never signs or changes an APK.
$ErrorActionPreference = 'Stop'
$App = Split-Path -Parent $MyInvocation.MyCommand.Path
$ManifestPath = Join-Path $App 'src\main\AndroidManifest.xml'
$StringsPath = Join-Path $App 'src\main\res\values\strings.xml'
$LegacyBackupPath = Join-Path $App 'src\main\res\xml\backup_rules.xml'
$ModernBackupPath = Join-Path $App 'src\main\res\xml\data_extraction_rules.xml'
$GradlePath = Join-Path $App 'build.gradle.kts'
function Assert-Tgf([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "Release preflight FAIL: $Message" }
    Write-Host "OK: $Message"
}
[xml]$Manifest = Get-Content -LiteralPath $ManifestPath -Raw -Encoding UTF8
[xml]$Strings = Get-Content -LiteralPath $StringsPath -Raw -Encoding UTF8
[xml]$LegacyBackup = Get-Content -LiteralPath $LegacyBackupPath -Raw -Encoding UTF8
[xml]$ModernBackup = Get-Content -LiteralPath $ModernBackupPath -Raw -Encoding UTF8
$Gradle = Get-Content -LiteralPath $GradlePath -Raw -Encoding UTF8
$Ns = 'http://schemas.android.com/apk/res/android'
$AppNode = $Manifest.manifest.application
Assert-Tgf ($AppNode.GetAttribute('allowBackup', $Ns) -eq 'false') 'System auto backup is disabled'
Assert-Tgf ($AppNode.GetAttribute('label', $Ns) -eq '@string/app_name') 'App label uses the resource string'
Assert-Tgf ($AppNode.GetAttribute('dataExtractionRules', $Ns) -eq '@xml/data_extraction_rules') 'Android 12+ backup policy linked'
Assert-Tgf ($AppNode.GetAttribute('fullBackupContent', $Ns) -eq '@xml/backup_rules') 'Android 11- backup policy linked'
$AppNameNode = $Strings.SelectSingleNode('/resources/string[@name="app_name"]')
Assert-Tgf (($null -ne $AppNameNode) -and ($AppNameNode.InnerText -eq '图工坊')) 'Chinese launcher name 图工坊 exists'
$CodeMatch = [regex]::Match($Gradle, 'versionCode\s*=\s*(\d+)')
$NameMatch = [regex]::Match($Gradle, 'versionName\s*=\s*"1\.0\.(\d+)"')
Assert-Tgf ($CodeMatch.Success -and $NameMatch.Success -and
    [int]$CodeMatch.Groups[1].Value -ge 31 -and
    $CodeMatch.Groups[1].Value -eq $NameMatch.Groups[1].Value) 'Version name/code match (1.0.N)'
Assert-Tgf ($Gradle -match 'applicationId\s*=\s*"com\.tugongfang\.editor"') 'Existing app ID is unchanged'
Assert-Tgf ($LegacyBackup.SelectNodes('/full-backup-content/exclude').Count -ge 3) 'Legacy full-backup excludes private data'
Assert-Tgf ($ModernBackup.SelectNodes('/data-extraction-rules/cloud-backup/exclude').Count -ge 3) 'Cloud backup excludes private data'
Assert-Tgf ($ModernBackup.SelectNodes('/data-extraction-rules/device-transfer/exclude').Count -ge 3) 'Device-transfer backup excludes private data'
$Permissions = $Manifest.SelectNodes('/manifest/uses-permission')
Assert-Tgf ($Permissions.Count -eq 0) 'App requests no direct manifest permissions'
# XML XPath doesn't bind the android prefix automatically; use the namespace attribute directly.
$ProviderNode = @($AppNode.provider) | Where-Object { $_.GetAttribute('name', $Ns) -eq 'androidx.core.content.FileProvider' } | Select-Object -First 1
Assert-Tgf ($null -ne $ProviderNode -and
    $ProviderNode.GetAttribute('exported', $Ns) -eq 'false' -and
    $ProviderNode.GetAttribute('grantUriPermissions', $Ns) -eq 'true' -and
    $ProviderNode.GetAttribute('authorities', $Ns) -eq '${applicationId}.fileprovider') 'Camera file provider is private and can grant temporary URIs'
$CameraPaths = Join-Path $App 'src\main\res\xml\camera_file_paths.xml'
[xml]$Camera = Get-Content -LiteralPath $CameraPaths -Raw -Encoding UTF8
$Paths = $Camera.SelectNodes('/paths/cache-path')
Assert-Tgf ($Paths.Count -eq 1 -and $Paths[0].GetAttribute('path') -eq 'tgf_camera/') 'Camera URI shares only dedicated cache folder'
Write-Host 'Release source configuration checks passed.' -ForegroundColor Green
Write-Host 'Reminder: this is NOT a Gradle build, signed APK, privacy audit or device test.'
