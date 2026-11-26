#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
url=https://storage.googleapis.com/mediapipe-models/pose_landmarker/pose_landmarker_lite/float16/1/pose_landmarker_lite.task
expected=59929e1d1ee95287735ddd833b19cf4ac46d29bc7afddbbf6753c459690d574a
dest=app/src/main/assets/pose_landmarker_lite.task
tmp=$(mktemp); trap 'rm -f "$tmp"' EXIT
curl --fail --silent --show-error --location --proto '=https' --retry 3 --connect-timeout 15 --max-time 120 "$url" -o "$tmp"
printf '%s  %s\n' "$expected" "$tmp" | sha256sum -c -
test "$(wc -c < "$tmp")" -eq 5777746
unzip -t "$tmp"
install -m 0644 "$tmp" "$dest"
printf 'MODEL_SOURCE=%s\nMODEL_SHA256=%s\n' "$url" "$expected"
