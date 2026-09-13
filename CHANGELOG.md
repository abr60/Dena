# Changelog

## v0.4.0-alpha — 2026-09-13
- **1.3 MB APK** (down from 19.8 MB): R8 minify + shrinkResources, legacy packaging, locale prune (`en`/`bn`), `ui-tooling-preview` → `debugImplementation`. Single dex verified on device.

## v0.3.1-alpha — 2026-09-13
- **minSdk 33 → 24** (Android 7.0+, `LocaleHelper` floor). Guarded Dynamic Colors for < S. APK 19.8 MB → 7.0 MB via dex compression difference.

## v0.3.0-alpha
- 19.8 MB baseline (minSdk 33, dex uncompressed/page-aligned).

## v0.2.0-alpha — 2026-09-13
- FAB neutral `primaryContainer` mapping; dynamic scheme override (System/Light/Dark).

## v0.1.0-alpha
- Initial release.
