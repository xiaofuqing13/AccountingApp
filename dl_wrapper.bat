@echo off
curl.exe -L -o "%~dp0gradle\wrapper\gradle-wrapper.jar" "https://github.com/nicoulaj/gradle-wrapper/raw/main/gradle-wrapper.jar" --connect-timeout 30
if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
    echo SUCCESS > "%~dp0dl_result.txt"
) else (
    echo FAIL > "%~dp0dl_result.txt"
)
