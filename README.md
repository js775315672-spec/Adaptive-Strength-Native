# Adaptive Strength Native — P0 DEV

Experimental offline Android video analysis. Kotlin native views, MediaCodec presentation timestamps, C++ multi-feature normalized correlation tracker via JNI, OpenCV circle candidates, optional MediaPipe Pose Landmarker. Manual plate center and two-point calibration. JSON export. Time-weighted ascent velocity and gap exclusion. No automatic load increase or learning approval.

This is an unvalidated developer build for arm64-v8a (Android 8+). The model is not bundled; pose inference requires an independently verified pose_landmarker_lite.task. CameraX, stabilization, rotation-aware plate tracking and real-device accuracy tests remain future work. No INTERNET permission. No personal videos or original HTML files are published; original hashes are recorded in reference/SHA256SUMS and source originals remain in the local P0 package.

## Build
JDK 17, Gradle 8.9, Android SDK 35, build-tools 35.0.0, NDK 27.0.12077973, CMake 3.22.1. Run bash scripts/build_apk.sh. CI builds and verifies a debug APK, not a validated release.

## Tests
Run bash scripts/test_host.sh (requires g++, JDK 17, kotlinc). The CI host test job and APK build job are separate. This does not validate Android runtime behavior. No automatic merge.
