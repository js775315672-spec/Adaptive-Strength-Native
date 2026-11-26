#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
: "${ANDROID_HOME:=${ANDROID_SDK_ROOT:-}}"
[[ -n "$ANDROID_HOME" && -d "$ANDROID_HOME/platforms/android-35" ]] || { echo 'BLOCKED: Android SDK platform 35 unavailable' >&2; exit 2; }
command -v gradle >/dev/null || { echo 'BLOCKED: Gradle 8.9 unavailable' >&2; exit 2; }
[[ -d "$ANDROID_HOME/ndk/27.0.12077973" ]] || { echo 'BLOCKED: NDK unavailable' >&2; exit 2; }
[[ -d "$ANDROID_HOME/cmake/3.22.1" ]] || { echo 'BLOCKED: Android CMake unavailable' >&2; exit 2; }
# P123 CI requires a pinned model; the independent third-party digest is a consistency check, not official signed provenance.
model=app/src/main/assets/pose_landmarker_lite.task
if [[ -f "$model" ]]; then
 : "${POSE_MODEL_SHA256:?Provide independently trusted model SHA256}"
 printf '%s  %s\n' "$POSE_MODEL_SHA256" "$model" | sha256sum -c -
fi
gradle --no-daemon :app:assembleDebug :app:lintDebug
apk=app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
"$ANDROID_HOME/build-tools/35.0.0/apksigner" verify --verbose --print-certs "$apk"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$apk"
unzip -t "$apk"
unzip -Z1 "$apk" | grep -Fx 'lib/arm64-v8a/libase_native.so'
if [[ -n "${POSE_MODEL_SHA256:-}" ]]; then
  test -f "$model"
  actual=$(unzip -p "$apk" assets/pose_landmarker_lite.task | sha256sum | cut -d' ' -f1)
  [[ "$actual" == "$POSE_MODEL_SHA256" ]] || { echo "APK embedded pose model hash mismatch" >&2; exit 1; }
  echo "APK_EMBEDDED_MODEL_SHA256=$actual"
fi
sha256sum "$apk"
