$ErrorActionPreference = "Stop"

$gradleVersion = "8.13"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bootstrapDir = Join-Path $root ".gradle-bootstrap"
$zipPath = Join-Path $bootstrapDir "gradle-$gradleVersion-bin.zip"
$extractDir = Join-Path $bootstrapDir "dist"
$gradleBat = Join-Path $extractDir "gradle-$gradleVersion\bin\gradle.bat"

New-Item -ItemType Directory -Force -Path $bootstrapDir | Out-Null

if (-not (Test-Path $gradleBat)) {
    Write-Host "Downloading Gradle $gradleVersion..."
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $zipPath

    if (Test-Path $extractDir) {
        Remove-Item -Recurse -Force $extractDir
    }
    New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $extractDir -Force
}

Write-Host "Generating Gradle wrapper..."
& $gradleBat -p $root wrapper --gradle-version $gradleVersion
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

Write-Host "Wrapper generated. Try: .\gradlew.bat :composeApp:run"
