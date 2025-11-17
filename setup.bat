@echo off
REM ----------------------------------------------------------
REM setup.bat
REM Automatically generate IDE run configs for Forge 1.20.1
REM Detects IntelliJ or Eclipse and runs the proper Gradle task
REM ----------------------------------------------------------

REM Detect IDE choice via environment variable
SET "IDE=%1"

IF "%IDE%"=="" (
    ECHO No IDE specified. Defaulting to IntelliJ.
    SET "IDE=intellij"
)

REM Determine gradlew executable
IF EXIST gradlew.bat (
    SET "GRADLEW=gradlew.bat"
) ELSE (
    ECHO ERROR: gradlew.bat not found! Please run from project root.
    EXIT /B 1
)

REM Run the appropriate Gradle task
IF /I "%IDE%"=="intellij" (
    ECHO Generating IntelliJ run configs...
    %GRADLEW% genIntellijRuns --refresh-dependencies
) ELSE IF /I "%IDE%"=="eclipse" (
    ECHO Generating Eclipse run configs...
    %GRADLEW% genEclipseRuns --refresh-dependencies
) ELSE (
    ECHO Unknown IDE specified: %IDE%
    ECHO Valid options: intellij, eclipse
    EXIT /B 1
)

ECHO Done.
PAUSE
