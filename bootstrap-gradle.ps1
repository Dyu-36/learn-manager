$ErrorActionPreference = "Stop"

$gradleVersion = "8.13"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$bootstrapDir = Join-Path $root ".gradle-bootstrap"
$zipPath = Join-Path $bootstrapDir "gradle-$gradleVersion-bin.zip"
$extractDir = Join-Path $bootstrapDir "dist"
$gradleBat = Join-Path $extractDir "gradle-$gradleVersion\bin\gradle.bat"
$wrapperProject = Join-Path $bootstrapDir "wrapper-project"

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

if (Test-Path $wrapperProject) {
    Remove-Item -Recurse -Force $wrapperProject
}
New-Item -ItemType Directory -Force -Path $wrapperProject | Out-Null
Set-Content -Path (Join-Path $wrapperProject "settings.gradle.kts") -Value 'rootProject.name = "wrapper-bootstrap"' -Encoding UTF8

Write-Host "Generating Gradle wrapper in an isolated bootstrap project..."
& $gradleBat -p $wrapperProject wrapper --gradle-version $gradleVersion
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

New-Item -ItemType Directory -Force -Path (Join-Path $root "gradle\wrapper") | Out-Null
Copy-Item (Join-Path $wrapperProject "gradlew") (Join-Path $root "gradlew") -Force
Copy-Item (Join-Path $wrapperProject "gradlew.bat") (Join-Path $root "gradlew.bat") -Force
Copy-Item (Join-Path $wrapperProject "gradle\wrapper\gradle-wrapper.jar") (Join-Path $root "gradle\wrapper\gradle-wrapper.jar") -Force
Copy-Item (Join-Path $wrapperProject "gradle\wrapper\gradle-wrapper.properties") (Join-Path $root "gradle\wrapper\gradle-wrapper.properties") -Force

Write-Host "Wrapper generated. Try: .\gradlew.bat :composeApp:run"
