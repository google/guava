echo hello
type NUL > artifact.jar
mkdir -p artifacts
echo "hello2" > artifact2.jar
echo "hello3" > artifacts\artifact.jar
echo "hello4" > %KOKORO_ARTIFACTS_DIR%\artifact.jar
