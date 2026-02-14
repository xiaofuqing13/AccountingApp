$logFile = "c:\Users\Administrator\Desktop\AccountingApp\dl_log.txt"
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$wrapperJar = "c:\Users\Administrator\Desktop\AccountingApp\gradle\wrapper\gradle-wrapper.jar"

try {
    "Attempting download..." | Out-File $logFile
    $url = "https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar"
    $wc = New-Object System.Net.WebClient
    $wc.DownloadFile($url, $wrapperJar)
    if (Test-Path $wrapperJar) {
        $size = (Get-Item $wrapperJar).Length
        "SUCCESS: Downloaded $size bytes" | Out-File $logFile -Append
    } else {
        "FAIL: File not created" | Out-File $logFile -Append
    }
} catch {
    ("ERROR: " + $_.Exception.Message) | Out-File $logFile -Append
    
    try {
        "Trying alternate URL..." | Out-File $logFile -Append
        $url2 = "https://github.com/nicoulaj/gradle-wrapper/raw/main/gradle-wrapper.jar"
        $wc2 = New-Object System.Net.WebClient
        $wc2.DownloadFile($url2, $wrapperJar)
        if (Test-Path $wrapperJar) {
            $size = (Get-Item $wrapperJar).Length
            "SUCCESS (alt): Downloaded $size bytes" | Out-File $logFile -Append
        }
    } catch {
        ("ERROR2: " + $_.Exception.Message) | Out-File $logFile -Append
    }
}
