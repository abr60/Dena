# Changelog

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
