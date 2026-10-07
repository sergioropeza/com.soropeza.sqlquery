# com.soropeza.sqlquery — SQL Query Enhanced Form

*Read in [English](README.md).*

Plugin OSGi para **iDempiere 13** que reemplaza la forma estándar *SQL Query* por una
versión mejorada (`WSQLQueryEnhanced`), pensada para el consultor técnico que trabaja
con queries complejos: editor redimensionable, atajos de teclado, historial de
sentencias, copiado de resultados y exportación a CSV.

- **Bundle:** `com.soropeza.sqlquery`
- **Versión:** `13.0.0.qualifier`
- **Autor:** Sergio Oropeza
- **Licencia:** [GPL v2](LICENSE.md)

![SQL Query Enhanced](docs/screenshot.png)

## Funcionalidades

| Función | Detalle |
|---|---|
| Editor SQL con resaltado | [CodeMirror](https://codemirror.net/5/) (incluido en el plugin) con resaltado de sintaxis SQL y números de línea, en una región norte redimensionable. Se puede desactivar por SysConfig y queda un textarea monoespaciado con los mismos atajos |
| Ejecutar con Ctrl+Enter | Además del botón "Ejecutar Consulta" de la toolbar; muestra indicador de "Procesando" mientras corre el query |
| Ejecutar la selección | Si hay texto seleccionado en el editor, Ctrl+Enter o el botón ejecutan solo esa parte (el estado lo indica con "(Selección)") |
| Tab indenta | Tab inserta 4 espacios (o indenta las líneas seleccionadas); Shift+Tab desindenta |
| Historial de sentencias | Combo en la toolbar con las últimas 50 queries exitosas del usuario, con hora y cantidad de registros (tooltip muestra el SQL completo); al seleccionar una se carga en el editor. Persiste entre sesiones: se lee de `AD_Issue` (requiere `FORM_SQL_QUERY_LOG_ISSUE=Y`) |
| NULL visible | Los valores nulos se muestran como `NULL` en gris cursiva, distinto de un texto vacío; las columnas numéricas sin decimales (IDs) se muestran como enteros |
| Ayuda de atajos | Botón "?" en la toolbar con los atajos de teclado |
| Copiar resultados | Botón que copia todo el resultado al portapapeles como valores separados por tabulador (pegable directo en Excel/Calc) |
| Copiar una fila | Doble clic sobre una fila del resultado la copia al portapapeles |
| Exportar a CSV | Descarga el resultado como archivo `SQLQuery_yyyyMMdd_HHmmss.csv` en UTF-8 con BOM (Excel lo abre con acentos correctos) |
| Estado y errores | La cantidad de registros y la duración se muestran en la toolbar (en naranja si se alcanzó el máximo de registros); los errores se muestran completos en una región sur que solo aparece cuando hay error |
| Limpiar editor | Botón de la toolbar que vacía el editor y le devuelve el foco |

## Cómo funciona

El plugin **no modifica el core ni el Application Dictionary**. Registra
`SOP_FormFactory` como servicio OSGi `IFormFactory` con `service.ranking = 100`
(mayor que las factories del core), de modo que cuando el usuario abre la forma
estándar *SQL Query* (classname `org.adempiere.webui.apps.form.WSQLQuery`), la
factory devuelve en su lugar una instancia de
`com.soropeza.webui.apps.form.WSQLQueryEnhanced`.

Para volver al comportamiento estándar basta con detener o desinstalar el bundle.

## Seguridad

Se conserva intacta la lógica de seguridad de la forma original, controlada por
SysConfig:

| SysConfig | Default | Descripción |
|---|---|---|
| `FORM_SQL_QUERY_ALLOWED_KEYWORDS` | `SELECT,WITH,SHOW` | Palabras clave con las que puede iniciar la sentencia |
| `FORM_SQL_QUERY_MAX_RECORDS` | `500` | Máximo de registros retornados |
| `FORM_SQL_QUERY_TIMEOUT_IN_SECONDS` | `120` | Timeout del query |
| `FORM_SQL_QUERY_LOG_ISSUE` | `Y` | Registra cada ejecución en `AD_Issue` (SQL, resultado y duración) |

Además: una sola sentencia por ejecución (se aceptan `;` al final, no entre
sentencias) y la conexión se abre en modo solo lectura.

Configuración del editor:

| SysConfig | Default | Descripción |
|---|---|---|
| `SQLQUERY_ENHANCED_CODE_EDITOR` | `Y` | `Y` usa el editor CodeMirror; `N` usa el textarea simple (con los mismos atajos) |

CodeMirror 5.65.16 (licencia MIT) va **incluido en el plugin** (`src/.../form/codemirror/`) y
el servidor lo envía al navegador la primera vez que un editor lo necesita (una vez por página).
No se carga nada desde CDNs ni sitios externos, así que funciona en servidores sin internet.

Textos: la ayuda de atajos usa el mensaje `SQLQueryEnhancedHelp` (una línea por
atajo); si no existe en `AD_Message` se muestra en inglés.

> **Nota de mantenimiento:** este plugin lleva una copia de la lógica de la forma
> del core, incluyendo las validaciones de seguridad anteriores. Si una versión
> futura de iDempiere parcha `WSQLQuery` (por ejemplo un fix de seguridad), este
> plugin **no** lo hereda — hay que actualizarlo. Revisa los cambios del core en
> `org.adempiere.ui.zk/.../WSQLQuery.java` al actualizar.

## Estructura

```
com.soropeza.sqlquery/
├── META-INF/MANIFEST.MF                                  # Manifiesto OSGi (Require-Bundle: base, ui.zk, zk, zul, zcommon)
├── OSGI-INF/formfactory.xml                              # Componente DS que publica la IFormFactory
├── build.properties
├── pom.xml                                               # Empaquetado tycho (padre org.idempiere.parent)
└── src/
    └── com/soropeza/webui/
        ├── apps/form/WSQLQueryEnhanced.java              # La forma mejorada
        ├── apps/form/WSQLQueryEnhanced.js                # Editor del lado cliente (CodeMirror, atajos, selección)
        ├── apps/form/codemirror/                         # CodeMirror 5.65.16 minificado + LICENSE (MIT)
        └── factory/SOP_FormFactory.java                  # Intercepta el classname de la forma estándar
```

## Instalación

### Entorno de desarrollo (Eclipse)

1. `File > Import > Existing Projects into Workspace` y seleccionar este directorio.
2. Agregar el plugin a la Run Configuration del servidor (pestaña *Plug-ins*).
3. Iniciar el servidor; verificar en la consola OSGi con `ss soropeza` que el bundle
   esté `ACTIVE` (o `LAZY`).

### Servidor (producción)

1. Exportar el jar: en Eclipse `Export > Deployable plug-ins and fragments`
   (o `mvn verify` si el plugin está integrado al build tycho).
2. Copiar `com.soropeza.sqlquery_13.0.0.*.jar` al servidor e instalarlo vía la
   consola OSGi (telnet al puerto 12612):

   ```
   install file:/ruta/com.soropeza.sqlquery_13.0.0.jar
   start <bundle-id>
   ```

   o copiarlo a la carpeta `plugins/` con su entrada correspondiente y reiniciar.
3. Abrir la forma **SQL Query** desde el menú: debe mostrarse la versión mejorada
   (toolbar con botones e historial).

## Notas

- El historial de queries vive en memoria de la instancia de la forma: se pierde al
  cerrar la pestaña de la forma o la sesión.
- El copiado al portapapeles usa `navigator.clipboard`, que requiere que iDempiere
  se sirva por HTTPS o desde `localhost` (restricción de los navegadores).
- Probado en iDempiere 13 (release-13).

## Licencia

[GNU General Public License v2](LICENSE.md) — la misma licencia de iDempiere.
`WSQLQueryEnhanced` deriva de la forma original `WSQLQuery` de Carlos Ruiz
(globalqss - bxservice); la atribución original se conserva en el header del
código fuente.
