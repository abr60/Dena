# AGENTS.md — Dena

Read this before touching build, release, or theme code.

## Build golden rules (protect the 1.3 MB win)

APK size history — do not regress:
- v0.3.0 (minSdk 33): **19.8 MB** — dex stored uncompressed + page-aligned (AGP default for minSdk ≥ 28, mmap path)
- v0.3.1 (minSdk 24): **7.0 MB** — dex auto-compressed (AGP default for minSdk < 28)
- v0.4.0 (minSdk 24 + R8): **1.3 MB** — single dex, full shrink

Rules that preserve this:

1. **`minSdk = 24`** — floor is `LocaleHelper` (`LocaleList`/`setLocales`, Android 7.0). Do not raise without re-checking.
2. **`isMinifyEnabled = true` + `isShrinkResources = true`** in `release` — full R8. See `app/proguard-rules.pro` (Room + BuildConfig keeps). Do not disable to "debug" a release crash — use `Logcat` or a debug build instead.
3. **`packaging { dex { useLegacyPackaging = true } ; jniLibs { useLegacyPackaging = true } }`** — forces explicit compression. Without it, a future `minSdk ≥ 28` bump silently restores uncompressed dex and balloons back to ~20 MB.
4. **`resourceConfigurations += listOf("en","bn")`** in `defaultConfig` — prunes unused locales from dependencies.
5. **`debugImplementation("androidx.compose.ui:ui-tooling-preview")`** — must stay `debugImplementation`, not `implementation`.
6. Guard Dynamic Colors (Material You) with `Build.VERSION.SDK_INT >= S` in `SettingsScreen` — it crashes on API < 31.

## Versioning (SemVer discipline)

- **0.x until 1.0.0 is declared.** Stay on `0.*` (e.g. `0.6.0-alpha`) until the owner explicitly declares 1.0.0 feature-complete. Do not jump to 1.0.0 on your own.
- **Format:** `MAJOR.MINOR.PATCH[-PRERELEASE]` (e.g. `0.6.0-alpha`). Tags are `vX.Y.Z[-PRERELEASE]` (e.g. `v0.6.0-alpha`).
- **Rules:** `versionCode` strictly monotonic. One `CHANGELOG.md` entry per release (newest on top). Never skip a version or reuse a tag.
- **Pre-release suffix:** keep `-alpha` until 1.0.0 unless the owner says otherwise.

## Release process

- Keystore: `~/.keystores/central-release.keystore`, alias `dena` (env: `CENTRAL_RELEASE_STORE_FILE`, `DENA_RELEASE_KEY_ALIAS`, passwords via env/gradle props). Never commit `*.jks`/`*.keystore`, never mention keystore path/alias in release notes.
- Bump `versionCode` + `versionName` in `app/build.gradle.kts`.
- Build: `./gradlew :app:assembleRelease` → verify `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` (CN=Dena).
- Publish: `gh release create vX.Y.Z ... app/build/outputs/apk/release/app-release.apk` — use a notes file, not inline ` --notes "…` with backticks.

## Theme notes

- `Theme.kt`: `DenaLightScheme`/`DenaDarkScheme` provide neutral `surfaceContainer*` ramps. `buildDynamicScheme(ctx,dark)` overlays only accents from wallpaper. Do not tint surfaces.
- FAB uses `primaryContainer` — keep neutral mapping.

## App overview

Kotlin · Compose · Material3 · Room · Navigation-Compose. On-device only. See `README.md` + `screenshots/`.
