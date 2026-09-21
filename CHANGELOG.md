# Changelog

## v0.6.8-alpha — 2026-09-21
- **Reference-style cards** — match Device Info Sensors reference (white `#FFFFFF` cards on warm gray, 14dp big / 12dp rows, 2dp light elevation / 0dp dark, no stroke). `DebtCardItem`, `SummaryBanner`, `DebtDetail` summary + `TransactionRow`, `Settings ActivityRow` use elevated neutral `surfaceContainerLowest` (light) / `surfaceContainerHigh` (dark); settled uses flat `surfaceContainer` 0dp with muted `outline` avatar. Fixed settled ghosting (removed `alpha(0.55)`, flat muted container + outline avatar, bottom padding 24dp). Avatar follows `primary` (dynamic accent when Material You active). Dynamic colors overlay only accents — card design applies to all themes, not limited to Material You.
- **Preferences** — `showDecimals` default `false`; `Show decimals` toggle in Localization; `Show contact number` (DB v3 `contactPhone` migration) toggle + display on debt cards (`MIGRATION_2_3`); `Show percentages` + moved `By date` (`showDateHeaders`, default off) toggle from list screens to `Settings → Language & Formatting → Formatting` (screens now read `prefs.showDateHeaders()`); date headers gated by pref.
- **Font picker** — `Settings → Appearance → FONT` changed from 3 `SettingsRow`+check rows to `DenaSelect` dropdown (Inter / JetBrains Mono / System default, keys `inter`/`jbmono`/`system` → `DenaPreferences.KEY_APP_FONT`, wired via `ThemeState.fontKey` + `DenaTheme` `fontKey` → `buildFontFamily`).
- **Due date cleanup** — removed “loan with no due date” / “No due date” fallback; `DebtList` + `DebtDetail` only show due line when `dueDate != null` (form picker kept).
- **Theming** — `DenaLightScheme`/`DenaDarkScheme` neutral `surfaceContainer*` ramps; `buildDynamicScheme` overlays only accents (`primary`/`secondary`/`tertiary`/`surfaceTint`) from wallpaper when `dynamicColorsEnabled && S+`, keeps surfaces neutral.

## v0.6.7-alpha — 2026-09-18
- **Tab icons swap** — `I Borrowed` now uses the list icon, `I Lent` uses the person icon (positions unchanged).

## v0.6.6-alpha — 2026-09-18
- **Bottom nav swap** — `I Borrowed` now first tab, `I Lent` second (`DenaApp` destinations + tab mapping + FAB default updated).
- **Sort menu** — single sort icon in a right-aligned row beneath the overview card (was per-section); options: Newest first (default), Low to high, High to low (by remaining balance), disabled `Category` placeholder for future filtering. Sort state hoisted per tab so open + settled lists stay consistent.
- **Rename on Debt Details** — double-tap the contact name in the detail top bar to edit inline (Done saves, blank ignored); removed from main-list cards and Recent Activity rows.
- **Create Debt polish** — contact picker moved from standalone right icon to the leading icon inside the Contact Name field (manual typing still works); currency button now shows the symbol (e.g. `৳`) instead of the code, sized 64×56dp to match the amount field.
- **Release process** — new `release.sh` (build → `apksigner` verify `CN=Dena` → publish); asset naming convention `Dena-vX.Y.Z.apk`, never raw `app-release.apk`.

## v0.6.5-alpha — 2026-09-15
- **Modern date picker** — replaced `android.app.DatePickerDialog` (dated white dialog, bar-style header) with Material 3 `DatePicker` in a shared `DenaDatePickerDialog` composable (`surfaceVariant` container matching cards, monochrome-friendly, respects light/dark/palette/dynamic themes, `MMM d, yyyy` + local-noon storage preserved). All 3 sites covered: create-debt creation & due dates, edit-transaction date, log-payment date. Verified on device (Sep 15 round-trip).

## v0.6.4-alpha — 2026-09-15
- **New icon** — simple Talom-style launcher icon: flat bold `D` (`#F0F0F0`, same weight/footprint as Talom's `T`) on `#1A1A1A` tile; vector foreground + monochrome, legacy PNGs (mdpi–xxxhdpi, normal + round) regenerated, old `drawable-nodpi` PNG foreground deleted, adaptive `monochrome` layer added.

## v0.6.3-alpha — 2026-09-14
- **Feature: feedback** — Settings hub `Feedback` row (Email icon, `ACTION_VIEW https://github.com/abr60/Dena/issues/new/choose`) between App Updates and About, with `.github/ISSUE_TEMPLATE/bug_report.yml` + `feature_request.yml` + `config.yml` so `issues/new/choose` shows two labeled templates.

## v0.6.2-alpha — 2026-09-14
- **Fix: settled scroll bug** — `DebtList` `LazyColumn weight(1f)` split screen to 50/50 when `SETTLED` appeared (only 3 active visible). Reverted `DebtList` to `Column`, `ScreenContainer` outer `Column` → `fillMaxSize` + inner `Column(weight(1f).verticalScroll)` as single shared scroll; removed `weight` from `OwedToMeScreen`/`IOweScreen`.
- **Fix: date picker** — `PaymentModal`/`EditTransactionDialog` date field now `Box(fillMaxWidth.clickable)` + `enabled=false` + disabled colors so whole field triggers `DatePickerDialog`.
- **Feature: auto-update** — `POST_NOTIFICATIONS` permission, `UpdateNotifier` channel `dena_updates`, `UpdateAvailableDialog`, `DenaApp` `LaunchedEffect` throttled 12h (`fetchLatest`→`isNewer`→skip dismissed→show dialog + `notifyUpdateAvailable` once per tag). `DenaPreferences` keys `dismissed_update_tag`/`last_update_check`/`last_notified_tag`.

## v0.6.1-alpha — 2026-09-14
- **Performance**: `DebtList` → `LazyColumn` with `stickyHeader` date grouping, extracted `DebtCardItem`, `ScreenContainer` exposes `ColumnScope` for weighted lists.
- **UX polish**: `SummaryBanner` count now `N open debts` (not unique people), removed `+` badge, `DebtFormScreen` redesign (compact header, no-scroll form, 12dp spacing), `ScreenContainer` header 48dp / `DENA` 16sp, search field height-constrained 48dp.
- **Settings**: hub icons `Palette`/`Language`/`Storage`/`SystemUpdate` (requires `material-icons-extended`), spacing `24dp` / top `12dp`, `RecentActivity` → `ActivityRow` hoisted prefs.
- **Fixes**: `DenaPreferences` wrapped in `remember(context)` in `DebtList`, `DebtDetailScreen` (incl. `TransactionRow`), `SummaryBanner`, `DenaApp`; `DebtViewModel` counts open debts (not distinct names); removed unused `TransactionRow` `sign`/`amountColor` + dead `formatCurrency` helpers; theme `MoneyPalette` remembers palette; `ThemeState` consolidation.
- **Export**: backup JSON pretty-printed 2-space, `.json` + `application/json` MIME, date-named `dena-backup-YYYY-MM-DD.json`; statement CSV/PDF date-named.

## v0.6.0-alpha — 2026-09-13
- **Auto close/reopen**: `isClosed` flag (balance ≤ 0.005 → `SETTLED` greyed section, reopen on new transaction) + `MIGRATION_1_2` clamping legacy overpaid rows to 0, single `recalculateDebtBalance()` choke point, summaries exclude closed.
- **No overpaying status**: `remainingBalance` clamped at 0; repository + UI guards block amounts exceeding `remaining + 0.005`; "Overpaid" visuals removed.
- **Adaptive theme (API < 29)**: `Follow system` toggle on Android 10+; manual `Dark theme` toggle (defaults to light) on older devices, routed via `manualDark` in `DenaTheme`.
- **Header polish**: centered `DENA` titles with symmetrical gaps (status-bar ↔ header ↔ overview card equal), search lowered 4dp, `+` before I Lent overview total and per-row amounts.
- **Backup & restore**: picker-only export; `isClosed`/`creationDate` persisted, debtId remapping + dedupe, single transaction, filename-confirm toast, absolute-path toasts for CSV/PDF.

## v0.5.0-alpha — 2026-09-13
- Follow system toggle (Talom clone), hidden Advanced → Dynamic colors (Material You) behind 4-tap easter egg, transaction edit bottom sheet.

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
