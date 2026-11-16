# Idiomas en la proyección contextual de brechas

Base: `main` actualizado por fast-forward a `7932a12` (`origin/main`). Rama de trabajo: `feat/android-idiomas-brechas`. No se integró `develop`. Los worktrees existentes en `.worktrees/` se conservaron.

Referencia funcional: `cvexpert-web/apps/web/src/services/jobGapProjection.ts`, función `projectCvForJob()`, y `skillsGapService.ts` en `feat/web-brechas-por-oferta`.

## Función y contratos

`Cv.toJobGapCvProjection(requiredSkills)` en `data/mapper/JobGapCvProjection.kt` crea un `JobGapCvDto` nuevo. Incorpora las habilidades originales y únicamente los idiomas exigidos en `offer.skills`. No muta el CV, ejecuta HTTP o PUT, cambia modelos persistentes, extrae requisitos del texto ni calcula cobertura, evidencias o déficits.

Los dos mapeadores (`toJobSkillsGapRequest` y `toEvaluationRequest`) usan la misma función:

- Recomendaciones: `cv: {id, skills: [{name, level}]}`, `jobs: [{id, skills}]`.
- Evaluación: la misma proyección de CV y `jobs: [{id, skills, description, requirements}]`.

No se serializan `languages`, datos personales ni el resto del CV. Inglés avanzado en `languages` se proyecta como `English/advanced` si la oferta exige English, conservando Inglés en el CV original. No se inventa un nivel requerido cuando la oferta no lo especifica.

## Alias explícitos

| Español | Inglés |
| --- | --- |
| Inglés | English |
| Español | Spanish |
| Francés | French |
| Portugués | Portuguese |
| Alemán | German |
| Italiano | Italian |
| Ruso | Russian |
| Mandarín | Mandarin |
| Japonés | Japanese |
| Coreano | Korean |
| Árabe | Arabic |
| Lengua de señas costarricense | Costa Rican Sign Language |

Los nombres completos se comparan sin distinguir mayúsculas (`Locale.ROOT`), colapsando los espacios exactos de ECMAScript, incluidos NBSP y BOM. No se usa el `\s` predeterminado de JVM ni `isWhitespace` para esta identidad: sus conjuntos difieren de JavaScript. Tras NFD se eliminan solo los signos U+0300/U+0301/U+0302/U+0308 y se recompone NFC. Se preservan ñ, puntuación y otros signos; no hay coincidencias por subcadenas. Idiomas fuera de la tabla solo coinciden por el nombre normalizado. Java/JavaScript, C/C++ y English/English literature permanecen distintos.

## Niveles, precedencia y duplicados

| LanguageLevel | SkillLevel proyectado |
| --- | --- |
| basic | beginner |
| intermediate | intermediate |
| advanced | advanced |
| native | expert |

`native → expert` es una equivalencia operativa de cuatro posiciones para `cv-skill-v1`, no una afirmación universal de competencia experta certificada ni una equivalencia CEFR. Se explica en los detalles existentes del análisis mediante una cadena localizada en español e inglés. No se asignan valores por defecto a niveles inválidos: se usa `INVALID_CV_LEVEL`.

`skills` tiene precedencia sobre `languages`, incluso con alias distintos; la primera entrada equivalente prevalece, sin tomar el nivel máximo. Repeticiones técnicas con niveles contradictorios producen `INVALID_CV`. Los duplicados normalizados con el mismo nivel se envían una sola vez conservando la primera grafía. Las habilidades técnicas mantienen sus nombres y niveles.

Para los idiomas se usa la denominación exacta exigida por la oferta. Si esta enumera Inglés y English como requisitos distintos, se envían ambas denominaciones con el mismo nivel seleccionado por la precedencia anterior, como en Web. Repeticiones normalizadas de una misma denominación se envían solo una vez.

## Límites y alcance

Se conservan las validaciones previas de CV/oferta, el máximo de 200 requisitos, nombres e identificadores de 200 puntos de código Unicode y las restricciones de caracteres. Tras proyectar se vuelve a comprobar el máximo de 500 competencias y la validez de los nombres finales. Se rechaza el exceso sin truncar. El repositorio ya comprueba los 100 KiB del JSON UTF-8 después de construir cada DTO; esa comprobación se conserva para ambos endpoints.

No se encontraron contadores del flujo contextual basados en `cv.skills.size`: el resumen utiliza los conteos del backend. No se modifica ese resumen, el análisis histórico, el catálogo, la navegación, el editor o el guardado de habilidades.

## Pruebas y verificación

Se añadieron 22 pruebas JVM dedicadas en `JobGapLanguageProjectionTest.kt`: alias y sentido inverso, Unicode y espacios ECMAScript, locale turco, desconocidos/no exigidos, los cuatro niveles, niveles inválidos, precedencia, duplicados, dos denominaciones, conflictos técnicos, igualdad entre endpoints, JSON mínimo, ausencia de mutaciones, CV solo con idiomas/vacío, límite final de 500, límites de campos y comprobación de 100 KiB antes de invocar una API. Utilizan datos simulados y proxies en memoria, sin servidores ni sockets.

El fixture del test existente de tamaño de cuerpo se ajustó a 500 nombres únicos: las 500 repeticiones idénticas anteriores ahora se deduplican por diseño y ya no constituyen un cuerpo excesivo. No se ejecutó ese test ni el resto de la batería.

No se ejecutó Gradle ni ninguna prueba JVM en esta entrega. La instalación local tiene Gradle, JDK y SDK, pero carece de caché para Google Services 4.6.0 y Firebase Messaging 26.0.2 requeridos por el `main` actual. Se omitió la ejecución para no descargar dependencias ni componentes. Tampoco se compilaron APK ni se realizaron pruebas visuales o contra APIM.

Una vez que las dependencias ya estén disponibles, la ejecución puntual pendiente es:

```powershell
.\gradlew.bat :app:testDebugUnitTest --offline --no-daemon --max-workers=1 --tests com.cvpro.app.data.JobGapLanguageProjectionTest
```

Verificaciones estáticas realizadas: contraste de los 12 alias y las cuatro adaptaciones con Web, lectura del uso compartido en ambos endpoints, validación XML de los recursos localizados y `git diff --check`.

Quedan pendientes compilar/ejecutar esas pruebas en el entorno completo y validar manualmente Inglés avanzado solo en `languages` frente a English. La normalización cubre la política explícita de Web; diferencias futuras entre versiones Unicode de JVM y JavaScript requerirían ampliar los casos de prueba, sin introducir coincidencias aproximadas.
