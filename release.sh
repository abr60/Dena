#!/usr/bin/env bash
# release.sh — Dena release entry point.
# Builds, verifies the signature, renames the APK to Dena-vX.Y.Z.apk,
# and publishes it to GitHub Releases. Convention: Dena-v<versionName>.apk
# Usage: ./release.sh [--notes-file FILE] [--draft] [--skip-build]
set -euo pipefail

NOTES_FILE=""
DRAFT=false
SKIP_BUILD=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --notes-file) NOTES_FILE="${2:?missing value}"; shift 2 ;;
    --draft) DRAFT=true; shift ;;
    --skip-build) SKIP_BUILD=true; shift ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

cd "$(dirname "$0")"

VERSION="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n1)"
[[ -n "$VERSION" ]] || { echo "Could not parse versionName from app/build.gradle.kts" >&2; exit 1; }
TAG="v${VERSION}"
ASSET="Dena-${TAG}.apk"
OUT_DIR="app/build/outputs/apk/release"
RAW_APK="${OUT_DIR}/app-release.apk"
RENAMED_APK="${OUT_DIR}/${ASSET}"

if git rev-parse "$TAG" >/dev/null 2>&1 || git ls-remote --tags origin | grep -q "refs/tags/${TAG}$"; then
  echo "Tag ${TAG} already exists — bump versionName first (never reuse a tag)." >&2
  exit 1
fi

if [[ "$SKIP_BUILD" == false ]]; then
  ./gradlew :app:assembleRelease
fi
[[ -f "$RAW_APK" ]] || { echo "Missing ${RAW_APK} — build failed?" >&2; exit 1; }

VERIFY_OUT="$(apksigner verify --print-certs "$RAW_APK")"
echo "$VERIFY_OUT"
echo "$VERIFY_OUT" | grep -q "CN=Dena" || { echo "Signature check failed: CN=Dena not found." >&2; exit 1; }

cp -f "$RAW_APK" "$RENAMED_APK"
echo "Asset: $RENAMED_APK ($(du -h "$RENAMED_APK" | cut -f1))"

ARGS=(--title "Dena ${TAG}")
[[ "$DRAFT" == true ]] && ARGS+=(--draft)
if [[ -n "$NOTES_FILE" ]]; then
  ARGS+=(--notes-file "$NOTES_FILE")
else
  ARGS+=(--generate-notes)
fi

gh release create "$TAG" "${ARGS[@]}" "$RENAMED_APK"
echo "Published ${TAG} with ${ASSET}"
