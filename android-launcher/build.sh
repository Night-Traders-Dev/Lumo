#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

echo "==> Building debug APK..."
./gradlew assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "ERROR: APK not found at $APK"
    exit 1
fi

echo "==> Installing via adb..."
adb install -r "$APK"

echo "==> Done."
