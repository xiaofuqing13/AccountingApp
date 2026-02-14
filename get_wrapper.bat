@echo off
set JAR_PATH=%~dp0gradle\wrapper\gradle-wrapper.jar
set LOG_PATH=%~dp0wrapper_log.txt

echo Checking environment... > "%LOG_PATH%"

:: Try to find Android Studio bundled Gradle
for /d %%G in ("C:\Program Files\Android\Android Studio\gradle\gradle-*") do (
    if exist "%%G\lib\gradle-wrapper-*.jar" (
        echo Found Android Studio Gradle: %%G >> "%LOG_PATH%"
        for %%J in ("%%G\lib\gradle-wrapper-*.jar") do (
            copy "%%J" "%JAR_PATH%" >nul 2>&1
            echo Copied from: %%J >> "%LOG_PATH%"
            goto :done
        )
    )
)

:: Try PowerShell download
echo Trying PowerShell download... >> "%LOG_PATH%"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; try { (New-Object Net.WebClient).DownloadFile('https://services.gradle.org/distributions/gradle-8.2-all.zip','%TEMP%\g82.zip'); Add-Type -A 'System.IO.Compression.FileSystem'; $z=[IO.Compression.ZipFile]::OpenRead('%TEMP%\g82.zip'); $e=$z.Entries|Where-Object{$_.Name -like 'gradle-wrapper*.jar'}|Select -First 1; if($e){$s=$e.Open();$f=[IO.File]::Create('%JAR_PATH%');$s.CopyTo($f);$f.Close();$s.Close();'OK'|Out-File '%LOG_PATH%' -Append}; $z.Dispose(); Remove-Item '%TEMP%\g82.zip' -Force } catch { $_.Exception.Message|Out-File '%LOG_PATH%' -Append }"

:done
if exist "%JAR_PATH%" (
    echo SUCCESS >> "%LOG_PATH%"
) else (
    echo FAILED >> "%LOG_PATH%"
)
