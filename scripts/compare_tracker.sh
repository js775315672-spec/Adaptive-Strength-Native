#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp=$(mktemp -d);trap 'rm -rf "$tmp"' EXIT
git show 3ffc167e6f2d510941f0ddf767cb95661aa9310a:app/src/main/cpp/tracker.hpp > "$tmp/tracker.hpp"
g++ -std=c++17 -O3 -I"$tmp" tests/core_test.cpp -o "$tmp/baseline"
g++ -std=c++17 -O3 -Iapp/src/main/cpp tests/core_test.cpp -o "$tmp/current"
for n in 1 2 3; do
 for mode in baseline current; do
  "$tmp/$mode" > "$tmp/$mode.$n"
  grep -E 'HOST_TRACKER_BENCHMARK|CXX_PASS' "$tmp/$mode.$n" | sed "s/^/$mode trial=$n /"
 done
 diff -u <(grep -v HOST_TRACKER_BENCHMARK "$tmp/baseline.$n") <(grep -v HOST_TRACKER_BENCHMARK "$tmp/current.$n")
done
echo 'P8_BASELINE_REGRESSION_MATCH=3/3'
