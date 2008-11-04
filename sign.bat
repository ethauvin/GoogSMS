@echo off
"%JAVA_HOME%\bin\jarsigner" -verbose -keystore "%USERPROFILE%\personal\android.keystore" "%1" android
if errorlevel 1 goto ERROR
"%JAVA_HOME%\bin\jarsigner" -verify "%1"
if errorlevel 1 goto ERROR
goto DONE
:ERROR
@pause
:DONE
@echo on