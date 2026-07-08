# com.soropeza.sqlquery — SQL Query Enhanced Form

Plugin OSGi para **iDempiere 10** que reemplaza la forma estándar *SQL Query* por una
versión mejorada (`WSQLQueryEnhanced`), pensada para el consultor técnico que trabaja
con queries complejos: editor redimensionable, atajos de teclado, historial de
sentencias, copiado de resultados y exportación a CSV.

- **Bundle:** `com.soropeza.sqlquery`
- **Versión:** `10.0.0.qualifier`
- **Autor:** Sergio Oropeza

## Funcionalidades

| Función | Detalle |
|---|---|
| Editor SQL redimensionable | Región norte con splitter (arrastra el borde para agrandar/colapsar), fuente monoespaciada, sin corrector ortográfico |
| Ejecutar con Ctrl+Enter | Además del botón de la toolbar; muestra indicador de "Procesando" mientras corre el query |
| Historial de sentencias | Combo en la toolbar con las últimas 50 queries exitosas de la sesión (tooltip muestra el SQL completo); al seleccionar una se carga en el editor |
| Copiar resultados | Botón que copia todo el resultado al portapapeles como valores separados por tabulador (pegable directo en Excel/Calc) |
| Copiar una fila | Doble clic sobre una fila del resultado la copia al portapapeles |
| Exportar a CSV | Descarga el resultado como archivo `SQLQuery_yyyyMMdd_HHmmss.csv` en UTF-8 con BOM (Excel lo abre con acentos correctos) |
| Barra de estado | Región sur con cantidad de registros y duración en segundos; los errores se muestran en rojo |
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

Además: una sola sentencia por ejecución (sin `;`) y la conexión se abre en modo
solo lectura.

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
2. Copiar `com.soropeza.sqlquery_10.0.0.*.jar` al servidor e instalarlo vía la
   consola OSGi (telnet al puerto 12612):

   ```
   install file:/ruta/com.soropeza.sqlquery_10.0.0.jar
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
- Probado en iDempiere 10 (release-10).
