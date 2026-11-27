# Adaptive Strength Native P4567 DEV
Based on P123 ae94d4b5e8191460dff005c00c960322cbc03ae7; separate Android applicationId so prior apps remain installed.
P4: optional two-stationary-anchor inverse rigid translation/rotation (up to 15 degrees); rejects weak, unsynchronized, degenerate, zoom-like anchors. Underlying NCC is translation-template based, not rotation-invariant; real motion, perspective, occlusion and optical-flow fallback remain unvalidated/unimplemented.
P5: stack candidate array, squared distance, live AI interval >=200 ms, same-runner baseline regression comparison. No GPU/NEON or measured mobile speed claim.
P6: confidence-filtered 2D image-plane joint angle ranges and simultaneous knee-angle difference. At least three valid frames; not a clinical/anatomical assessment. Existing VBT rep speed/loss retained.
P7: last 24 completed ascent candidate chart, explicit local summary history (50 newest, app-private, no video/landmarks), original JSON export retained.
Offline model pinned to existing official URL and independent reference digest. CI gates host regression, same-runner comparison, APK lint/build/signature/archive and model SHA. No INTERNET permission or automatic approval. No main merge. Real camera, AI inference and exercise videos require actual device validation.

## P8–P10 experimental extension
Based on P4567 3ffc167e6f2d510941f0ddf767cb95661aa9310a. Separate package com.adaptivestrength.nativeapp.p8910.
P8: one stricter fixed-template 32px recovery attempt on first miss, minimum five agreeing features, ambiguity margin; no optical flow or rotation-invariant plate tracking yet. Synthetic recovery and baseline regression gate.
P9: per-decoder/camera-worker ARGB scratch reuse; file preview release. No measured Android FPS or GPU/NEON claim.
P10: explicit user-confirmed descriptive median rep speed comparison, same exercise, user-defined setup, mass +/-0.05kg, calibrated complete result, coverage >=95%, minimum 3 reps and 3 distinct saved sessions. Re-saving identical analysis ID prevented; re-analysis of same video can still make another ID; user must avoid duplicate recordings. No weight increase, clinical validation or auto learning. Old apps and history preserved separately.

## Pony optimization (P8910 baseline)
NCC exhaustive search, thresholds, feature consensus, rescue and dot-product order are retained. Bounded ROI integral sums avoid recomputing patch intensity and squared intensity. Maximum logical payload of the two integral arrays is 147,968 bytes per tracker (136*136*8); capacity and overall app memory may differ. File analysis shares immutable raw samples when no correction is selected, reuses joint definitions and serializes results off the UI thread. The independent comparison uses 288 synthetic frame decisions, coordinates and scores; 5 alternating timed trials. CI fails if a result differs or median CPU time increases by more than 2%. Previous app IDs, source branches and artifacts are unchanged; no promotion if gate fails. No real-device, GPU, memory or exercise accuracy claim.
