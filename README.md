# com.soropeza.sqlquery — SQL Query Enhanced Form

*Leer en [español](README.es.md).*

OSGi plugin for **iDempiere 13** that replaces the standard *SQL Query* form with an
enhanced version (`WSQLQueryEnhanced`) aimed at technical consultants working with
complex queries: SQL code editor, run the selection, persistent statement history,
clipboard copy and CSV export — without touching the core or the Application Dictionary.

- **Bundle:** `com.soropeza.sqlquery`
- **Version:** `13.1.0` (see [CHANGELOG](CHANGELOG.md))
- **Compatibility:** iDempiere 13 (Java 17), PostgreSQL and Oracle
- **Dependencies:** none besides the iDempiere core
- **Author:** Sergio Oropeza
- **License:** GPL-2.0-or-later ([LICENSE.md](LICENSE.md))
- **Source code / issues:** https://github.com/sergioropeza/com.soropeza.sqlquery


![SQL Query Enhanced](docs/screenshot.png)


## Features

| Feature | Detail |
|---|---|
| SQL editor with highlighting | [CodeMirror](https://codemirror.net/5/) (bundled in the plugin) with SQL syntax highlighting and line numbers, in a resizable north region. It can be disabled by SysConfig, leaving a monospace textarea with the same shortcuts |
| Run with Ctrl+Enter | In addition to the toolbar "Execute Query" button; shows a busy indicator while the query runs |
| Run the selection | When text is selected in the editor, Ctrl+Enter or the button run only that part (the status shows "(Selection)") |
| Tab indents | Tab inserts 4 spaces (or indents the selected lines); Shift+Tab unindents |
| Statement history | Toolbar combo with the user's last 50 successful queries, with time and record count (tooltip shows the full SQL); selecting one loads it into the editor. Persists across sessions: read from `AD_Issue` (requires `FORM_SQL_QUERY_LOG_ISSUE=Y`) |
| Visible NULL | Null values are shown as `NULL` in grey italics, distinct from an empty string; numeric columns without decimals (IDs) are shown as integers |
| Shortcuts help | "?" toolbar button listing the keyboard shortcuts |
| Copy results | Toolbar button that copies the whole result set to the clipboard as tab-separated values (paste directly into Excel/Calc) |
| Copy one row | Double-click a result row to copy it to the clipboard |
| Export to CSV | Downloads the result as `SQLQuery_yyyyMMdd_HHmmss.csv`, UTF-8 with BOM (Excel opens accents correctly) |
| Status and errors | Record count and duration are shown in the toolbar (orange when the maximum records was reached); errors are shown in full in a south region that only appears on error |
| Clear editor | Toolbar button that empties the editor and returns focus to it |

## How it works

The plugin **does not modify the core or the Application Dictionary**. It registers
`SOP_FormFactory` as an OSGi `IFormFactory` service with `service.ranking = 100`
(higher than the core factories), so when a user opens the standard *SQL Query*
form (classname `org.adempiere.webui.apps.form.WSQLQuery`), the factory returns an
instance of `com.soropeza.webui.apps.form.WSQLQueryEnhanced` instead.

To go back to the standard behavior, just stop or uninstall the bundle.

## Security

The security logic of the original form is preserved untouched, governed by
SysConfig:

| SysConfig | Default | Description |
|---|---|---|
| `FORM_SQL_QUERY_ALLOWED_KEYWORDS` | `SELECT,WITH,SHOW` | Keywords the statement may start with |
| `FORM_SQL_QUERY_MAX_RECORDS` | `500` | Maximum records returned |
| `FORM_SQL_QUERY_TIMEOUT_IN_SECONDS` | `120` | Query timeout |
| `FORM_SQL_QUERY_LOG_ISSUE` | `Y` | Logs every execution to `AD_Issue` (SQL, result and duration) |

Additionally: a single statement per execution (trailing `;` are accepted, not
between statements) and the connection is opened read-only.

Editor configuration:

| SysConfig | Default | Description |
|---|---|---|
| `SQLQUERY_ENHANCED_CODE_EDITOR` | `Y` | `Y` uses the CodeMirror editor; `N` uses the plain textarea (same shortcuts) |

CodeMirror 5.65.16 (MIT license) is **bundled in the plugin** (`src/.../form/codemirror/`) and
the server sends it to the browser the first time an editor needs it (once per page). Nothing
is loaded from CDNs or external sites, so it works on servers without internet access.

Texts: the shortcuts help uses the `SQLQueryEnhancedHelp` message (one line per
shortcut); when it is not defined in `AD_Message` it is shown in English.

> **Maintenance note:** this plugin carries a copy of the core form logic,
> including the security checks above. If a future iDempiere release patches
> `WSQLQuery` (e.g. a security fix), this plugin does **not** inherit it — it must
> be updated accordingly. Review core changes to
> `org.adempiere.ui.zk/.../WSQLQuery.java` when upgrading.

## Project layout

```
com.soropeza.sqlquery/
├── META-INF/MANIFEST.MF                                  # OSGi manifest (Require-Bundle: base, ui.zk, zk, zul, zcommon)
├── OSGI-INF/formfactory.xml                              # DS component publishing the IFormFactory
├── build.properties
├── pom.xml                                               # Standalone Tycho build (iDempiere 13 p2 repository)
├── migration/{postgresql,oracle}/ad_issue_formuser_idx.sql  # Optional index for the history (see Installation)
└── src/
    └── com/soropeza/webui/
        ├── apps/form/WSQLQueryEnhanced.java              # The enhanced form
        ├── apps/form/WSQLQueryEnhanced.js                # Client side editor (CodeMirror, shortcuts, selection)
        ├── apps/form/codemirror/                         # CodeMirror 5.65.16 minified + LICENSE (MIT)
        └── factory/SOP_FormFactory.java                  # Intercepts the standard form classname
```

## Installation

### Development environment (Eclipse)

1. `File > Import > Existing Projects into Workspace` and select this directory.
2. Add the plugin to the server Run Configuration (*Plug-ins* tab).
3. Start the server; check in the OSGi console with `ss soropeza` that the bundle
   is `ACTIVE` (or `LAZY`).

### Server (production)

1. Download `com.soropeza.sqlquery-<version>.jar` from the
   [GitHub Releases](https://github.com/sergioropeza/com.soropeza.sqlquery/releases)
   page (or build it, see below).
2. Install it with the Felix web console (`https://<server>/osgi/system/console/bundles`,
   *Install/Update...*, check *Start Bundle*), or through the OSGi console
   (telnet to port 12612):

   ```
   install file:/path/com.soropeza.sqlquery-13.1.0.jar
   start <bundle-id>
   ```

3. Open the **SQL Query** form from the menu: the enhanced version should appear
   (toolbar with *Execute Query*, history and the code editor).

To go back to the standard form, stop or uninstall the bundle. The plugin makes no
database changes by itself, so nothing needs to be rolled back (the optional index below
can be left in place or dropped).

### History index (recommended)

The statement history is read from `AD_Issue`, filtering by form and user and ordering by
date. The core only indexes `AD_Issue_UU`, so on databases with many issues that lookup scans
the whole table every time the form is opened. The `migration/` folder has a script that
creates the index `ad_issue_formuser_idx` on `AD_Issue (AD_Form_ID, CreatedBy, Created)`:

| Database | Script |
|---|---|
| PostgreSQL | `migration/postgresql/ad_issue_formuser_idx.sql` (can be run more than once) |
| Oracle | `migration/oracle/ad_issue_formuser_idx.sql` (ORA-00955 means it already exists) |

Run it once, as the iDempiere database user (`adempiere`), for example:

```
psql -h <host> -U adempiere -d idempiere -f migration/postgresql/ad_issue_formuser_idx.sql
```

It is not required: without the index everything works the same, only slower to open the form
on large `AD_Issue` tables. The index is not registered in the Application Dictionary, so it
does not appear in the *Table Index* tab. To remove it: `DROP INDEX ad_issue_formuser_idx;`.

### Building from source

Requires Java 17 and Maven 3.9+. The iDempiere core bundles are resolved from the
public iDempiere 13 p2 repository, so no local iDempiere checkout is needed:

```
mvn verify
```

The jar is created in `target/`. To build against another iDempiere build or offline:
`mvn verify -Didempiere.core.repository.url=file:///path/to/org.idempiere.p2/target/repository`.

## Notes

- The query history is read from `AD_Issue`, where the form logs every successful
  execution (`FORM_SQL_QUERY_LOG_ISSUE=Y`, the default). Each user only sees their own
  statements. With the SysConfig set to `N`, the history only lasts while the form is open.
  On large `AD_Issue` tables, create the [history index](#history-index-recommended).
- Clipboard copy uses `navigator.clipboard`, which browsers only allow over HTTPS
  or from `localhost`.
- Tested on iDempiere 13 (release-13).

## License

[GNU General Public License v2](LICENSE.md) — same license as iDempiere.
`WSQLQueryEnhanced` is derived from the original `WSQLQuery` form by Carlos Ruiz
(globalqss - bxservice); the original attribution is preserved in the source
header.

Third-party components bundled in the jar:

| Component | Version | License | Location |
|---|---|---|---|
| [CodeMirror](https://codemirror.net/5/) | 5.65.16 | MIT | `src/com/soropeza/webui/apps/form/codemirror/` (with its `LICENSE`) |

## Support

Report bugs and feature requests in the
[GitHub issues](https://github.com/sergioropeza/com.soropeza.sqlquery/issues).
