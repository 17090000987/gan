set OUT_PATH=.\package
#xcopy jni %OUT_PATH%\jni\ /s/d/e
xcopy libs %OUT_PATH%\libs\ /s/d/e
xcopy public/player %OUT_PATH%\public\player\ /s/d/e
#xcopy assets %OUT_PATH%\assets\ /s/d/e
copy envsetup.sh %OUT_PATH%\
copy start.sh %OUT_PATH%\
copy start.bat %OUT_PATH%\
copy README.md %OUT_PATH%\
copy server\src\main\resources\application.yml %OUT_PATH%\application.yml.tpl
copy server\target\gan*.jar %OUT_PATH%\