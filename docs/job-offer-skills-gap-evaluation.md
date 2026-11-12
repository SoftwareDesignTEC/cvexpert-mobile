# Integración del PR #34 en develop

La rama `feat/job-offer-skills-gap-evaluation` integra `origin/develop` mediante un merge normal.
El destino del PR #34 es `develop`. No se reescribe el historial remoto ni se fusiona el PR.

## Referencias y divergencia inspeccionadas

- Rama del PR antes de integrar: `056c537`, creada sobre `main` en `0dbaa0f`.
- `origin/develop`: `c257a8c`; `origin/main`: `d4968f8` al ejecutar `git fetch origin`.
- Ancestro común entre main/develop y entre la rama del PR/develop: `bb21148`.
- Develop aporta los PR #31/#32 (análisis por oferta, cursos y selector canónico de niveles)
  y la validación CI separada de distribución. La rama del PR ya hereda de main cambios
  de CV, fechas, IA, ubicación, autenticación y postulaciones. Se conserva ese historial;
  el diff del PR contra develop incluye esos cambios heredados, no solo la evaluación nueva.

Contratos consultados: [OpenAPI vigente](https://github.com/SoftwareDesignTEC/cvexpert-ms-brecha-cursos/blob/develop/openapi.yaml)
y [niveles requeridos por oferta](https://github.com/SoftwareDesignTEC/cvexpert-ms-brecha-cursos/blob/develop/docs/job-required-skill-levels.md).
`recommendations` rechaza `description`/`requirements`; `evaluate` los admite. No se modifican
contratos del backend ni se implementa comparación de competencias en Android.

## Arquitectura final: un solo flujo por oferta

La entrada está en Ofertas → detalle → Analizar con mi CV. El callback de `JobOfferDetailSheet`
es opcional; Postularme y los otros consumidores de la hoja conservan su comportamiento.

1. `JobSkillsGapSheet` muestra CV paginados mediante `JobSkillsGapViewModel` y `CvRepository`.
   No se selecciona uno automáticamente; se obtiene el detalle del elegido con `getCvById`.
2. `ApiJobSkillsGapRepository` consulta `GET /jobs/{id}` si faltan habilidades, descripción
   o requisitos en la oferta. Usa el detalle disponible, sin extraer ni inventar habilidades.
3. La acción normal consulta `recommendations` con el contrato original, sin texto adicional.
   El ViewModel conserva y presenta esos cursos antes de solicitar niveles a `evaluate`.
4. La evaluación envía descripción y requisitos. Solo se agregan sus niveles a los cursos
   si coinciden CV, oferta y habilidades documentadas/no documentadas de ambas respuestas.
   Si la oferta cambió entre peticiones, se informa incompatibilidad y no se mezclan datos.
5. Si falla la evaluación, los cursos ya disponibles se conservan. Si falla el catálogo con
   `CATALOG_UNAVAILABLE`, se mantiene la opción explícita «Consultar solo la evaluación»;
   esa acción no vuelve a consultar recomendaciones y puede presentar niveles sin cursos.

Solo existe `JobSkillsGapRepository`, su implementación y su binding Hilt; también un único
ViewModel y una única hoja por oferta. Se retiran las clases paralelas `JobGapEvaluation*`.
Los tests del PR original mantienen sus escenarios y nombres de suite, adaptados al flujo unificado.

El request de recomendaciones y el de evaluación son DTO distintos dentro de `JobSkillsGapDto.kt`.
Comparten la proyección mínima del CV. La respuesta y los modelos de dominio existentes se
extienden con `skillLevelAssessments`/`levelAnalysis`, opcionales por compatibilidad intencional.

## Datos, estados y privacidad

- Se reutilizan `NetworkModule` y `AuthInterceptor`. Android no introduce encabezados de
  identidad, certificados, credenciales ni URLs Azure fijas.
- `SkillLevel` define los cuatro valores válidos. Una habilidad con nivel inválido bloquea
  todo el envío; un nombre inválido o más de 500 habilidades también lo bloquea. No se filtran
  habilidades ni se convierten niveles desconocidos en principiante. Un CV vacío es válido.
- Se validan límites por puntos Unicode: 200 habilidades por oferta, 20 000 caracteres de
  descripción, 100 requisitos de 2 000 caracteres y JSON UTF-8 de 100 KiB. No hay truncamiento.
  Identificadores, controles y sustitutos Unicode inválidos se rechazan localmente.
- El repositorio y mapper validan identidad de la respuesta, cobertura, niveles y estados.
  Una respuesta antigua sin análisis de niveles conserva la cobertura y muestra el aviso correspondiente.
- El backend es quien calcula el déficit. `null` no se convierte en cero. Los pasos son ordinales,
  no porcentajes. `complete`/100 % solo indican nombres documentados, no cumplimiento de niveles.
- La hoja muestra niveles, evidencia textual, desconocidos, opcionalidad y conflictos. La evidencia
  se presenta como texto. No hay recomendaciones nuevas basadas en déficits de nivel.
- Cambiar CV/oferta o cerrar cancela la operación. Se comprueba actividad antes de publicar
  resultados y se bloquea la doble pulsación. La hoja oculta estado de otra oferta durante el cambio.
- Los errores HTTP se convierten en mensajes locales. Se conserva el logger de develop, que
  registra únicamente el código HTTP en todas las rutas: sin cuerpos, URLs, tokens ni evidencia.

## Resolución de los 22 conflictos

| Grupo | Resolución |
| --- | --- |
| `SkillsGapApi.kt` | Una declaración por POST; DTO textual solo en evaluate. GET/PUT conservan el contrato y feature gate de develop. |
| `SkillsGapMapper.kt`, `SkillsGapDto.kt`, `ApiSkillsGapRepository.kt`, modelo/interfaz de mercado | Se mantiene la selección de CV, snapshot, metadatos y tipos de develop. Se conserva la entrada manual de main. Un 409 solo solicita snapshot si GET devuelve `SKILLS_INPUT_REQUIRED`; otro 409 no se interpreta como datos faltantes. |
| `RepositoryModule.kt` | Un binding JobSkillsGap; se conservan los demás bindings de la rama, incluidos idioma, IA y notificaciones. |
| `JobsScreen.kt` | Una entrada al análisis en el detalle; conserva postulación, URL original, filtros y paginación de la rama. |
| `CvEditorSheet.kt` | Combina validaciones y campos del editor de main con el selector canónico y la validación de habilidades de develop. |
| `JobOfferMapper.kt`, DTO/modelo de oferta | Un solo campo skills, sin filtrar la entrada; conserva applyUrl y fecha de publicación. |
| `LoggingInterceptor.kt` | Versión de develop que limita los logs al código HTTP, con sus pruebas. |
| Home/Profile | Se conserva la presentación con guardas de cobertura, mercado ausente y radar solo cuando hay datos. |
| `SkillsGapScreen.kt`, `SkillsGapViewModel.kt`, test con add/add | Un flujo de mercado con selector de CV y entrada manual. Las cinco regresiones manuales quedan en `SkillsGapManualInputTest`; se conservan las siete de develop. |
| XML español/inglés | Unión de claves por idioma. Los textos por oferta quedan en `job_skills_gap.xml`; se eliminan `strings_job_gap.xml` y claves nuevas sin consumidores. Ambigüedad de catálogo y de nivel tienen textos distintos. |
| Gradle/CI | Se conserva el flag de mercado y el CI de develop para Debug/Release/pruebas/lint; también el diagnóstico de firma heredado de main. No se cambia la versión ni se distribuye desde la rama del PR. |

`DetailSheet.kt`, `JobSkillsGapSheet.kt`, su ViewModel/repositorio/mapper/DTO/modelos y
las pruebas de integración se adaptan explícitamente fuera de las marcas de conflicto.
Los fixtures del editor se actualizan a las fechas JSON de la rama; no se elimina su cobertura.

## Ejemplo de evaluación

```json
{
  "cv": {"id": "cv1", "skills": [
    {"name": "Python", "level": "intermediate"},
    {"name": "SQL", "level": "advanced"}
  ]},
  "jobs": [{"id": "job1", "skills": ["Python", "SQL", "Docker"],
    "description": "Python avanzado y SQL básico.",
    "requirements": ["Docker intermedio"]}]
}
```

Para recomendaciones, el mismo CV/oferta se proyecta solamente con `id` y `skills`.
El fixture del backend devuelve déficit 1 para Python, 0 para SQL y `cvLevel`/déficit null
para Docker. Son respuestas simuladas de contrato, no una ejecución contra Azure.

## Validación y alcance

Comando obligatorio:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console=plain
git diff --check
```

Se ejecutan también `assembleRelease` y `testReleaseUnitTest`, como exige el CI.
Las suites incluyen mercado, editor de CV, privacidad de logs, recomendaciones, niveles,
límites, reintentos, HTTP, solicitudes duplicadas y resultados tardíos.
Se añaden pruebas específicas de contratos separados de POST y combinación de cursos/niveles,
incluidas las dos direcciones de fallo (catálogo o evaluación).

Resultado local final: `assembleDebug assembleRelease testDebugUnitTest testReleaseUnitTest
lintDebug --console=plain` terminó con `BUILD SUCCESSFUL`. Pasaron 89 pruebas en Debug y 89 en
Release, sin fallos ni errores. Lint terminó con 0 errores y 103 advertencias del proyecto.
`git diff --check` pasó. La inspección XML encontró 0 recursos duplicados por qualifier;
hay una sola declaración Retrofit por cada POST y un único binding de análisis por oferta.
Hilt compiló en ambas variantes. No se publicaron los APK generados localmente.

Una ejecución intermedia detectó la diferencia entre el manejo genérico de 409 de develop
y el específico de main. Se resolvió según el código contractual `SKILLS_INPUT_REQUIRED`
para GET, y se adaptó el caso de error no contractual de la suite anterior. Los demás fallos
intermedios fueron incompatibilidades de tipos en fixtures/selector, resueltas antes de esta ejecución final.

No se ejecuta una sesión Android autenticada contra Azure/APIM. El backend está desplegado
según el contexto de la tarea; estas pruebas locales no certifican sus rutas ni políticas reales.
Para la comprobación real: configurar APIM/OAuth en el entorno local, registrar la firma Debug,
iniciar sesión, seleccionar CV/oferta y verificar cursos, niveles, cambio rápido de selección,
fallo del catálogo, Postularme y análisis de mercado. El flag `SKILLS_GAP_HTTP_020_ENABLED`
debe habilitarse explícitamente para la pantalla de mercado, como antes en develop.

No publicar APK, distribuir versiones ni fusionar el PR como parte de esta integración.
