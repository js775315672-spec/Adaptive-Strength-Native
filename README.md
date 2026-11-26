# Adaptive Strength Native P4567 DEV
Based on P123 ae94d4b5e8191460dff005c00c960322cbc03ae7; separate Android applicationId so prior apps remain installed.
P4: optional two-stationary-anchor inverse rigid translation/rotation (up to 15 degrees); rejects weak, unsynchronized, degenerate, zoom-like anchors. Underlying NCC is translation-template based, not rotation-invariant; real motion, perspective, occlusion and optical-flow fallback remain unvalidated/unimplemented.
P5: stack candidate array, squared distance, live AI interval >=200 ms, same-runner baseline regression comparison. No GPU/NEON or measured mobile speed claim.
P6: confidence-filtered 2D image-plane joint angle ranges and simultaneous knee-angle difference. At least three valid frames; not a clinical/anatomical assessment. Existing VBT rep speed/loss retained.
P7: last 24 completed ascent candidate chart, explicit local summary history (50 newest, app-private, no video/landmarks), original JSON export retained.
Offline model pinned to existing official URL and independent reference digest. CI gates host regression, same-runner comparison, APK lint/build/signature/archive and model SHA. No INTERNET permission or automatic approval. No main merge. Real camera, AI inference and exercise videos require actual device validation.
