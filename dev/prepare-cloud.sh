#!/usr/bin/env bash
set -euo pipefail
# Retained tools live outside the checkout; no administrator access is needed.
tools=/workspace/toolchains
mkdir -p "$tools" /workspace/gradle-cache /workspace/socrates-runtime/shader-cache
verify() { printf '%s  %s\n' "$1" "$2" | sha256sum --check --status; }
gradle_sha=a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612
jdk_sha=166774efcf0f722f2ee18eba0039de2d685b350ee14d7b69e6f83437dafd2af1
if ! verify "$gradle_sha" "$tools/gradle-8.8-bin.zip" 2>/dev/null; then
  curl -fL --max-time 180 https://services.gradle.org/distributions/gradle-8.8-bin.zip -o "$tools/gradle-8.8-bin.zip"
fi
verify "$gradle_sha" "$tools/gradle-8.8-bin.zip"
if ! verify "$jdk_sha" "$tools/jdk17.tar.gz" 2>/dev/null; then
  curl -fL --max-time 180 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.16%2B8/OpenJDK17U-jdk_x64_linux_hotspot_17.0.16_8.tar.gz' -o "$tools/jdk17.tar.gz"
fi
verify "$jdk_sha" "$tools/jdk17.tar.gz"
if [[ ! -f "$tools/gradle-8.8/bin/gradle" ]]; then
  python3 -m zipfile -e "$tools/gradle-8.8-bin.zip" "$tools"
fi
if [[ ! -x "$tools/jdk-17.0.16+8/bin/java" ]]; then
  tar -xzf "$tools/jdk17.tar.gz" -C "$tools"
fi
"$tools/jdk-17.0.16+8/bin/java" -version
echo 'Verified Java 17 and Gradle 8.8 tools prepared. Forge dependencies require network access.'
