#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
T=$(mktemp -d); trap 'rm -rf "$T"' EXIT
CPP=app/src/main/cpp
JAVA_HOME=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
g++ -O3 -std=c++17 -I"$CPP" tests/core_test.cpp -o "$T/cxx-test"
"$T/cxx-test"
g++ -O3 -std=c++17 -shared -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" "$CPP/jni.cpp" -o "$T/libase_native.so"
kotlinc core/src/main/kotlin/com/adaptivestrength/core/Vbt.kt app/src/main/java/com/adaptivestrength/nativeapp/NativeTracker.kt tests/CoreTest.kt -include-runtime -d "$T/test.jar"
java -Djava.library.path="$T" -jar "$T/test.jar"
# Source HTML is deliberately excluded from the public repo.
if [[ -f reference/AdaptiveStrength_Mobile_M18I.html ]]; then (cd reference && sha256sum -c SHA256SUMS); else echo 'Original files retained locally; public repo contains hashes only.'; fi
