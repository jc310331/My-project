$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
java -version
if ($LASTEXITCODE -ne 0) { throw "Install a Java 17 JDK and set JAVA_HOME first." }
& .\gradlew.bat --no-daemon clean build
if ($LASTEXITCODE -ne 0) { throw "Build failed. See Gradle output above." }
Write-Host "Compiled JAR: build\libs\socrates-0.3.2.jar"
Write-Host "Optional engine tests: .\gradlew.bat runGameTestServer"
