# Análisis de brecha por oferta en Android

Base: `main` (`0dbaa0f`), rama `feat/job-offer-skills-gap-evaluation`.

Contrato consultado: [OpenAPI de Brecha en develop](https://github.com/SoftwareDesignTEC/cvexpert-ms-brecha-cursos/blob/develop/openapi.yaml)
y [niveles requeridos por oferta](https://github.com/SoftwareDesignTEC/cvexpert-ms-brecha-cursos/blob/develop/docs/job-required-skill-levels.md).
`GET /jobs/{id}` está documentado en `cvexpert-docs/contratos/ofertas-postulaciones.openapi.yaml`.

## Flujo y decisiones

En Ofertas → Ver detalle → Analizar brecha se abre una hoja Compose. Se cargan CV paginados
por `CvRepository` y el usuario elige uno; se obtiene su detalle por `getCvById`.
El botón Analizar brecha envía una evaluación de ese CV y una sola oferta.
Si faltan habilidades, descripción o requisitos en la lista, el repositorio consulta
`GET jobs/{id}`. Usa los campos de detalle disponibles y conserva los de lista cuando el
detalle omite un campo. Un error de detalle impide enviar una evaluación incompleta y permite reintentar.
Un detalle confirmado sin habilidades se evalúa como tal; no se extraen nombres en Android.

El callback de `JobOfferDetailSheet` es opcional; los usos de Panel y Postulaciones conservan
su comportamiento. El botón Postularme, su registro y la apertura de la publicación original
no cambian. Tampoco cambian GET `/skills-gap`, PUT `/skills-gap/input` ni recomendaciones.

- DTO de evaluación separados, mapper de datos, repositorio Hilt, ViewModel y hoja Compose.
- Se usa el Retrofit de `NetworkModule` y su `AuthInterceptor`; no se suministran encabezados
  de identidad ni certificados desde Android. No se exige `X-Data-Mode` a esta operación.
- Política del CV: cualquier nombre vacío/inválido, nivel no reconocido por `SkillLevel`,
  nombre de más de 200 puntos Unicode o más de 500 habilidades bloquea **todo** el envío.
  No se descartan habilidades ni se reemplazan niveles. Un CV vacío es válido: no documenta habilidades.
- Se validan los límites del contrato, Unicode y controles antes de enviar. El tamaño total
  se mide con Gson (el mismo conversor de Retrofit), en UTF-8, con máximo 100 KiB.
  No se recorta texto. Los duplicados contradictorios que rechace el backend producen un error de datos.
- Se envían descripción y requisitos, incluso vacíos si la oferta no tiene texto. Las respuestas
  anteriores sin `skillLevelAssessments`/`levelAnalysis` siguen mostrando cobertura y un aviso
  de que no se conocen los niveles requeridos.
- La comparación, extracción de evidencia y déficit pertenecen al backend. Android conserva
  sus resultados. `complete`/100 % describen nombres documentados; no son cumplimiento de niveles.
  `deficitSteps=null` no se convierte en cero; son pasos ordinales, nunca porcentajes.
- Conflictos, opcionalidad, niveles desconocidos y evidencia se muestran como texto sin interpretar HTML.
- La carga se marca antes de lanzar la corrutina para impedir dobles pulsaciones. Cambiar
  CV/oferta o cerrar cancela la operación. Antes de publicar resultados se comprueba que la
  corrutina siga activa; la hoja no presenta estado de otra oferta mientras cambia su argumento.
- Hay estados de carga, lista vacía, error y reintento de la operación que falló. Los errores
  400/401/403/404/413/503 se traducen a mensajes locales, sin cuerpos ni excepciones remotas.
  El logger existente omite el cuerpo de evaluación también en Debug.

## Ejemplo del request construido

Los tests usan `CvDto` y `JobOfferDto` convertidos con los mappers reales:

```json
{
  "cv": {
    "id": "cv1",
    "skills": [
      {"name": "Python", "level": "intermediate"},
      {"name": "SQL", "level": "advanced"}
    ]
  },
  "jobs": [{
    "id": "job1",
    "skills": ["Python", "SQL", "Docker"],
    "description": "Python avanzado y SQL básico.",
    "requirements": ["Docker intermedio"]
  }]
}
```

La respuesta de prueba del backend documenta Python con déficit 1, SQL con déficit 0 y
Docker sin `cvLevel` ni déficit. Estas son respuestas simuladas del contrato, no una llamada a Azure.

## Verificación local

Archivos de implementación (bajo `app/src/main/java/com/cvpro/app/`):

- `data/remote/api/SkillsGapApi.kt`, `data/remote/api/JobApi.kt`: operaciones Retrofit.
- `data/remote/dto/JobGapEvaluationDto.kt`, `data/mapper/JobGapEvaluationMapper.kt`: contrato y adaptación.
- `data/repository/ApiJobGapEvaluationRepository.kt`, `data/repository/RepositoryModule.kt`: repositorio y binding Hilt.
- `domain/model/JobGapEvaluation.kt`, `domain/repository/JobGapEvaluationRepository.kt`: modelos e interfaz.
- `ui/screens/skillsgap/JobGapEvaluationViewModel.kt`, `ui/screens/skillsgap/JobGapEvaluationSheet.kt`: estados y presentación.
- `ui/screens/jobs/JobsScreen.kt`, `ui/components/DetailSheet.kt`: entrada opcional al flujo.
- `data/remote/LoggingInterceptor.kt`: exclusión del cuerpo de evaluación.

También se agregan `app/src/main/res/values/strings_job_gap.xml` y
`app/src/main/res/values-en/strings_job_gap.xml`; las pruebas son
`app/src/test/java/com/cvpro/app/data/JobGapEvaluationTest.kt` y
`app/src/test/java/com/cvpro/app/ui/screens/skillsgap/JobGapEvaluationViewModelTest.kt`.
Este documento completa los 18 archivos de la entrega.

Desde la raíz del worktree, con SDK Android instalado y accesible:

```powershell
$env:ANDROID_HOME = 'C:\ruta\al\Android\Sdk'
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
git diff --check
```

Las pruebas nuevas cubren proyección real de modelos, transporte de ambos textos, niveles del
contrato, respuestas antiguas, cobertura completa con déficit, ausencia de habilidades,
CV vacío/inválido, límites Unicode/tamaño total, detalle de oferta, errores HTTP, token mediante
el interceptor existente, reintentos, selección explícita, doble pulsación y respuestas tardías
después de cambiar de CV/oferta. También se ejecuta la suite existente del análisis de mercado.

Resultado final local (11 de noviembre de 2026): `assembleDebug`, `testDebugUnitTest` y
`lintDebug` terminaron con `BUILD SUCCESSFUL`. Pasaron 41 pruebas (20 nuevas), sin fallos ni
errores. Lint terminó con 0 errores y 99 advertencias del proyecto; no señala los archivos
nuevos de esta entrega. `git diff --check` y `git diff --cached --check` también pasaron.
El APK Debug se generó en `app/build/outputs/apk/debug/app-debug.apk`.

## Prueba real pendiente contra APIM

No se verificó Azure/APIM ni se ejecutó una sesión Android autenticada. El APK de validación local
no demuestra que las operaciones estén publicadas ni que las políticas del gateway sean correctas.

1. Confirmar con el responsable de APIM que están desplegados el backend de Brecha con análisis
   de niveles, `POST /skills-gap/evaluate` y `GET /jobs/{id}`, con sus rutas públicas/reescrituras.
   APIM debe validar Google y administrar la identidad y el certificado hacia el backend.
2. Configurar localmente `APIM_BASE_URL` (prefijo público con barra final) y
   `GOOGLE_WEB_CLIENT_ID` en `local.properties` ignorado por Git, o mediante el entorno ya soportado.
   No agregar certificados, secretos ni encabezados de identidad a la app.
3. Registrar el application ID Debug `com.cvpro.app.debug` y la firma Debug para el OAuth
   correspondiente. Compilar con `assembleDebug` e instalar el APK en un equipo con Google Play.
4. Iniciar sesión. Crear o elegir un CV con niveles canónicos, abrir una oferta real y seleccionar
   explícitamente ese CV en Analizar brecha. Confirmar que título/CV/evidencia corresponden a la selección.
5. Con una oferta controlada disponible para el entorno de prueba, verificar Python intermedio
   frente a avanzado (1), SQL avanzado frente a básico (0), Docker ausente (nivel CV/déficit desconocidos),
   texto opcional/conflictivo y oferta sin habilidades. No editar producción para fabricar estos datos.
6. Confirmar en trazas seguras del gateway una única evaluación por pulsación y su autenticación;
   no copiar tokens ni cuerpos de CV a reportes. Probar cambio rápido de CV/oferta, cierre y reintento.
7. Probar sesión vencida, permisos denegados y operación ausente en un entorno controlado.
   Verificar finalmente Postularme/apertura de oferta original y la pantalla de análisis de mercado.

No fusionar el PR ni distribuir automáticamente como parte de esta entrega.
