# Auditoría de fuentes de tasa BCV — borrador de investigación

> **ESTADO: INVESTIGACIÓN NO CANÓNICA.** Recopilación solicitada antes de integrar o modificar decisiones/documentación oficiales. No es una decisión aprobada ni una especificación de implementación.

**Fecha de comprobación:** 2026-10-21, aproximadamente 19:27–19:28 UTC (15:27–15:28 Caracas), según la cabecera HTTP `Date` recibida. Las tasas y cabeceras son una observación puntual; pueden cambiar.

## Dictamen

La información del propietario contiene endpoints válidos y activos en esta comprobación. DolarApi y BCV Today devolvieron el mismo USD (`883.3963 Bs/USD`) con fecha efectiva 2026-10-21. Eso confirma que ambos endpoints respondían y coincidían en ese instante; **no** prueba disponibilidad sostenida, exactitud frente al BCV, baja latencia, ni actualización en tiempo real.

El diseño más útil para evaluar en M1 es consultar el endpoint específico de USD oficial de DolarApi y mostrar su dato como **propuesta de un espejo**, conservando confirmación humana y la entrada manual existente. BCV Today puede servir como contraste secundario. Ninguna de las dos fuentes es el BCV ni promete SLA/garantía de exactitud; no se debe presentar el espejo como fuente oficial directa ni activar una actualización automática sin confirmación bajo la regla D-034.

**Resultado de la auditoría:** parcial y favorable como insumo de diseño. Los endpoints fueron comprobados en vivo; las afirmaciones de estabilidad/rapidez, la frecuencia exacta de publicación y el cálculo ponderado no quedaron demostrados por una fuente primaria disponible.

## A. DolarApi

### Evidencia observada

- `GET https://ve.dolarapi.com/v1/dolares/oficial` respondió HTTP 200, `Content-Type: application/json`, `Cache-Control: public, max-age=0, must-revalidate`.
- JSON observado:

  ```json
  {
    "moneda": "USD",
    "fuente": "oficial",
    "nombre": "Dólar",
    "compra": null,
    "venta": null,
    "promedio": 883.3963,
    "fechaActualizacion": "2026-10-21T00:00:00-04:00"
  }
  ```

- `GET https://ve.dolarapi.com/v1/dolares` respondió HTTP 200 y una lista con objetos `oficial` y `paralelo`; el objeto oficial coincidió con el endpoint específico.
- `GET https://ve.dolarapi.com/v1/estado` respondió HTTP 200 con `estado: "Disponible"`. Es una comprobación puntual de estado, no un SLA.
- El OpenAPI público declara `/v1/dolares/oficial`, su esquema y a BCV como fuente de datos. El dominio/API sigue siendo un servicio de terceros. En el JSON, `fuente: "oficial"` clasifica la tasa; no identifica al proveedor como BCV.
- El ejemplo Python del propietario, que usa solo la biblioteca estándar, se ejecutó contra el endpoint y devolvió `883.3963`.

### Límites

- `fechaActualizacion` observada es medianoche de Caracas para la fecha indicada; por sí sola no permite determinar la hora de captura del dato. El endpoint no entregó en esta comprobación otro campo que permita medir cuándo su servidor obtuvo el dato del BCV.
- `max-age=0, must-revalidate` pide revalidación a las cachés; no demuestra cuándo se actualizó la fuente upstream.
- Una respuesta 200 y una ejecución del ejemplo no justifican “la más estable” ni “la más rápida”. No se hizo benchmark comparativo, serie de disponibilidad, ni prueba de carga; no se encontró un SLA en las especificaciones revisadas.

## B. BCV Today

### Evidencia observada

- `GET https://bcv.today/api/v1/rate.json` respondió HTTP 200 con USD `883.3963`, `effective_date: "2026-10-21"` y `updated_at: "2026-10-20T23:16:25.972171+00:00"`.
- `GET https://bcv.today/api/v1/status.json` respondió HTTP 200, `status: "ok"`, `source: "https://www.bcv.org.ve/"`, el mismo `updated_at` y `effective_date`.
- A la hora de consulta, `updated_at` equivalía a cerca de 20 horas antes. Es un dato válido para la fecha efectiva informada, pero no una lectura minuto a minuto.
- `https://cdn.jsdelivr.net/gh/grupoclip/bcv-api/api/v1/rate.json` también respondió HTTP 200 con la misma tasa. En esa respuesta concreta se observó `Age: 6588` y caché configurada hasta 7 días (`max-age=604800`, `s-maxage=43200`); por frescura, prefiero el endpoint directo `bcv.today` al CDN para una lectura primaria.
- El README del proyecto describe una API de JSON estático publicada por GitHub Pages, alimentada por scraping del portal del BCV. Su workflow de GitHub Actions programa ejecuciones cada 30 minutos. Por tanto, llamarlo “microservicio” no describe su implementación publicada.
- Ese mismo README la identifica como no oficial y entrega los datos “as-is”, sin garantías de disponibilidad o exactitud financiera.

### Límites

- La frecuencia programada cada 30 minutos no garantiza ejecución puntual; una corrida del workflow y la publicación en Pages pueden demorarse.
- `updated_at` sí aporta una hora de captura que ayuda a detectar antigüedad; `effective_date` expresa la fecha de vigencia. Son conceptos distintos y deben conservarse separados.
- DolarApi y BCV Today son implementaciones separadas, pero ambas dicen apoyarse en el sitio del BCV. Su coincidencia es corroboración entre espejos, no una garantía institucional independiente.

## C. pyDolarVenezuela

- PyPI sirve la versión `2.0.3`; el artefacto más reciente observado fue publicado el 2024-10-31. El paquete declara Python `>=3.9` y varias dependencias externas (`requests`, `curl-cffi`, BeautifulSoup, Babel, pytz, cachetools, SQLAlchemy, psycopg2-binary, entre otras). La función de ejemplo del propietario sí es de biblioteca estándar; el paquete no.
- Se inspeccionó el wheel de PyPI sin instalarlo. El proveedor BCV apunta a `http://www.bcv.org.ve/` y llama la consulta con `verify=False`; además desactiva las advertencias de urllib3. Esto deshabilita la verificación TLS y usa HTTP en su URL declarada, un riesgo de integridad ante intermediarios. **No recomiendo introducir este paquete en la app.**
- El extractor BCV inspeccionado realiza una petición y parsea HTML; el código revisado no muestra un fallback automático para esa extracción. No quedó acreditada la afirmación de “fallbacks” de la descripción compartida.
- La app objetivo está documentada como Android nativo Kotlin/Compose. Esta biblioteca Python no se integra directamente en ese stack; usarla requeriría añadir/runtime de Python y sus dependencias, sin beneficio demostrado frente a un cliente HTTP nativo.

## D. Afirmaciones sin verificación primaria suficiente

- **“La más estable y rápida”:** NOT VERIFIED. La prueba de hoy solo muestra que DolarApi y BCV Today respondieron una vez.
- **Publicación habitual alrededor de las 16:30 y validez el siguiente día hábil:** aparece en la documentación de BCV Today y concuerda con los campos de fecha observados, pero no se confirmó en una fuente primaria del BCV en esta auditoría.
- **Cálculo ponderado de operaciones de mesas de cambio:** NOT VERIFIED. La web del BCV no pudo inspeccionarse durante esta sesión (fallo 502 en el lector web y error de confianza de certificado al intentar la lectura directa). No atribuir este mecanismo al BCV sin una resolución o publicación oficial accesible.
- **“En tiempo real”:** no demostrado ni técnicamente apropiado para una tasa oficial diaria. La interpretación defendible es detectar y traer la nueva tasa oficial cuando el proveedor la publique; no una cotización que cambie minuto a minuto. Esta conclusión queda condicionada a la verificación oficial de la cadencia.

## Propuesta para evaluar al canonicalizar

1. Mantener D-034: ingreso manual siempre disponible; una fuente externa no se convierte en “BCV garantizado” solo porque devuelva `fuente: "oficial"`.
2. Evaluar el endpoint dedicado `https://ve.dolarapi.com/v1/dolares/oficial` como proveedor de prellenado propuesto (D-051), en lugar de descargar todas las tasas y seleccionar una fila.
3. Mostrar valor, proveedor, fecha/hora disponible y antigüedad; exigir el toque de confirmación establecido por D-051. No guardar la propuesta como tasa confirmada hasta esa acción.
4. Usar la respuesta directa de BCV Today como contraste diagnóstico si DolarApi falla o difiere; no cambiar silenciosamente de proveedor ni elegir entre tasas contradictorias.
5. En un cliente Kotlin, validar HTTP y JSON: `moneda == USD`, `fuente == oficial`, `promedio` numérico, finito y positivo; parsear el timestamp con su offset y conservar la fecha efectiva. Manejar `compra`/`venta` nulos. En error, dato ausente o tasa antigua según una política explícita, conservar el fallback manual/última tasa con aviso.
6. No describir el flujo como “tiempo real”. Medir disponibilidad/frescura por un periodo antes de calificar estabilidad o rapidez; el estado actual solo es una observación puntual.

## Fuentes revisadas

- DolarApi, endpoint específico: <https://ve.dolarapi.com/v1/dolares/oficial>
- DolarApi, todas las tasas: <https://ve.dolarapi.com/v1/dolares>
- DolarApi, OpenAPI: <https://ve.dolarapi.com/openapi.json>
- BCV Today, tasa: <https://bcv.today/api/v1/rate.json>
- BCV Today, estado/frescura: <https://bcv.today/api/v1/status.json>
- BCV Today, CDN: <https://cdn.jsdelivr.net/gh/grupoclip/bcv-api/api/v1/rate.json>
- Fuente del proyecto BCV Today y sus limitaciones: <https://github.com/grupoclip/bcv-api>
- Workflow de actualización programada: <https://github.com/grupoclip/bcv-api/blob/main/.github/workflows/update-rate.yml>
- pyDolarVenezuela en PyPI: <https://pypi.org/project/pyDolarVenezuela/>
- Portal primario BCV (no verificable en esta sesión): <https://www.bcv.org.ve/>

## Relación con la documentación del proyecto

Esta recopilación puede servir para proponer un ajuste puntual a `docs/TECH.md` y D-051: usar el endpoint específico oficial y registrar que el dato/hora se atribuye a un espejo. No cambia D-034, no convierte la automatización en requisito nuevo y no actualiza ni canoniza archivos del paquete de producto. La decisión de integrar estos hallazgos queda pendiente de revisión del propietario.
