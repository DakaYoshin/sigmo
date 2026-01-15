$libsDir = "F:\sigmo\GameServer\lib"
$downloads = @(
    @("https://repo1.maven.org/maven2/com/zaxxer/HikariCP/5.1.0/HikariCP-5.1.0.jar", "HikariCP-5.1.0.jar"),
    @("https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar", "slf4j-api-2.0.9.jar"),
    @("https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar", "logback-classic-1.4.14.jar"),
    @("https://repo1.maven.org/maven2/ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar", "logback-core-1.4.14.jar"),
    @("https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.14.0/commons-lang3-3.14.0.jar", "commons-lang3-3.14.0.jar"),
    @("https://repo1.maven.org/maven2/org/bouncycastle/bcprov-jdk18on/1.77/bcprov-jdk18on-1.77.jar", "bcprov-jdk18on-1.77.jar"),
    @("https://repo1.maven.org/maven2/org/python/jython-standalone/2.7.3/jython-standalone-2.7.3.jar", "jython-standalone-2.7.3.jar"),
    @("https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.35.0/ecj-3.35.0.jar", "ecj-3.35.0.jar"),
    @("https://repo1.maven.org/maven2/org/apache-extras/beanshell/bsh/2.0b6/bsh-2.0b6.jar", "bsh-2.0b6.jar")
)

$legacyFiles = @(
    "c3p0-0.9.1.2.jar",
    "commons-lang-2.1.jar",
    "commons-logging-1.1.jar",
    "bcprov-jdk16-144.jar",
    "bsh-2.0b5.jar",
    "jython.jar",
    "jython-engine-2.2.1.jar",
    "core-3.3.0.jar"
)

Write-Host "Cleaning up legacy libraries..."
foreach ($file in $legacyFiles) {
    if (Test-Path "$libsDir\$file") {
        Remove-Item "$libsDir\$file" -Force
        Write-Host "Removed $file"
    }
}

Write-Host "Downloading modern libraries..."
foreach ($item in $downloads) {
    $url = $item[0]
    $name = $item[1]
    $outPath = "$libsDir\$name"
    Write-Host "Downloading $name..."
    try {
        Invoke-WebRequest -Uri $url -OutFile $outPath
        Write-Host "Successfully downloaded $name"
    } catch {
        Write-Error "Failed to download $name from $url"
        Write-Error $_
    }
}

Write-Host "Library update complete."
