@echo off
setlocal

set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "GRADLE_HOME=C:\Users\%USERNAME%\.gradle\wrapper\dists\gradle-9.0.0-bin\d6wjpkvcgsg3oed0qlfss3wgl\gradle-9.0.0"

"%JAVA_HOME%\bin\java.exe" -classpath "%GRADLE_HOME%\lib\gradle-launcher-9.0.0.jar" org.gradle.launcher.GradleMain %*

endlocal
