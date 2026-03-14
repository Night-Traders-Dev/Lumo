#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

case "${1:-}" in
    --build)
        echo "==> Building debug APK..."
        ./gradlew assembleDebug
        ;;
    --clean-build)
        echo "==> Clean building debug APK..."
        ./gradlew clean build
        ./gradlew assembleDebug
        ;;
    *)
        echo "Usage: $0 [--build | --clean-build]"
        echo "  --build        Run assembleDebug only"
        echo "  --clean-build  Run clean build, then assembleDebug"
        exit 1
        ;;
esac

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "ERROR: APK not found at $APK"
    exit 1
fi

echo "==> Installing via adb..."
adb install -r "$APK"

echo "==> Done."
