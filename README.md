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
| Resizable SQL editor | North region with splitter (drag the border to enlarge/collapse), monospace font, spellcheck disabled |
| Run with Ctrl+Enter | In addition to the toolbar button; shows a busy indicator while the query runs |
| Statement history | Toolbar combo with the last 50 successful queries of the session (tooltip shows the full SQL); selecting one loads it into the editor |
| Copy results | Toolbar button that copies the whole result set to the clipboard as tab-separated values (paste directly into Excel/Calc) |
| Copy one row | Double-click a result row to copy it to the clipboard |
| Export to CSV | Downloads the result as `SQLQuery_yyyyMMdd_HHmmss.csv`, UTF-8 with BOM (Excel opens accents correctly) |
| Status bar | South region with record count and duration in seconds; errors are shown in red |
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

Additionally: a single statement per execution (no `;`) and the connection is
opened read-only.

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
