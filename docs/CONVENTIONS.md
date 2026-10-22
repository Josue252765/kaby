# CONVENTIONS.md — Los contratos del repo de Kaby

Version: v1.1 CANÓNICO — ensayo aislado superado (18/18; 2026-10-21); asalto 1 integrado | Fecha: 2026-10-21 | Director-delegación — supervisable.
Contratos: fechas, nombres, mensajes, la vida del acta y de los hitos. Si TECH/TESTING/DESIGN
dictan el QUÉ/CUÁNDO/CÓMO-SE-JUZGA, este doc dicta el CÓMO-SE-ESCRIBE y el CÓMO-SE-RECUENTA.
Best-effort, NO certificada. Respaldo: ROADMAP §11 (método de regresión + registro de hitos
— aquí está el FORMATO exacto) y D-045 (el acta).

## 1. OBJETOS CON NOMBRE (el vocabulario del repo)

- App: **Kaby** (mayúscula inicial en textos; paquete `com.<dueño>.kaby` — el dueño fija el
  dominio real; [ABIERTO menor]).
- Tablas/entidades: EXACTAMENTE como TECH §3 (movimientos, deudas, cuotas, bolsos,
  bolso_participantes, bolso_participaciones, ventas_p2p_mano, tasas_bcv, lecturas_binance,
  estado_cuentas, bitacora, bitacora_errores, settings, acta_casillas) [CONSTRUCTOR: sincronizado
  con TECH §3 y el esquema Room v1]. Renombrados = migración versionada + nota en TECH (el
  canon manda).
- Estados/etiquetas del proceso: CONFIRMADO / POR VERIFICAR / ABIERTO / DERIVADO /
  DIRECTOR-DELEGACIÓN (significados: decisions-ledger.md — no se reinventa).
- Doc-struct conversión: PRODUCT (qué/por qué), ROADMAP (cuándo/orden), TECH (cómo
  técnicamente), TESTING (vara), DATA-PRIVACIDAD (la casa de los datos), DESIGN (la cara),
  CONVENTIONS (los contratos), BUILD-RUNBOOK (la obra).

## 2. NOMBRES DE ARCHIVOS/CODIGO

- Behavioral Kotlin: archivos en CamelCase por clase (`CuadreRepository.kt`); paquetes en
  minúsculas (`data`, `domain`, `ui`, `binance`, `tasas`, `backup`, `avisos` — los módulos
  de falla de TECH §2).
- Tests automáticos: `test<Regla>_<Caso>_<ResultadoEsperado` — EXACTAMENTE los nombres de
  TESTING §2 (testCuadre_datasetCanonico_saldoCoincide, ...) SI el auditor de D-053 los
  ajusta, TESTING es la vara y se actualiza EN EL MISMO paso (nunca divergen).
- Fixtures: `fixtures/canonico Dataset...json` (el dataset de TESTING §1) — versionado,
  con montos de mentira.
- Schema Room: exportado a `app/schemas/` en cada migración (TECH §3/D-039).

## 3. CONMITS Y VERSIÓN

- Mensaje de commit: `<área>: <verbo en presente> <qué>` (ej. `cuadre: corrige doble conteo
  del puente jubilado`). Áreas: ui/cuadre/datos/binance/tasas/respaldo/avisos/build/docs.
- Versión de la app: `1.0.<etapa*N>+<hitos>` — visto exacto en etapas del ROADMAP; el tag de
  git = hito (`M1-cuaderno`, `M2-binanza`...).
- CADA commit de feature arrastra su test automático si la regla tiene vara en TESTING.

## 4. EL ABI DE LOS PERMISOS (lista auditable, TECH §1/§8)

- Archivo en el repo `docs/permisos.md`: una línea POR permiso: nombre, por qué existe, qué
  pantalla lo pide, y cuál feature muere si se le quita. Cualquier permiso NO listado = bug
  a arreglar (regla del constructor, BUILD-RUNBOOK §6).

## 5. ERRORES CANÓNICOS (ids estables)

- Cada error canónico de TECH §9 tiene id ESTABLE; la app mapea el id al TEXTO del mensaje.
  Lista de etapa 1: `E-SIN-RED` (sin internet), `E-TASAS-SIN-FUENTE`, `E-SIN-VALUAR`
  (movimiento/venta sin tasa anterior confirmada), `E-RESPALDO-VIEJO`, `E-RELOJ-DESFASADO`.
  Etapa 2 añade: `E-LLAVE-MUERTA`, `E-BINANCE-TIMEOUT`, `E-BINANCE-429` [los códigos y
  mapeos reales de Binance: POR VERIFICAR — F1]. El DIAGNÓSTICO copiado muestra: id + hora +
  versión (sin datos personales — DATA-PRIVACIDAD §5; asalto A2: los ids viven AQUÍ, no en
  TECH §9).
- Mensajes en ESPAÑOL llano, tiempo relativo + hora absoluta, siempre con "qué Puedes hacer
  ya" (varas de la convención, no del gusto).

## 6. FORMATOS DE NÚMERO/DATA EN PANTALLA (para que el cuadre "no baile")

- Montos: separador de miles `.` y decimal `,` (es-VE); SIEMPRE dos decimales en Bs y la
  forma del USDT tabulada (2 decimales).
- Moneda explícita: `Bs 12.500,00` / `50,00 USDT` — JAMÁS un desnudo sin unidad (D-037).
- Fechas: pantalla → "lun 19 oct · 10:32 (Caracas)"; bits → siempre ISO-8601 con offset
  (D-040). El sello de frescura mostrado ES la hora de la FUENTE, no la hora del render.
- Tasa: `881,0664 Bs/USDT` (4 decimales — el estándar absoluto del BCV).

## 7. EL ACTA DE HITOS (formato exacto; el método del ROADMAP §11)

```
## [Mn] <título del hito> — sellado @ <fecha/hora TZ>
- Dispositivo: <modelo> · Android <vendedor versión> · Kaby <versión de la app>
- Etapas: <correspondencia con ROADMAP etapa N>
- Casillas de TESTING §3 de la etapa: <resultado PASO A PASO: casilla → PASS/FAIL/...>
- Pruebas automáticas corridas (TESTING §2): <lista + resultado; NOT_TESTED es explícito>
- Cifras contrastadas (día del sellado): saldo X Bs · gasté Y · ganancia neta Z · prima P
- Obstrucciones resueltas en camino: <breve, con id del error si lo hubo>
- Bloqueos pendientes: <lista con puerta POR VERIFICAR (¿bloqueado por trámite externo?)>
- FIRMA: propietario (casilla/toca de la app o nota) · constructor (commits)
```

- El acta se guarda en `docs/actas/hito-Mn.md` (un archivo por hito) y se menciona en el
  changelog. El SELLADO de una etapa ocurre SOLO con TODAS sus casillas en PASS real
  (D-045). Un solo FAIL/BLOCKED = NO sella (se arregla primero).
- Si una etapa cerrada se RE-ABRE (re-apertura del ROADMAP §11): acta NUEVA con "re-sellado
  @hora" vinculado a la vieja (nunca EDITAR el acta vieja; el rastro es ley).

## 8. EL CHANGELOG INTERNO (el diario del constructor)

- `CHANGELOG.md` en el repo: una entrada por sesión de trabajo: hora (TZ), qué se tocó,
  qué pruebas corrieron, qué se rompió y cómo se arregló, qué quedó pendiente [POR VERIFICAR].
  El changelog alimenta la auditoría de D-053 y las actas.

## 9. EL IDIOMA DE LOS DOCUMENTOS (reglas de canon ya instaladas)

- El canon (PRODUCT/ROADMAP) manda sobre los hijos (TECH/TESTING/...); un hijo que contradice
  = corrección del hijo (TECH §0).
- Toda afirmación técnica duros lleva FUENTE (investigación F#) o etiqueta [POR VERIFICAR] o la
  doble [DIRECTOR-DELEGACIÓN/DERIVADO]. Sin etiqueta = hallazgo del enjambre (D-053).
- El estado vigente se lee de la cabecera de cada documento, no se deduce de su número de
  versión. El flujo es borrador → revisión/ensayo best-effort → ratificación y publicación
  autorizada; el estado final se registra como CANÓNICO o ESTABLE según la decisión vigente.
  Después de ratificarlo, no se conserva una etiqueta antigua como BORRADOR AUDITADO.

## 10. TRABAJOS DEL AGENTE CONSTRUCTOR (lo mínimo — la misión vive en BUILD-RUNBOOK)

- Cada bloque en 2 intentos sin resolver → registro de bloqueo (BUILD-RUNBOOK §7) — NUNCA
  hay una imprenta de fantasía: bloqueo en su formato: causa + propuesta.
- Prohibiciones rápidas (repite BUILD-RUNBOOK §6): sin nube, sin JetSec, sin llave dura,
  sin tocar el proyecto fuera de la etapa actual, sin publicar sin permiso por ruta.
- Etiqueta [CONSTRUCTOR]: la usa el constructor cuando EDITA un doc hijo del paquete
  (TECH/TESTING/DESIGN/CONVENTIONS/BUILD-RUNBOOK/DATA-PRIVACIDAD) — línea cambiada marcada +
  entrada en el CHANGELOG interno; PRODUCT/ROADMAP no se tocan (canon); vetable por el
  Director o el dueño. [definida tras el asalto B/E]

## 11. EL CIERRE DEL PAQUETE (el informe final del Director; D-058)

Al terminar, el Director entrega un INFORME con: (1) estado POR DOCUMENTO (versión, estado,
pendientes); (2) la lista de ABIERTOS con su puerta y su momento; (3) el veredicto de la
auditoría (asaltos del enjambre — siempre "best-effort, NO certificada"); (4) las cifras del
oráculo canónico (TESTING §1.5) y su cadena de evidencia; (5) lo que require su visto o trámite.
El propietario revisa y PUEDE VETAR por documento (no hace falta un "sí" global: el veto se
aplica punto por punto y el Director corrige). El cierre del SISTEMA exige: cobertura
justificada + cero decisión bloqueante sin resolver + auditoría registrada (D-053 cumplida).

## 12. PUERTAS ABIERTAS (la cola con su momento — asalto E6/E10)

| Puerta | Qué se decide | Momento de resolución |
|---|---|---|
| Borrar datos de una fila | Confirmar si la línea base M1 excluye BORRAR; recomendación técnica actual: solo CREAR/EDITAR con rastro | Antes de cerrar la unidad CRUD/bitácora de M1 |
| PIN/biometría optativa | ¿ blindar la pantalla de datos? | Antes de M3 (cuando hay panel que proteger) |
| Cifrado del respaldo exportado | ¿plano o cifrado? (DATA §3) | Antes de implementar exportación/importación de M1 |
| minSdk | Confirmar API del teléfono y compatibilidad de dependencias; 26 es provisional (TECH §1) | Antes de instalar en el teléfono del propietario |
| Umbral del aviso "sin respaldo" | constante (hoy 7 días) | Al soltar v1 |
| Cuándo leer Binance en el día | apertura + manual (propuesta en TECH §4) | Antes de M2 |
| Criterio de "terminado" por función | PRODUCT §9 ABIERTO → lista y pesos para aprobación del dueño | Antes de sellar M1; no bloquea unidades de código independientes |
| Presentación fina de la ganancia neta | PRODUCT §12 ABIERTO (la FÓRMULA ya está: D-055) | Antes de M3 (el panel) |
| Lista COMPLETA de tipos/categorías | PRODUCT §6/§12 deja abierta la lista. M1 admite texto libre; no inventar sugerencias ni catálogo | Antes de implementar sugerencias/catálogo; no bloquea el registro libre |

## 13. LEDGER TÉCNICO LOCAL [CONSTRUCTOR]

Las versiones que siguen fijan la primera compilación de Kaby y completan D-060. Son una
resolución técnica del constructor, no una nueva decisión de producto; quedan visibles para
veto antes del sello M1. No actualizarlas sin volver a correr el paquete completo.

| Pieza | Versión fijada | Fuente en repo |
|---|---:|---|
| Gradle Wrapper | 9.8.2 | `gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.4.1 | `gradle/libs.versions.toml` |
| Kotlin integrado / Compose compiler | 2.2.10 | D-060 + `gradle/libs.versions.toml` |
| KSP | 2.3.12 | `gradle/libs.versions.toml` |
| Compose BOM | 2026.09.00 | `gradle/libs.versions.toml` |
| Room | 2.8.5 | `gradle/libs.versions.toml` |
| Lifecycle | 2.11.0 | `gradle/libs.versions.toml` |
| Activity Compose | 1.13.0 | `gradle/libs.versions.toml` |
| WorkManager | 2.12.0 | `gradle/libs.versions.toml` |
| Retrofit | 3.0.0 | `gradle/libs.versions.toml` |
| OkHttp | 5.5.0 | `gradle/libs.versions.toml` |
| Gson | 2.14.0 | `gradle/libs.versions.toml` |
| JUnit | 4.13.2 | `gradle/libs.versions.toml` |
| Robolectric (solo pruebas) | 4.17; Room corre con API 35 | `gradle/libs.versions.toml`; [compatibilidad oficial](https://robolectric.org/compatibility_table/) |
| JDK / Java target | 17 | `docs/ENTORNO-ANDROID.md`, `app/build.gradle.kts` |
| compileSdk / targetSdk / build tools | 37 / 37 / 37.0.0 | `app/build.gradle.kts` |
| minSdk | 26 provisional | TECH §1; confirmar con el teléfono antes de instalar |

El cliente JSON usa Gson; no se incorpora una librería de almacenamiento cifrado. El espejo
BCV permanece detrás de `TasaClient` y su respuesta no se guarda hasta confirmación humana.
La compilación local inicial se corrió el 2026-10-21; la evidencia de cada corrida queda en
`CHANGELOG.md`.

### Decisiones de implementación M1 [CONSTRUCTOR]

| Decisión | Regla aplicada | Implementación |
|---|---|---|
| Rotación de `bitacora_errores` | Conservar las 500 entradas más recientes por `id`, después de cada alta y en la misma transacción. La rotación solo afecta esta bitácora; no borra movimientos, ventas ni el rastro contable. | `CuadreRepository.ERROR_LOG_LIMIT`; `KabyDao.pruneErrorEntries` |

| Identificador real del paquete | `com.<dueño>.kaby`; el valor provisional de ENTORNO-ANDROID se usa para desarrollo hasta confirmación | Antes de instalar con datos del propietario; no cambiarlo después sin plan de migración/exportación |
| Keystore de firma (D-056) | el constructor genera; el dueño custodia | En la etapa 5 |

FIN — CONVENTIONS v1.1 CANÓNICO (asalto 2 + verificación de cierre 4/4 + ensayo aislado 18/18 superados; 2026-10-21).
