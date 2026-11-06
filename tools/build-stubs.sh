#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
find app/src/main/java_stubs -name '*.java' > "$tmp/sources.txt"
javac -source 8 -target 8 -d "$tmp/classes" @"$tmp/sources.txt"
mkdir -p app/libs
jar cf app/libs/xposed-api-stubs.jar -C "$tmp/classes" .
echo "Compile-only legacy Xposed API stub jar created."
