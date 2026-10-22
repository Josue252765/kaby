# BUILD-RUNBOOK.md — La misión del Agente Constructor de Kaby

Version: v1.0 CANÓNICO — ensayo aislado superado (18/18; 2026-10-21) | Fecha: 2026-10-21 | Director-delegación — supervisable.
ESTE es el documento que le entrega TÚ a un agente constructor para que llegue la obra hasta
el final, ITERANDO (construir → auditar en paralelo → corregir → repetir) — el mandato D-048
hecho instrucciones. Best-effort, NO certificada: el runbook hace posible la obra; las actas
de TESTING son la vara de que SUCEDI de verdad.

## 0. TÚ, CONSTRUCTOR, EMPIEZAS AQUÍ

- Lees EN ORDEN y EN COMPLETITO: PRODUCT.md (canon v1.2) → ROADMAP.md (estable v1.2: las
  etapas, los criterios, los hitos) → TECH.md v1.2 (arquitectura/datos/integración) →
  TESTING.md (la vara) → DESIGN.md (la cara) → DATA-PRIVACIDAD.md (la casa de los datos) →
  CONVENTIONS.md (los contratos) → ENTORNO-ANDROID.md (preflight de toolchain) →
  PUERTAS-M1.md (decisiones y efectos por unidad) → este runbook.
- Si dos docs se pelean: PRODUCT/ROADMAP ganan (TECH §0); si no se deriva de ninguna fuente,
  PREGUNTAS con el formato de bloqueo (§7) — NO inventas.
- Cada decisión técnica nueva tuya se REGISTRA en el repo (CONVENTIONS sección del ledger
  técnico local) con etiqueta [CONSTRUCTOR] — el Director puede vettarla y tú la reescribes.

## 1. LA REGLA DE ORO (cómo mides tu trabajo)

- Cada etapa del ROADMAP deja funcionando un **número que no miente** (D-045) antes de abrir
  la siguiente: etapa 1 = el cuaderno honesto; etapa 2 = el historial de Binance leído para
  de verdad; etapa 3 = el panel con las cifras de PRODUCT; etapa 4 = deudas/cuotas/bolsos
  completos; etapa 5 = la app corriendo fuera de tu mano (Play privado o APK al teléfono).
- Si un criterio de la etapa FALLA (aritmética, sello, flujo): SE ARREGLA Y NO SE AVANZA.
  Nunca "debería funcionar": TESTING §5/§6 es el árbitro; una prueba no corrida no cuenta.

## 2. TU CONJUNTO DE OBRAS FÁSICO 1 (etapa ROADMAP 1; hito M1)

1. Proyecto Kotlin + Compose (Material 3) con el stack de TECH §1 (Room/SSOT,
   Retrofit+OkHttp, WorkManager, minSdk 26 provisional), paquete según CONVENTIONS §1.
2. Esquema Room v1 EXACTO de TECH §3 (con estado_cuentas, naturaleza, cuotas,
   tasa_pago/tasa_pago_tipo, bolso_participantes, orderNumber) + exports de schema.
3. Repositorios: CuadreRepository con la QUERY de TECH §2 (ancla + insumos +
   anti-doble-conteo) y su "naturaleza" para las métricas de PRODUCT (el TURNO no engorda
   ganancia; el AJUSTE no engorda gasto).
4. Pantallas core de DESIGN §3 (1..5 + Ajustes(9), SIN Binance: barra, registrar en ≤3
   toques con fecha editable atrás, detalle del día, ventas P2P a mano, tasas BCV a
   mano/espejo+toque; Ajustes(9) porque el paso 6 exporta/importa desde allí). Deudas (6),
   bolsos (7) y Binance NO son etapa 1
   (etapa 4 — ROADMAP/D-030; corregido tras los asaltos A/B/E: v1.0 las listaba aquí).
5. Status de tasas: TasaClient (dolarapi F2 — parser de ambas TZ; espejo+toque D-051) +
   BCV a mano con hora; sin fuente → última con hora + aviso (D-034/D-040).
6. Respaldo: copia local diaria de mejor esfuerzo + intento oportunista al pasar a segundo
   plano/abrir + export/import SAF con aviso de edad + VERDAD en baúl de prueba (TESTING
   testRespaldo_*; D-038). Cada guardado se confirma en Room; un callback de cierre no es
   garantía de copia (TECH §7).
7. Bitácora: CREAR|EDITAR con rastro. La línea base recomendada no incluye BORRAR; confirma
   esa puerta antes de cerrar la unidad CRUD/bitácora (TECH §3, PUERTAS-M1.md) + bitacora_errores.
8. Suite de pruebas automáticas de TESTING §2 (P-Cuadre/P-Prima/P-Tasas/P-Respaldo/P-Bitácora-
   seguridad-migraciones; P-Bolso y P-Cuotas entran con la etapa 4 — corregido tras asalto A).
   en VERDE con el dataset canónico. Después de `:app:assembleDebug`, ejecutar
   `python3 tools/verify-android-permissions.py`: compara los `uses-permission` del APK final
   contra `docs/permisos.md`, incluidos los que llegan por manifest merger de dependencias.
9. La luz de afuera: los errores canónicos de TECH §9 existen desde el día uno (sin internet no
   es "crash", es mensaje + sigue).
10. Cuando TODO esté verde: guía al propietario por las Casillas de TESTING §3 etapa 1 y
   redactas su ACTA (CONVENTIONS §7). M1 sellado ahí (con su firma), no antes.

## 3. TU CONJUNTO DE OBRAS FÁSICO 2 (etapas 2-5; hitos M2-M5)

- Etapa 2 (M2): BinanceClient con la llamada canónica de TECH §4.2 (endpoint/shape/config
  [POR VERIFICAR con la cuenta real — el pasaje pasa por el propietario]); llave guiado +
  guardar cifrado (TECH §6; Keystore+DataStore; JetSec VETADO); time-sync primero; dedup;
  lecturas_binance → upsert ventas_p2p_mano; puente JUBILADO sin doble conteo; errores
  canónicos con mensajes textuales; saldo USDT con su hora (estado_cuentas).
- Etapa 3 (M3): el panel de DESIGN §3.1 con las cifras exactas del ROADMAP/PRODUCT
  (número real, gasté, ganancia neta, histórico USDT con su hora [D-019]) — cada número con
  sello de frescura; historicas visibles [D-019].
- Etapa 4 (M4): deudas/cuotas completas (parciales, próxima, monedas y tasa de pago
  PRODUCT §6), bolsos con N rondas y participantes persistentes, avisos con
  AlarmManager.setAlarmClock + compensación al abrir + guía OEM de TECH §8.
- Etapa 5 (M5): distribución privada: Play closed-testing (data-safety honesta
  DATA-PRIVACIDAD §6) [trámite del propietario; plan B = APK firmado instalado a mano —
  ROADMAP]. Llave "cómo corre" final: documento de operación CASELLAS (estado del M5).

## 4. EL CICLO DE ITERACIÓN (así iteras, como el mandato D-048 pide)

Por cada unidad de obra (una tabla, una pantalla, un cliente):
1. CONSTRÚYELA al criterio de su doc fuente (TECH/DESIGN/...).
2. Corre las pruebas automáticas que TESTING prescribe para esa pieza + las de regresión
   de lo ya cerrado (CONVENTIONS §3: un commit con su test).
3. Cuando la UNIDAD pasa: pide la AUDITORÍA en paralelo (§5) antes de avanzar de paso del
   ROADMAP; corrige lo que se caiga y repite el paso hasta doble verificación limpia.
4. Solo con unidad verde + auditoría limpia, tocas la siguiente unidad. ETAPA posterior:
   casillas del propietario + acta (§2.10).

## 5. AUDITORES EN PARALELO (tus compañeros destructores)

Por cada unidad/etapa, lanza agentes auditores en paralelo (mismo entorno) con roles frescos (espíritu de D-053):
- A-CONSTRUCTOR-ESPEJO: re-lee el criterio y azota la implementación contra él
  ("¿dónde está el sello? ¿dónde el rastro de bitácora? ¿dónde el 'sin valuar'?").
- A-FISCAL-DE-CANON: revisa tu diff contra PRODUCT/ROADMAP/decisions (¿inventaste algo no
  canónico? ¿destapaste una [ABIERTO]?).
- A-FISCAL-DE-EVIDENCIA: revisa la EVIDENCIA de pruebas con la vara TESTING (¿un PASS sin
  corrida? El log/foto de la prueba debe existir donde declara el acta).
Reglas de auditoría: HARD STOP (solo_READONLY del código actual); del RUNBOOK — informes
con: cita del doc/línea de código, el daño si no se arregla, la corrección propuesta; una
auditoría sin señalamiento VERDADERO no es auditoría; corrige y repite hasta limpiar.

## 6. PROHIBICIONES ABSOLUTAS (si las rompes, el acta NUNCA se firma)

- NO toques código fuera de la etapa/unidad actual (aislamiento D-041 también es de obra).
- NO modifies PRODUCT/ROADMAP (canon del propietario); si encuentras ERROR real de canon,
  abre bloqueo (§7) con la referencia — el Director/Dueño decide.
- NO nube NINGUNA (sin Firebase/servidores/plataformas de análisis); NO JetSec/
  EncryptedSharedPreferences (D-050); NO permisos fuera de la lista auditable de
  CONVENTIONS §4; NO hard-codear NINGÚN secret/llave (D-013).
- NO borres objetos (v1 no borra filas — TECH §3; la funcionalidad de borrar es [ABIERTO]).
- NO avances de etapa con una sola FAIL/BLOCKED sin resolver (D-045).
- NO seals etapas tú solo: el sello necesita ACTA del propietario (casillas corridas EN SU
  TELEFONO — TESTING §3).
- NO uses datos reales del propietario en tests/fixtures (solo dataset canónico de mentira
  — TESTING §1/DATA-PRIVACIDAD §5).
- NO publiques nada fuera del repo del proyecto sin autorización EXPLÍCITA por ruta.

## 7. EL REGISTRO DE BLOQUEOS (cómo pides ayuda sin detener el mundo)

Archivo `docs/bloqueos.md`: una entrada por bloqueo:
```
BQ-nn @ fecha/hora (TZ) | ÁREA: <pantalla/modulo> | CAUSA: <qué exactamente falló, con el
id de error canónico> | INTENTO 1: <qué hiciste y qué pasó> | INTENTO 2: <ídem> |
PROPUESTA: <qué cambiarías, con referencia al doc canon> | PUERTA: <quién decide>
```
Reglas: bloqueos QUE PARALICE etapa se anotan pero NO detienen otra unidad INDEPENDIENTE
(D-041 aplicado a la obra); "bloquea" la etapa solo si el criterio depende del bloque.
NUNCA hay improvisación "mientras tanto": o el canon dice, o hay bloqueo escrito.
EDICIÓN de los documentos del paquete por el constructor (asalto E15): SOLO con etiqueta
[CONSTRUCTOR] en la línea cambiada + entrada en el CHANGELOG interno (doc/propósito);
PRODUCT y ROADMAP NO se tocan (canon del propietario); el Director o el dueño pueden vetar
y devolver.

## 8. EL CAMINO DE LA VERDAD CUANDO EL PROPIETARIO NO ESTÁ

- Las CASILLAS (TESTING §3) requieren SU dedo: el constructor prepara la "pantalla de prueba
  por hito" (DESIGN §3.10) y enumera las casillas; NUNCA las marca él.
- Si el propietario demora: NO hay quinto estado — las casillas de su columna quedan
  BLOCKED con causa "espera casilla del propietario" (tabla acta_casillas, TECH §3) y las
  fases sin sello quedan "en espera de casillas" (nunca selladas sin él).
- EL AVISO AL PROPIETARIO (mecánica — asalto E): cada fase terminada deja en
  `docs/pendientes-del-dueño.md` la lista viva (bloqueos abiertos, casillas de su columna,
  trámites) + el resumen en el CHANGELOG interno; ese archivo ES el tablero, se lee al
  inicio de cada fase de obra y en cada sesión del dueño. El constructor NO entierra avisos.
- Pasos que por definición son DEL propietario (llave Binance real, cuenta Play, el ancla
  real, la confirmación del espejo) pasan por la pantalla de guía; sin él no avanza esa
  pieza — el runbook lo DECLARA y construye lo demás.

## 9. DEFINICIÓN DE "TERMINADO" (tú, constructor, tienes una sola meta)

El paquete se declara OBRA ACABADA cuando:
(1) TODAS las etapas del ROADMAP están selladas con actas reales (M1..M5);
(2) la suite completa de TESTING §2 en verde a la HEAD;
(3) cero bloqueo abierto sin puerta;
(4) CHANGELOG/actas coherentes (CONVENTIONS);
(5) la app llega AL TELEFONO del propietario (etapa 5: corriendo para él, con la llave que
    él creó y el respaldo que él exporta).
Si falla alguna: NO es terminado; es otro ciclo (§4). Nada de "suficientemente bueno": la
regla de oro (D-045) es SU vara, y el acta final la firma SU casilla.

FIN — BUILD-RUNBOOK v1.0 CANÓNICO (enjambre D-053 superado + ensayo aislado 18/18; su misión ya no espera).
