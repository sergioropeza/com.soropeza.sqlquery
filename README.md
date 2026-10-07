# com.soropeza.sqlquery — SQL Query Enhanced Form

*Leer en [español](README.es.md).*

OSGi plugin for **iDempiere 13** that replaces the standard *SQL Query* form with an
enhanced version (`WSQLQueryEnhanced`) aimed at technical consultants working with
complex queries: resizable editor, keyboard shortcuts, statement history, clipboard
copy and CSV export — without touching the core or the Application Dictionary.

- **Bundle:** `com.soropeza.sqlquery`
- **Version:** `13.0.0.qualifier`
- **Author:** Sergio Oropeza
- **License:** [GPL v2](LICENSE.md)


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
├── pom.xml                                               # Tycho packaging (parent org.idempiere.parent)
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

1. Export the jar: in Eclipse `Export > Deployable plug-ins and fragments`
   (or `mvn verify` if the plugin is integrated into a Tycho build).
2. Copy `com.soropeza.sqlquery_13.0.0.*.jar` to the server and install it through
   the OSGi console (telnet to port 12612):

   ```
   install file:/path/com.soropeza.sqlquery_13.0.0.jar
   start <bundle-id>
   ```

   or drop it into the `plugins/` folder with its corresponding entry and restart.
3. Open the **SQL Query** form from the menu: the enhanced version should appear
   (toolbar with buttons and history).

## Notes

- The query history lives in the form instance memory: it is lost when the form
  tab or the session is closed.
- Clipboard copy uses `navigator.clipboard`, which browsers only allow over HTTPS
  or from `localhost`.
- Tested on iDempiere 13 (release-13).

## License

[GNU General Public License v2](LICENSE.md) — same license as iDempiere.
`WSQLQueryEnhanced` is derived from the original `WSQLQuery` form by Carlos Ruiz
(globalqss - bxservice); the original attribution is preserved in the source
header.
