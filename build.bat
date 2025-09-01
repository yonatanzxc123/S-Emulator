@echo off
setlocal enabledelayedexpansion

echo == CLEAN ==
rmdir /s /q build 2>nul
rmdir /s /q dist 2>nul
mkdir build\engine
mkdir build\client
mkdir dist
mkdir dist\lib

echo == VERIFY LIBS ==
set LIBS=jakarta.xml.bind-api.jar jaxb-core.jar jaxb-impl.jar jakarta.activation-api.jar
for %%J in (%LIBS%) do (
  if not exist "lib\%%J" (
    echo Missing lib\%%J
    exit /b 1
  )
)

echo == COPY LIBS ==
xcopy /y /q lib\*.jar dist\lib\ >nul

echo == SCAN SOURCES ==
dir /b /s engine\src\*.java > build\engine-sources.txt
dir /b /s client\src\*.java > build\client-sources.txt

echo == COMPILE ENGINE ==
javac -encoding UTF-8 -cp "dist\lib\*" -d build\engine @build\engine-sources.txt || goto :halt

echo == PACKAGE ENGINE ==
jar --create --file dist\engine.jar -C build\engine .

echo == COMPILE CLIENT ==
javac -encoding UTF-8 -cp "build\engine;dist\lib\*" -d build\client @build\client-sources.txt || goto :halt

echo == PACKAGE CLIENT ==
>build\manifest.mf (
  echo Manifest-Version: 1.0
  echo Main-Class: ui.Main
  echo Class-Path: engine.jar lib/jakarta.xml.bind-api.jar lib/jaxb-core.jar lib/jaxb-impl.jar lib/jakarta.activation-api.jar
)
jar --create --file dist\client.jar --manifest build\manifest.mf -C build\client .

echo == DONE ==
exit /b 0

:halt
echo.
echo COMPILE FAILED (see errors above)
exit /b 1
