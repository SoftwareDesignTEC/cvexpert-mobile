# Verificación pendiente de brechas por oferta mediante APIM

Esta guía prepara una prueba; no registra una ejecución E2E ni un despliegue. Se revisó el contrato de Brecha/Cursos `develop` en `3ef43f37a13e26c897812bce334dceac51fa76bc` y Android parte de `4148b00e0894eb74aef68f168a07ca62d04da14a` (merge del PR #31). El estado actual de Azure no se ha inspeccionado.

La integración del PR #34 en develop usa una única `JobSkillsGapSheet`: solicita recomendaciones
con el contrato anterior y niveles mediante evaluate con descripción/requisitos. Conserva cursos
si falla la evaluación y permite evaluar sin cursos cuando falla el catálogo. El backend ya está
desplegado según el contexto de la tarea; la comprobación autenticada desde Android sigue pendiente.
Ver [arquitectura final y validación local](job-offer-skills-gap-evaluation.md). Para comprobar niveles,
usar el backend con la extensión del PR #24 de Brecha/Cursos además de las operaciones anteriores.

## Condiciones previas y responsables

| Responsable | Comprobación necesaria |
| --- | --- |
| Gabriel / APIM e infraestructura | Backend desplegado con las implementaciones de los PR #21 y #23 de Brecha/Cursos (la revisión citada, o una posterior que conserve sus contratos); identificar el SHA/artefacto activo, no solo consultar `/health` |
| Gabriel / APIM | Publicar POST `/api/v1/skills-gap/evaluate` y `/api/v1/skills-gap/recommendations`, dirigidos respectivamente a `/skills-gap/evaluate` y `/skills-gap/recommendations` del backend Brecha, preservando las operaciones anteriores |
| Gabriel / seguridad | Validación de JWT Google firmado, expiración, issuer y audience del cliente; sobrescribir una única `X-User-Id` con el `sub` validado; presentar el certificado cliente autorizado hacia App Service; comprobar vigencia y allowlist sin divulgar claves |
| Administración PostgreSQL y revisión editorial | Conectividad y TLS del runtime, permisos de lectura en `skills`, `courses` y `course_skills`, cursos reales aprobados y asociaciones explícitas; resolver conflictos del lote y autorizar la carga por separado |
| Android / integración | APK de esta rama o posterior, sesión de prueba válida y `APIM_BASE_URL` de la revisión autorizada, terminada en `/api/v1/`; registrar versión y resultados sin datos personales |

`NetworkModule` construye un único Retrofit con `AuthInterceptor` y `LoggingInterceptor`, y lo inyecta a `SkillsGapApi`. Los POST son relativos y no empiezan con `/`, por lo que conservan el prefijo de `APIM_BASE_URL`; no requieren cambios de código ni URLs internas. El módulo añade la barra final cuando falta. Una URL vacía utiliza un host `.invalid` y no permite integración real. La prueba local con MockWebServer comprueba `/api/v1/` para ambos POST. El flag `SKILLS_GAP_HTTP_020_ENABLED` pertenece al flujo anterior, no habilita estas nuevas operaciones.

Gabriel debe revisar políticas efectivas y su herencia, backend y reescritura de rutas. App Service debe exigir mTLS, sin exclusiones ni bypass; no debe confiar en un certificado o una identidad aportados por Android. El backend no valida tokens Google. Rechazar query y `userId` en el cuerpo; no enviar datos personales adicionales. Mantener JSON máximo 102400 bytes y `Cache-Control: no-store`, incluidos errores. Si APIM valida tamaño antes del backend, acordar el retorno `413`: la política `validate-content` devuelve `400` por defecto para prevención inbound ([referencia oficial](https://learn.microsoft.com/en-us/azure/api-management/validate-content-policy)). No transformar fallos en respuestas exitosas vacías ni registrar bodies/tokens en trazas del gateway.

## Datos mínimos y coincidencia parcial

Utilizar una cuenta y un entorno de prueba autorizados, con un CV y una oferta obtenidos mediante sus repositorios/APIM reales. No hacen falta nombre, teléfono ni correo en la proyección de análisis. Para el caso de referencia, el CV declara solo Python `advanced` y la oferta identifica Python y SQL. Usar IDs devueltos por los servicios; los siguientes son marcadores sintéticos:

```json
{
  "cv": {"id": "ID_CV_DE_PRUEBA", "skills": [{"name": "Python", "level": "advanced"}]},
  "jobs": [{"id": "ID_OFERTA_DE_PRUEBA", "skills": ["Python", "SQL"]}]
}
```

Si no hay una oferta de prueba con esas habilidades explícitas, el equipo debe disponer de una entrada autorizada o adaptar las expectativas a una oferta existente; no sustituir los requisitos por texto general de mercado. El análisis no comprueba titularidad o competencias y sus resultados no sirven para contratación o autorización.

1. En Mis CVs, editar el CV: seleccionar nivel, guardar y volver a cargar. Comprobar en español e inglés que cambia la etiqueta, mientras el request conserva el valor canónico. Un nivel anterior inválido debe conservarse hasta corregirlo y el editor debe impedir guardarlo. Un nombre personalizado es válido; idiomas no cambian. Verificar también que se pueda quitar una entrada incompleta y que TalkBack permita abrir el selector, leer las opciones y reconocer el error.
2. El responsable de integración envía la proyección mínima a POST `/api/v1/skills-gap/evaluate` con la sesión de prueba mediante una herramienta autorizada, sin guardar el token en el repositorio ni en la evidencia. Esperar `200`, `dataSource: client_request`, mismo CV/oferta, `partial`, dos requisitos, una coincidencia, cobertura documental `50`, Python con `cvLevel: advanced` y SQL no documentada. No debe requerir snapshot, `SKILLS_GAP_MARKET_KEY` ni PostgreSQL.
3. En Ofertas → **Analizar con mi CV**, elegir esa oferta y CV. Android obtiene el detalle por `CvRepository.getCvById` y llama a recommendations. Comprobar la selección y los mismos campos de evaluación. No interpretar la cobertura como probabilidad de contratación ni como dominio comprobado.

La UI inicia recommendations; la evaluación pura se solicita desde su acción explícita tras `CATALOG_UNAVAILABLE`. Probar evaluate directamente no exige provocar una caída de PostgreSQL compartido.

## Cursos reales y estados diferenciados

Para el caso positivo, administración debe haber autorizado previamente al menos un curso real asociado a SQL mediante `course_skills`, con fuente editorial revisada. Confirmar título, plataforma, URL y modalidad contra el catálogo aprobado, sin fijar un ID ni una duración ficticios. Esperar `catalogStatus: consulted`, una recomendación SQL con `catalogMatch: matched` y el curso asociado. Se devuelven hasta cinco por habilidad en orden de ID numérico; ese orden no indica calidad. `durationHours: null` debe continuar como duración desconocida en Android.

| Escenario controlado | API y comportamiento Android esperado |
| --- | --- |
| CV sin habilidades y oferta con requisitos | `partial`, 0 % documental; consulta catálogo si se solicitan recomendaciones. No es fallo de carga de CV |
| Oferta sin nombres identificables | `insufficient_information`, cobertura `null`, recomendaciones vacías y `catalogStatus: not_needed`; UI de información insuficiente |
| CV cubre todos los nombres | `complete`, 100 % documental, `not_needed`; sin cursos ni consulta PostgreSQL |
| Catálogo accesible sin cursos elegibles | `200`, `consulted`, `courses: []`; UI de ausencia de coincidencias, sin mensaje de caída |
| Nombre no localizado o ambiguo | `catalogMatch: not_found` o `ambiguous`, cursos vacíos; no inventar asociaciones |
| Consulta necesaria no disponible | `503 CATALOG_UNAVAILABLE`; reintentar o **Consultar solo la evaluación**, que llama a evaluate y conserva el aviso de catálogo fallido; nunca simular recomendaciones |
| Ruta no publicada | `404`, `405` o `501` se muestran como operación no disponible. Otros fallos del gateway quedan como disponibilidad/error; no son catálogo vacío |
| Falla de red o detalle del CV | Estado de error y reintento. No convertir el detalle fallido en CV vacío ni enviar el análisis |
| Nivel ausente o no permitido | Corrección explícita en Mis CVs, sin nivel inventado; volver a seleccionar/cargar el CV corregido |
| Token ausente/inválido | Rechazo `401` del gateway, sin resultado exitoso; verificar también issuer/audience y expiración |
| Identidad ausente/ambigua tras mTLS válido | Backend `401 USER_IDENTITY_REQUIRED`; verificar que APIM sobrescriba headers del consumidor |
| Certificado inválido/no autorizado | Rechazo TLS/App Service o `403 APIM_CERTIFICATE_REQUIRED` si llega al backend; un fallo de handshake puede aparecer como error del gateway |
| JSON/DTO/query inválidos o cuerpo excesivo | `400` de validación o `413` por superar 100 KiB; no `503 CATALOG_UNAVAILABLE` |

Las pruebas de caída, catálogo vacío o certificado alternativo requieren un entorno aislado autorizado. No degradar infraestructura compartida para completar la matriz. Las pruebas con todas las habilidades documentadas o sin requisitos pueden comprobar que recommendations no depende de BD en esos casos. Un fallo de seguridad global puede dar `503 APIM_SECURITY_UNAVAILABLE`, distinto del fallo de catálogo.

## Privacidad y evidencia de ejecución

`LoggingInterceptorTest` ya comprueba que debug solo emita `HTTP <código>`, sin leer cuerpos ni registrar URL/query/headers, incluso para `/cvs/{id}`, los POST y rutas futuras; release no emite mensajes y los errores de transporte no se registran con detalles. No se duplica esa prueba. Durante la verificación manual, usar exclusivamente datos sintéticos y revisar Logcat del proceso con login, carga del CV, análisis exitoso y errores; buscar filtraciones de esos datos, tokens y cuerpos en el conjunto de diagnósticos. La prueba del interceptor no certifica todos los SDK ni las trazas de Azure. No compartir volcados completos de Logcat o capturas con información privada.

Registrar por caso: fecha, entorno/revisión APIM, SHA del backend realmente activo, commit y variante del APK, identificador de caso, estado HTTP, código de error y resultado esperado/observado. Usar alias de selección (CV-A/Oferta-A), métricas y capturas saneadas. Correlacionar con un ID diagnóstico que no sea el usuario; no guardar JWT, certificado privado, CV completo, email, teléfono ni URLs privadas con query. El responsable puede comprobar certificado/identidad en la configuración efectiva sin exportar el material sensible.

Completar después de ejecutar: casos aprobados/fallidos, limitaciones, responsables y evidencia saneada. CI, MockWebServer, compilaciones y lint prueban comportamiento local; no acreditan APIM, cursos disponibles ni un E2E realizado. La publicación y seguridad siguen dependiendo de Gabriel/infraestructura, y las recomendaciones reales de PostgreSQL y la aprobación editorial/administrativa. Esta rama no despliega, importa datos ni modifica otros proyectos.

## Validación local del selector — 11 de noviembre de 2026

Se ejecutó `gradlew.bat testDebugUnitTest testReleaseUnitTest assembleDebug assembleRelease lintDebug` con el SDK local y daemon Java 17. Las cinco tareas finalizaron correctamente. Hubo 43 pruebas JUnit en debug y 43 en release, sin fallos, errores ni omisiones: siete nuevas del editor y las 36 existentes, incluidas privacidad, contratos y estados de brechas. La prueba Retrofit existente ahora comprueba ambos POST bajo `/api/v1/`, sin duplicar la prueba de contrato.

Se generaron `app-debug.apk` y `app-release-unsigned.apk`; release no está firmado para distribución. Lint finalizó con cero errores y 103 advertencias, sin incidencias en el selector o la validación nuevos. Las advertencias adicionales frente al informe previo son avisos de versiones de herramientas/dependencias; no se actualizaron dependencias fuera del alcance. Estas pruebas no incluyen interacción instrumentada de Compose, comprobación manual con TalkBack ni acceso E2E a APIM.
