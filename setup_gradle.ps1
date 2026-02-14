$ErrorActionPreference = "Stop"
$logFile = Join-Path $PSScriptRoot "setup_log.txt"
$jarPath = Join-Path $PSScriptRoot "gradle\wrapper\gradle-wrapper.jar"

function Log($msg) { $msg | Out-File $logFile -Append -Encoding utf8 }

Log "=== Start $(Get-Date) ==="

# Check Java
try {
    $javaVer = & java -version 2>&1 | Select-Object -First 1
    Log "Java: $javaVer"
} catch {
    Log "Java not found"
}

# Check if gradle is available
$gradlePath = Get-Command gradle -ErrorAction SilentlyContinue
if ($gradlePath) {
    Log "Gradle found: $($gradlePath.Source)"
    Set-Location $PSScriptRoot
    & gradle wrapper --gradle-version 8.2 2>&1 | ForEach-Object { Log $_ }
} else {
    Log "Gradle not found in PATH"
}

# Check Android Studio
$asPath = "C:\Program Files\Android\Android Studio"
if (Test-Path $asPath) {
    Log "Android Studio found: $asPath"
    $bundledGradle = Get-ChildItem "$asPath\gradle" -Directory | Select-Object -First 1
    if ($bundledGradle) {
        $gradleExe = Join-Path $bundledGradle.FullName "bin\gradle.bat"
        if (Test-Path $gradleExe) {
            Log "Using bundled Gradle: $gradleExe"
            Set-Location $PSScriptRoot
            & $gradleExe wrapper --gradle-version 8.2 2>&1 | ForEach-Object { Log $_ }
        }
    }
} else {
    Log "Android Studio not found at default path"
}

# Try download if jar still missing
if (-not (Test-Path $jarPath)) {
    Log "Trying download..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $ProgressPreference = 'SilentlyContinue'
    try {
        $wc = New-Object System.Net.WebClient
        $wc.DownloadFile("https://github.com/nicoulaj/gradle-wrapper/raw/main/gradle-wrapper.jar", $jarPath)
        Log "Download method 1 done"
    } catch {
        Log "Download method 1 failed: $($_.Exception.Message)"
        try {
            Invoke-WebRequest -Uri "https://github.com/nicoulaj/gradle-wrapper/raw/main/gradle-wrapper.jar" -OutFile $jarPath -UseBasicParsing
            Log "Download method 2 done"
        } catch {
            Log "Download method 2 failed: $($_.Exception.Message)"
        }
    }
}

# Final check
if (Test-Path $jarPath) {
    $size = (Get-Item $jarPath).Length
    Log "SUCCESS: gradle-wrapper.jar exists, size=$size bytes"
} else {
    Log "FAILED: gradle-wrapper.jar not found"
}
Log "=== End ==="
