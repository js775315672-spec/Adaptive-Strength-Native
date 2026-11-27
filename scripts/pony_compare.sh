#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
t=$(mktemp -d); trap 'rm -rf "$t"' EXIT
mkdir -p "$t/baseline" "$t/current"
git show 1ea2198f335feb83c4b76b3f56defbc50f81ba53:app/src/main/cpp/tracker.hpp > "$t/baseline/tracker.hpp"
cp app/src/main/cpp/tracker.hpp "$t/current/tracker.hpp"
cp tests/pony_exact.cpp "$t/"
g++ -O3 -std=c++17 -ffp-contract=off "$t/pony_exact.cpp" -o "$t/compare"
"$t/compare"
echo PONY_EXACT_AND_CPU_GATE=PASS
