# Changelog

All notable changes to this project are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).
Versions follow `<iDempiere major>.<feature>.<fix>`.

## [12.1.0] - 2026-10-08

### Changed
- Port of 13.1.0 to iDempiere 12 (Java 17, Tycho 4.0.8). No functional changes.
- The build resolves iDempiere 12 from its public CI p2 repository.

## [13.1.0] - 2026-10-07

### Added
- SQL code editor based on CodeMirror 5.65.16 (bundled in the plug-in, MIT license):
  syntax highlighting, line numbers. Can be disabled with the SysConfig
  `SQLQUERY_ENHANCED_CODE_EDITOR=N`.
- Run only the selected text (Ctrl+Enter or the execute button).
- Tab / Shift+Tab indent and unindent.
- Statement history persisted across sessions (read from `AD_Issue`), with time and row count.
- `NULL` values shown in grey italics; numeric columns without decimals shown as integers.
- "Execute Query" label on the execute button and a "?" button with the keyboard shortcuts.
- Optional index `ad_issue_formuser_idx` on `AD_Issue (AD_Form_ID, CreatedBy, Created)` for the
  statement history lookup: scripts in `migration/postgresql` and `migration/oracle`,
  documented in the README.

### Changed
- Result count and duration shown in the toolbar; the error region only appears on errors.
- Plug-in version no longer copies the iDempiere version; standalone Maven/Tycho build
  resolving iDempiere 13 from its public p2 repository (`mvn verify`, no local checkout).

### Fixed
- Selecting a statement in the history combo did nothing: iDempiere's
  `ValidateReadonlyComponent` drops events from readonly comboboxes.
- Statements ending with `;` were rejected as multiple commands.
- `--` line comments covered the whole statement (line breaks were turned into spaces).
- Allowed keywords are matched as whole words (`SELECT*FROM` is accepted, `SELECTX` is not).
- Exceptions are logged with `CLogger` instead of `printStackTrace`.
- Broken screenshot link in the README.

## [13.0.0] - 2026-09-23

### Changed
- Updated for iDempiere 13 (Java 17).

## [10.0.0] - 2026-07-08

### Added
- First release for iDempiere 10: resizable SQL editor, Ctrl+Enter execution,
  statement history, copy results / copy row to the clipboard, CSV export.
