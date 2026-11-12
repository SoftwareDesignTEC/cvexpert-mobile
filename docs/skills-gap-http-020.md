# Android y brechas HTTP 0.2.0

Contrato inspeccionado: `SoftwareDesignTEC/cvexpert-ms-brecha-cursos`,
`origin/develop` en `1c5c793` (PR #16), especialmente `openapi.yaml` y
`docs/skills-gap-http.md`. Esta implementación no despliega ese backend ni modifica APIM.

## Activación segura

`SKILLS_GAP_HTTP_020_ENABLED` es **false por defecto**. Solo el valor textual `true`
(sin distinguir mayúsculas) activa el flujo. Se configura en `local.properties`
ignorado por Git o como variable de entorno al compilar:

```properties
SKILLS_GAP_HTTP_020_ENABLED=true
```

```powershell
$env:SKILLS_GAP_HTTP_020_ENABLED = 'true'
.\gradlew.bat assembleDebug testDebugUnitTest
Remove-Item Env:SKILLS_GAP_HTTP_020_ENABLED
```

La opción requiere recompilar; no existe interruptor para usuarios finales.
Con la opción apagada, el repositorio bloquea GET y PUT antes de tocar la red,
incluidas las consultas desde Inicio y Perfil. La pantalla de brechas indica que
el servicio está pendiente de activación. Las demás funcionalidades conservan sus APIs.
No se ofrece el mock anterior como análisis real ni se intenta escribir en una ruta
PUT todavía no confirmada. El workflow existente no propaga esta nueva opción, por
lo que sus builds continúan con ella apagada.

Para probarla, usar exclusivamente el `APIM_BASE_URL` existente y un gateway de
pruebas autorizado que ya exponga ambas operaciones reales. No usar una URL directa
al backend. Retrofit, OkHttp y el interceptor Google existentes siguen gestionando
la conexión. Android no envía identidad `X-User-*` ni certificados manualmente.

## Flujo y compatibilidad

1. Lee la instantánea vigente con `GET skills-gap?page=1&pageSize=100`.
2. Lista CVs con la API existente `GET cvs`, paginada de 20 en 20. La selección
   consulta `GET cvs/{id}` para obtener el detalle vigente; no usa habilidades del
   listado como si fueran necesariamente completas. No se inventan relaciones con CVs.
3. Transforma cada `{name, level}` del CV en `{skillName, level}`. Mantiene el nombre
   exacto. Solo reconoce `beginner`, `intermediate`, `advanced`, `expert`; etiquetas
   desconocidas o ausentes se envían como **null explícito**, nunca cero o porcentaje.
4. Muestra el CV elegido, cantidad de habilidades y niveles desconocidos. El botón
   explícito envía una autoevaluación y reemplaza toda la instantánea con
   `PUT skills-gap/input`; el cuerpo solo contiene `skills` y sus campos `skillName`
   y `level`. Valida nombres, duplicados, 500 habilidades y 100 KiB UTF-8 antes de enviar.
   Una lista vacía guarda una instantánea vacía. No modifica el CV.
5. Solo tras HTTP 204 vuelve a leer el análisis. Reintentar solo lee: no repite PUT
   después de una escritura confirmada y un GET fallido. Una escritura fallida no
   provoca GET ni conserva resultados previos en pantalla.

Los GET exigen `X-Data-Mode: reference`, metadatos 0.2.0 y escala `cv-skill-v1`.
Respuestas sin metadatos, demos y niveles legacy 40/80 se rechazan. El encabezado
indica procedencia persistente según el contrato; no certifica vigencia o completitud
del mercado y no sustituye la verificación operativa de APIM.

Los niveles y el radar usan posiciones ordinales 1–4. `deficitSteps` viene del
servidor y se valida contra los niveles. GET solo devuelve habilidades con déficit;
no permite reconstruir todas las habilidades del usuario. Inicio, Perfil y Brechas
lo indican para evitar presentar el radar parcial como perfil completo.

`analysis.unresolved` se conserva completo en cada página y muestra las cinco razones
del contrato; razones futuras tienen mensaje genérico. Niveles ausentes no se
dibujan como cero. Solo se afirma ausencia de brechas cuando hay mercado disponible,
estado completo, comparaciones positivas, ningún unresolved y totalRecords=0.
Mercado ausente, estado empty/incomplete o una página sin items no prueban cumplimiento.
Los cursos con duración null se conservan con duración desconocida; cero sigue siendo cero.

Paginación explícita del análisis: anterior/siguiente consultan páginas separadas,
sin agregar respuestas de instantes distintos en una falsa instantánea. Los totales,
el mercado y las habilidades pueden cambiar entre peticiones, como documenta el backend.
Las tarjetas de Inicio y Perfil muestran la primera página y remiten al análisis completo.

## Errores

- 401: aviso de sesión no autorizada; el interceptor Google existente invalida la sesión.
- 409: pide seleccionar y enviar habilidades; nunca informa ausencia de brechas.
- 503/403/404 y demás fallos operativos: servicio no disponible, sin fallback a mocks.
- 400/413: entrada inválida o demasiado grande; permite corregir el CV y volver a enviarlo.
- Respuesta incompatible: aviso propio, sin intentar interpretar porcentajes antiguos.
- Error de red: permite reintentar. La cancelación de corrutinas se propaga.

## Requisitos pendientes antes de producción

- Ratificar contrato 0.2.0, autoevaluaciones, privacidad, retención y borrado de instantáneas.
- Desplegar backend, migración 003 y permisos mínimos por el proceso administrativo autorizado.
- Configurar contexto del mercado, requisitos revisados, cursos y asociaciones reales.
- Confirmar rutas y reescrituras GET/PUT en APIM y sustituir sus mocks en otro cambio.
- Verificar token Google, identidad estable sobrescrita por APIM, mTLS al backend,
  rechazo de acceso directo y aislamiento entre dos usuarios a través del gateway.
- Verificar que APIM conserva `X-Data-Mode: reference` y respuestas/errores del contrato.
- Probar en dispositivo el flujo con el gateway preparado antes de habilitar builds productivos.

Rollback de Android: quitar la opción o asignarle false y recompilar. Esto detiene
las llamadas; no borra instantáneas ya guardadas ni cambia infraestructura.

## Validación

Pruebas JVM con JUnit, kotlinx-coroutines-test y MockWebServer (servidor HTTP local
de pruebas, no un resultado ficticio de producto). Cubren deserialización, ordinales,
duración null/cero, mapeo de CVs, null explícito en el cable HTTP, payload sin identidad,
paginación, bloqueo deshabilitado, errores HTTP, rechazo de mocks y estados del ViewModel,
incluido reintento tras PUT exitoso y GET fallido.

Comandos: `.\gradlew.bat assembleDebug testDebugUnitTest`, también con la opción habilitada.
La validación final ejecuta `.\gradlew.bat assembleDebug test lintDebug`: 18 pruebas JVM
en debug y las mismas 18 en release, sin fallos. Se verificó el campo generado de
BuildConfig en false para ambas variantes; también se comprobó compilación, suite debug
y lint con la activación true durante la implementación. Lint finaliza sin errores;
sus advertencias no se suprimen ni motivan cambios ajenos al flujo de brechas.
No se ejecutan distribución Firebase ni publicación de APKs. Las pruebas de integración
con APIM real quedan pendientes: todavía usa mocks y PUT no está confirmado.
Las pruebas de estado son JVM; no sustituyen una prueba visual instrumentada en dispositivo.
