# Brechas: por oferta y mercado general

Base de esta rama: `origin/develop` actualizado en `ffc694d` (PR #34).
Rama: `feat/skills-gap-ux-redesign`, en un worktree aislado. No se modifican main/develop,
backend, APIM, OAuth, certificados, Web ni base de datos. Tampoco cambian los contratos de cursos.

## Acceso y experiencia

**Desde Análisis de Brechas:** «Por oferta» es el modo inicial. Primero se selecciona un CV
explícitamente y se obtiene su detalle. Después aparecen ofertas de `JobOfferRepository`
(`GET /jobs`), con puesto, empresa, ubicación, búsqueda y páginas de 10. No se usa una lista
local de ofertas ni se elige el primer CV automáticamente. «Analizar habilidades» utiliza
el mismo repositorio y endpoints del flujo existente.

**Desde Ofertas laborales:** detalle → «Analizar con mi CV». Se conserva la oferta y solo se
solicita el CV. Se cierra el detalle antes de abrir la hoja; no se anidan hojas. El resultado
usa exactamente los mismos componentes que la pantalla principal.

**Mercado general:** pestaña independiente con la instantánea, selección de CV, entrada manual,
GET/PUT, cursos y paginación existentes. Explica que se compara con una referencia configurada
por el servicio, no con una oferta específica ni un mercado en tiempo real. El feature flag
`SKILLS_GAP_HTTP_020_ENABLED` sigue controlando únicamente este modo. El ViewModel de mercado
se crea al abrir su pestaña; no bloquea ni inicia solicitudes al entrar al modo por oferta.

## Jerarquía del resultado

- Encabezado con puesto, empresa, CV y acciones para cambiar la selección. Desde Ofertas,
  la oferta queda fija y la acción disponible es cambiar el CV.
- Resumen compacto: nombres documentados/total, cobertura documental y barra de cobertura.
  Los contadores de niveles se refieren explícitamente a las habilidades documentadas.
- Habilidades por mejorar y que cumplen: barras comparativas con cuatro posiciones discretas
  y etiquetas de nivel. Son posiciones ordinales, no porcentajes de dominio. Las listas
  comparables muestran cuatro inicialmente y permiten desplegar el resto.
- Habilidades ausentes: chips compactos, con nivel requerido si el backend lo especifica.
  Nunca se asigna principiante a una habilidad ausente.
- Habilidades documentadas sin nivel obligatorio: grupo independiente; no se dibuja cero ni
  se inventan requisitos. Las respuestas anteriores sin niveles siguen siendo presentables.
  Si la evaluación no llegó, se muestra «Sin evaluar», sin confundirlo con una exigencia no especificada.
- Evidencia, fuente, déficit (incluido cero), conflictos, menciones opcionales/negadas y
  metodología quedan dentro de «Ver detalles del análisis», cerrado inicialmente.
- Cursos en un desplegable: conserva la tarjeta existente con título, plataforma, enlace,
  duración y estado de pago conocido. Un curso compartido se muestra una vez. La ausencia de
  cursos se resume en un solo contador y los nombres se pueden desplegar voluntariamente.
  Una evaluación independiente distingue «sin consultar» de «no hay coincidencias».

El caso de aceptación «Asistente de TI» muestra 2/7, 28,57 %, SQL intermedio frente a principiante
como cumplimiento, Excel intermedio sin nivel requerido y cinco habilidades no documentadas.
No genera una nota de empleabilidad ni un porcentaje nuevo de cumplimiento de niveles.

## Componentes y arquitectura

| Archivo/componente | Responsabilidad |
| --- | --- |
| `SkillsGapScreen.kt` / `SkillsGapModeContent` | Dos modos, con instanciación separada del flujo de mercado. |
| `JobSkillsGapSheet.kt` / `JobSkillsGapContent` | Una presentación compartida para la hoja y la pantalla, con un único desplazamiento vertical. |
| `JobSkillsGapViewModel.kt` | Selección explícita, CV completo, búsqueda paginada de ofertas, cancelación y orquestación existente de cursos/niveles. |
| `JobGapPresentation.kt` | Proyección para agrupar datos del backend. No compara competencias ni calcula déficits. |
| `JobGapResult.kt` / `SkillLevelComparisonChart` | Resumen, barras ordinales, grupos compactos y desplegables accesibles. |
| `CvProNavGraph.kt` | Conserva destinos y conecta «Mis CVs» desde la pantalla de brechas. |
| `NetworkModule.kt` | Cliente de SkillsGap con reintentos automáticos de conexión desactivados; conserva base URL, interceptores y tiempos de espera. |
| `skills_gap_ux.xml`, `job_skills_gap.xml` (es/en) | Textos breves, traducciones y retirada de mensajes de resultado que ya no tienen consumidores. |
| `src/debug/.../JobGapPreviews.kt` | Fixtures aislados de preview, excluidos de Release y de la selección real de ofertas. |

Se reutilizan `JobSkillsGapRepository`, sus DTO/mappers y `ApiJobSkillsGapRepository`,
`CvRepository`, `JobOfferRepository` y `CourseCard`. No hay un segundo motor ni repositorio.
`recommendations` sigue enviando únicamente IDs/habilidades; `evaluate` añade descripción
y requisitos. El repositorio obtiene detalle de oferta cuando la proyección de lista lo requiere.

## Error de conexión: hallazgos y política

No hay trazas del incidente real original ni una causa reproducida. No se atribuye a Azure,
cold start, token o cobertura de red. El cliente existente tiene 20 segundos de espera de conexión
y lectura; el análisis incluye POSTs separados y puede consultar detalle de oferta.

La mejora es verificable sin inventar una causa:

- No hay reintentos automáticos de POST en el ViewModel. El cliente específico de SkillsGap
  desactiva `retryOnConnectionFailure`; los demás servicios conservan su configuración.
- El reintento se activa con una pulsación explícita. Si cursos ya respondió y falla la evaluación,
  se conservan los cursos y solo se repite evaluate. No se vuelve a enviar recommendations.
- Ante `CATALOG_UNAVAILABLE`, se conserva la opción explícita de consultar solo niveles.
- Un error conserva CV y oferta. Los mensajes distinguen red, sesión/permisos, ruta,
  datos incompatibles y catálogo sin exponer excepciones o cuerpos remotos.
- El estado de carga se establece antes de lanzar la corrutina. Cambiar CV/oferta cancela
  operaciones; `ensureActive` impide publicar resultados tardíos, incluso si el repositorio
  termina después de la cancelación. La búsqueda tiene su propia operación y debounce de 350 ms.
- `LoggingInterceptor` no cambia: solo registra el código HTTP, sin tokens, URLs, datos del CV
  ni evidencia. No se recabaron credenciales o datos del teléfono físico para las pruebas.

## Android Studio y prueba manual

1. Abrir el worktree como proyecto Android, sincronizar Gradle y seleccionar variante Debug.
2. Configurar `APIM_BASE_URL` y `GOOGLE_WEB_CLIENT_ID` por el mecanismo local ya existente,
   sin subir credenciales. Configurar el flag de mercado solo si se desea comprobar ese modo.
3. Ejecutar en un emulador/dispositivo de pruebas con su OAuth ya registrado e iniciar sesión.
4. Menú → Análisis de Brechas → Por oferta. Seleccionar CV, buscar una oferta, cargar otra página,
   elegir una y analizar. Cambiar CV/oferta y confirmar que el resultado anterior desaparece.
5. Probar la misma oferta desde Ofertas → detalle → Analizar con mi CV; comprobar que no pide
   elegir otra oferta y que Postularme conserva su acción.
6. Revisar cobertura, comparación SQL/Excel, chips, detalles y cursos. Activar fuente grande y
   probar español/inglés. El detalle y los cursos deben expandirse sin crear otro scroll vertical.
7. Simular un fallo controlado del servicio/red en el entorno de pruebas. Confirmar selección
   conservada, reintento manual, cursos parciales conservados y opción de evaluación independiente.
8. Abrir Mercado general con flag activado y desactivado. Con él activado, comprobar lectura,
   selección de CV, entrada manual, reemplazo explícito y paginación. Verificar Inicio, Perfil y CVs.

Las previews están en `app/src/debug/java/com/cvpro/app/ui/screens/skillsgap/JobGapPreviews.kt`
para español, inglés y fuente 1,6. No consumen APIM ni sustituyen las ofertas del repositorio.

## Validación automatizada y capturas

```powershell
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest testReleaseUnitTest lintDebug --console=plain
git diff --check
.\gradlew.bat assembleDebugAndroidTest --console=plain
```

La suite unitaria conserva las pruebas existentes y agrega selección/búsqueda/paginación,
respuestas tardías, caso 2/7, niveles faltantes, agrupación de cursos, error/reintento con selección
y reintento exclusivo de niveles. MockWebServer verifica un único POST cuando la conexión falla.

Resultado final local: **BUILD SUCCESSFUL**, 100 pruebas unitarias Debug y 100 Release aprobadas,
sin fallos ni errores; Lint con 0 errores y 107 advertencias del proyecto. `git diff --check` pasó
y la inspección de recursos encontró 0 identificadores duplicados por qualifier.
La suite Compose final aprobó **11 pruebas** en el emulador Pixel_8 (API 35), con informe
completo `OK (11 tests)`. Se inspeccionaron las cuatro capturas, incluida la fuente 1,6.
La revisión del diff confirmó que API, DTO, mapper/repositorio de análisis, autenticación y
ViewModel de postulaciones/ofertas conservan su implementación anterior.

La suite Compose está en `src/androidTest/.../JobGapUiTest.kt`. Se instala y ejecuta solo en el
emulador elegido; no usar `connectedDebugAndroidTest` indiscriminadamente si también hay un teléfono conectado:

Su runner de fixtures usa una `Application` de pruebas sin inicializar cuentas ni notificaciones.
No sustituye el arranque de la app real ni prueba el login. El CI compila también el APK de tests;
la ejecución y las capturas se realizan localmente en el emulador.

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
$serial = 'emulator-5554'
& $adb -s $serial install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk
& $adb -s $serial install --no-streaming -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s $serial shell am instrument -w -r -e class com.cvpro.app.ui.screens.skillsgap.JobGapUiTest com.cvpro.app.debug.test/com.cvpro.app.test.GapUiTestRunner
```

Durante la preparación hubo un ANR de arranque del proceso de instrumentación y cortes de
salida ADB. Son incidencias del entorno de QA, no una reproducción del error APIM reportado.
La ejecución final se repitió con el runner aislado y su informe se guardó antes de recuperarlo:

```powershell
& $adb -s $serial shell -n 'nohup am instrument -w -r -e class com.cvpro.app.ui.screens.skillsgap.JobGapUiTest com.cvpro.app.debug.test/com.cvpro.app.test.GapUiTestRunner > /data/local/tmp/gap-ui-results.txt 2>&1 < /dev/null &'
# Al finalizar el runner, recuperar y comprobar "OK (11 tests)":
& $adb -s $serial pull /data/local/tmp/gap-ui-results.txt build/gap-ui-results.txt
```

Capturas del emulador con datos de aceptación simulados, **no de una sesión Azure autenticada**:

- [Resumen y comparación](previews/gap-result-es.png).
- [Selector de oferta](previews/gap-selection-es.png).
- [Cursos agrupados](previews/gap-courses-es.png).
- [Inglés con fuente 1,6](previews/gap-large-font-en.png).

Pendiente: validación autenticada del rediseño contra APIM en un dispositivo real, reproducción
del primer fallo de conexión reportado y recorrido manual completo de autenticación/navegación.
Las pruebas Compose comprueban la acción de postulación, pero no crean una postulación real.
