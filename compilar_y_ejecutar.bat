@echo off
chcp 65001 > nul
if exist out rmdir /s /q out
javac -encoding UTF-8 -sourcepath src -d out src\co\edu\iudigital\tecnomovil\Main.java src\co\edu\iudigital\tecnomovil\PruebasFuncionales.java
if errorlevel 1 goto fin
java -Dstdout.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.PruebasFuncionales
java -Dstdout.encoding=UTF-8 -cp out co.edu.iudigital.tecnomovil.Main %*
:fin
pause
