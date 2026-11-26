# Adaptive Strength Native — P1–P3 DEV
P0 remains at commit 5a1a5a44367be9dc09713e5ce2d12e9b09fc60ad, app ID com.adaptivestrength.nativeapp. This branch uses a separate app ID com.adaptivestrength.nativeapp.p123 so P0 can remain installed. No source originals or personal videos are published. No INTERNET permission.

- P1: subpixel peak interpolation and reused score buffer; exact integer regression and fractional ground-truth tests. Optional two user-selected background anchors in file mode, correction only if both agree. Translation-only, no rotation/perspective validation.
- P2: pinned official Pose Landmarker lite version 1 downloaded at CI; independent third-party digest comparison and APK asset digest verification required. CPU inference; real human-pose inference not yet verified.
- P3: CameraX back camera, bounded latest-frame analysis, one pending UI preview, explicit freeze/touch seed, local pixel-unit VBT, optional pose overlay. Actual camera, frame rates, heat and battery use untested. Camera frames are not stored or transmitted.
- File analysis: original P0 pipeline, physical scale by two-point diameter, JSON export; experimental.
- Debug signing, no production or medical approval, no automatic load increase. Live mode has no calibrated scale or export yet.
- Build: JDK 17, Gradle 8.9, Android 35, NDK 27.0.12077973, CMake 3.22.1. CI first runs host regression then builds and checks APK and embedded model.
