# Catálogo y recomendaciones por oferta

La rama parte de `origin/develop` en `f1f392c`, con los PR #34 y #35 incluidos. El análisis por oferta conserva sus repositorios, contratos, cobertura documental, comparación ordinal y evidencia. Android no calcula niveles ni déficits.

## Comportamiento

Inicio y Perfil dejan de consultar el análisis general y de dibujar radares. Brechas sigue disponible en el menú y abre directamente la selección de CV y oferta. El detalle de Ofertas conserva su hoja con oferta fija y sus acciones de postulación.

El nuevo destino **Cursos** abre **Explorar cursos**. Usa el Retrofit existente, `APIM_BASE_URL`, `AuthInterceptor` y la ruta relativa `courses`: con la base configurada del gateway resulta `GET /api/v1/courses`. No contiene otro host ni encabezados manuales de identidad.

Los filtros admitidos son plataforma exacta, habilidad exacta y gratuito/de pago, combinados con la paginación. Escribir no consulta automáticamente: se usa **Aplicar filtros**. No hay búsqueda por título, sinónimos ni ordenamientos locales. Los espacios y mayúsculas de valores no vacíos se conservan. **Limpiar filtros** vuelve a la primera página sin filtros.

Las tarjetas muestran título, plataforma, habilidades, duración cuando está definida y condición gratuito/de pago. Los enlaces admiten HTTP(S) sin credenciales embebidas; un fallo al abrirlos se comunica en la tarjeta. La duración nula no se convierte en cero, y la duración numérica se presenta con recursos localizados.

`totalRecords` procede del servidor y determina si hay otra página. Los registros marcados `[DEMO]` o `[TEST]`, y los enlaces a dominios reservados de ejemplo, se excluyen de las tarjetas. Se presenta un aviso explícito de defecto de datos del backend, con el total original intacto. Una página sin cursos publicables y con total positivo no se anuncia como catálogo vacío. Esta protección se aplica también a las recomendaciones por habilidades no documentadas. No se cargan seeds ni se modifican datos remotos.

## Dos motivos de recomendación

1. **Habilidad no documentada:** se conservan exclusivamente los cursos recibidos de `POST /skills-gap/recommendations`. Se indica que la habilidad no está documentada en el CV. Las tarjetas compartidas entre varias habilidades ausentes se agrupan por ID y conservan todos los nombres asociados. No se afirma que la persona desconoce la habilidad.
2. **Nivel por mejorar:** se muestran secciones por habilidad únicamente si `/evaluate` informa `documented_skill_below_required`, detección `explicit`, déficit positivo y nivel requerido definido. Al expandir la habilidad se solicita `GET /courses?skillName=<nombre exacto>&page=1&pageSize=5`. Se muestran el nivel declarado y el requerido, y se explica que el catálogo no verifica el nivel educativo del curso. Habilidades suficientes, desconocidas o con detección contradictoria no disparan estas consultas.

La sección **Cursos sugeridos** permanece desplegable. Abrir solo esa sección no dispara consultas por déficit: hay que expandir la habilidad correspondiente. Cada habilidad conserva carga, error, resultados y página; las reaperturas no repiten consultas exitosas. **Cargar más cursos** obtiene una página de cinco y elimina IDs repetidos dentro de la habilidad. Las asociaciones siguen visibles en cada sección y en las habilidades de las tarjetas.

Las habilidades no documentadas sin cursos conservan una agrupación secundaria desplegable, siguiendo el rediseño del PR #35. Desde ella se puede explorar el catálogo por cada nombre exacto, sin repetir el mismo mensaje vacío en la vista principal.

Hay como máximo dos consultas de déficit simultáneas. Cambiar de CV, oferta, cerrar la hoja o iniciar otro análisis cancela las solicitudes anteriores y limpia su estado. También se comprueba la cancelación y la generación del análisis antes de aceptar respuestas tardías. No se recorren todas las páginas ni se hacen consultas por cada habilidad automáticamente.

**Explorar todos los cursos** abre el catálogo general; **Explorar cursos de …** transmite la habilidad exacta como argumento de navegación codificado, tanto desde Brechas como desde la hoja de Ofertas.

## Errores y estados vacíos

El catálogo distingue carga, resultados, catálogo vacío, filtros sin coincidencias y página sin registros publicables. Se diferencian autenticación (401/403), ruta no disponible (404/405), conexión y servicio/base de datos no disponible. Los datos de respuesta incompatibles tienen su propio mensaje. El reintento repite la página fallida; un error de una página posterior conserva la página ya cargada.

Los errores de cursos por déficit permanecen en su habilidad y no borran la evaluación ni los cursos obtenidos por `/recommendations`. El fallo de `/evaluate` conserva los cursos por habilidades no documentadas, según el flujo existente. No hay fallback a datos ficticios. Sin cursos asociados se muestra ese estado expresamente.

## Archivos principales

Todos los caminos siguientes son relativos a `app/src`:

- `main/java/com/cvpro/app/data/remote/api/CourseApi.kt`, `data/remote/dto/CoursePageDto.kt`, `data/mapper/CourseMapper.kt`, `data/repository/ApiCourseRepository.kt`: contrato y acceso al catálogo.
- `main/java/com/cvpro/app/domain/model/Course.kt`, `domain/repository/CourseRepository.kt`: catálogo, filtros, paginación, errores y selección de déficits elegibles.
- `main/java/com/cvpro/app/di/NetworkModule.kt`, `data/repository/RepositoryModule.kt`: inyección con la red autenticada existente.
- `main/java/com/cvpro/app/ui/screens/courses/CoursesScreen.kt`, `CoursesViewModel.kt`: pantalla y estado del catálogo.
- `main/java/com/cvpro/app/ui/components/CourseCard.kt`: tarjeta reutilizable extraída de la pantalla antigua. `RadarChart.kt` se elimina al quedar sin consumidores.
- `main/java/com/cvpro/app/ui/screens/skillsgap/SkillsGapScreen.kt`, `JobSkillsGapSheet.kt`, `JobSkillsGapViewModel.kt`, `JobGapResult.kt`: entrada por oferta, carga bajo demanda y presentación por motivo.
- `main/java/com/cvpro/app/domain/model/SkillsGapAnalysis.kt`, `data/mapper/JobSkillsGapMapper.kt`: conservación de duración numérica para presentación localizada de cursos.
- `main/java/com/cvpro/app/ui/navigation/Routes.kt`, `CvProNavGraph.kt`, `ui/screens/jobs/JobsScreen.kt`: destino Cursos y navegación contextual.
- `main/java/com/cvpro/app/ui/screens/home/HomeScreen.kt`, `HomeViewModel.kt`, `ui/screens/profile/ProfileScreen.kt`, `ProfileViewModel.kt`: retirada de radares, estado y consultas generales.
- `main/res/values/courses.xml`, `main/res/values-en/courses.xml`: textos en español e inglés.
- `debug/java/com/cvpro/app/ui/screens/skillsgap/JobGapPreviews.kt`: actualización del preview existente al flujo directo.
- Pruebas: `test/java/com/cvpro/app/data/CourseRepositoryTest.kt`, `ui/screens/courses/CoursesViewModelTest.kt`, `ui/screens/skillsgap/OfferOnlyArchitectureTest.kt`, `JobSkillsGapViewModelTest.kt`, `EmptyCourseRepository.kt` y adaptación de constructores en `JobGapEvaluationViewModelTest.kt` y `JobGapUxTest.kt`. Se retira la prueba de etiquetas de la pantalla de mercado eliminada de `SkillsGapViewModelTest.kt`.
- Compose: `androidTest/java/com/cvpro/app/ui/screens/courses/CoursesUiTest.kt` y `ui/screens/skillsgap/JobGapUiTest.kt`.

## Verificación y límites

Las pruebas de red usan MockWebServer; las de ViewModel usan repositorios locales y respuestas demoradas, incluidas respuestas que ignoran la cancelación. No son una prueba E2E de Google, APIM o PostgreSQL.

El 11 de noviembre de 2026 se ejecutó satisfactoriamente:

```powershell
.\gradlew.bat assembleDebug assembleRelease testDebugUnitTest testReleaseUnitTest lintDebug --console=plain
git diff --check
```

El mismo comando se repitió incluyendo `assembleDebugAndroidTest` para construir las pruebas visuales. Resultado: **116 pruebas unitarias en debug y 116 en release, sin fallos ni omisiones; lint con 0 errores y 153 advertencias**. No se añadieron dependencias de Gradle. Las comprobaciones estructurales de Inicio/Perfil y la entrada directa se distinguen de las pruebas de repositorio y de interacción Compose.

También se ejecutaron **15 pruebas Compose aprobadas en `emulator-5554`** con el runner existente `com.cvpro.app.test.GapUiTestRunner`. Cubren selección, oferta fija, postulación existente, evidencia, desconocimiento de niveles, carga bajo demanda, catálogo vacío, duración, filtros/paginación, reintento, ocultación de registros de prueba, español/inglés, tema oscuro y fuente 1,6×. Las dos pruebas afectadas por el último ajuste de agrupación se repitieron sobre su APK actualizado: **2 aprobadas, 0 fallos y 0 omitidas**, confirmado por el resumen del runner.

Capturas de estados locales de prueba, sin consultas a APIM ni datos personales:

- [Catálogo vacío en español](evidence/courses/courses-empty-es.png).
- [Error en inglés, tema oscuro y fuente grande](evidence/courses/courses-error-dark-en-large.png).
- [Defecto de datos sin alterar el total del servidor](evidence/courses/courses-data-defect-es.png).
- [Cursos por déficit y niveles declarados/requeridos](evidence/courses/gap-level-courses-es.png).

El inventario `cvexpert-docs/contratos/apim-rutas.json`, fechado 2026-11-08, registra `GET /courses`, operación `get-courses`, destino `brecha` y prefijo `/api/v1`. La consulta HTTP sin sesión desde este entorno agotó el tiempo de espera. La consulta de administración de Azure devolvió `AuthorizationFailed` para el ámbito consultado; eso no demuestra que la operación esté ausente. No se solicita un endpoint nuevo ni se cambian políticas, certificados o infraestructura. Queda pendiente una lectura autenticada positiva del gateway y su PostgreSQL real.

El descubridor Microsoft Learn del PR #25 genera candidatos locales pendientes de revisión; no publica cursos automáticamente. Un catálogo vacío es un resultado soportado. La publicación de cursos reales y sus asociaciones sigue siendo una dependencia editorial y operativa del backend.

## Prueba real en Android

1. Usar el build de esta rama con la configuración habitual de `APIM_BASE_URL`, sin incorporar certificados o tokens al APK. Iniciar sesión normalmente con Google.
2. Abrir Inicio y Perfil y comprobar que se mantienen CVs, perfil y ofertas sin solicitudes automáticas a `GET /skills-gap`.
3. Entrar a Brechas: debe mostrarse directamente la selección de CV y oferta. Repetir también desde el detalle de una oferta; esa oferta debe quedar fija y las postulaciones seguir funcionando.
4. Analizar un CV/oferta real que tenga una habilidad ausente y otra documentada con déficit explícito. Confirmar cobertura, niveles y evidencia frente al backend. Abrir Cursos sugeridos: los de la habilidad ausente deben provenir de `/recommendations`.
5. Expandir la habilidad con déficit: debe producirse una sola lectura del catálogo, con el nombre exacto y página de cinco. Cerrar y abrir no debe repetirla. La ausencia de cursos debe ser explícita. Las habilidades suficientes o con requisito desconocido/contradictorio no deben consultar por déficit.
6. Cambiar de CV u oferta durante la carga y comprobar que no aparecen cursos de la selección anterior. Simular un fallo de catálogo y verificar que permanecen el análisis y los otros cursos; reintentar solo esa habilidad. Un fallo de evaluación tampoco debe borrar las recomendaciones ausentes ya cargadas.
7. Abrir Cursos desde el menú y desde una habilidad. Probar plataforma, habilidad y pago con valores exactos conocidos del catálogo. Verificar página siguiente/anterior y el total del servidor. No esperar coincidencia por título o por variantes de mayúsculas.
8. Comprobar catálogo vacío, filtros sin coincidencias, sesión expirada, ruta no disponible y pérdida de conexión en un entorno de prueba autorizado. Revisar también español/inglés, tema oscuro, fuente grande y navegación por accesibilidad. Registrar cualquier `[DEMO]`, `[TEST]` o enlace reservado devuelto como defecto de datos del backend, sin corregir el conteo en Android.

No se fusiona el PR, distribuye el APK ni se cargan datos en Azure como parte de esta entrega. La instalación local para comprobación visual se limita al emulador.
