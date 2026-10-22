# TESTING.md — La vara de Kaby (pruebas automáticas y casillas, por etapa)

**Versión:** v1.1 CANÓNICA — re-escrita tras el ASALTO 1. Este documento define los contratos
de aceptación por etapa; no afirma que cada prueba nombrada aquí ya esté implementada.
**Fecha:** 2026-10-21 · **Estado:** CANÓNICO — hay pruebas unitarias Android ejecutables; su
resultado y alcance observado se registran en `CHANGELOG.md`. Esto no sella M1 ni sustituye las
casillas del propietario en un teléfono real.
Regla fija (D-045): una prueba que no se corrió JAMÁS es PASS.

## 0. CÓMO LEER ESTE DOCUMENTO

- Cada etapa del ROADMAP se sella solo cuando TODAS sus pruebas/casillas tienen veredicto real
  aquí, registradas en el ACTA (§6) y aceptadas por el propietario.
- Clasificación (PARTIAL consolidada como decisión D-054 — ver ledger):
  - **PASS** — se corrió y salió bien; con evidencia (log, captura o acta).
  - **FAIL** — se corrió y salió mal. El sello NO se pone; primero se arregla (D-045).
  - **NOT_TESTED** — no se corrió. JAMÁS se reporta como PASS ni "debería pasar".
  - **BLOCKED** — no se puede correr hoy; requiere causa escrita con su puerta (trámite del
    propietario, llave, cuenta real, telefonía, o "espera casilla del propietario").
  - **PARTIAL** — se corrió a medias; se cierra en PARTIAL con lo que faltó anotado. JAMÁS
    se convierte en PASS por estar "casi completa".
- Dos clases de prueba:
  - **AUTOMÁTICA** — corre en el repo (JVM, Robolectric u otra tarea indicada por el test), no
    exige al propietario. La evidencia es el log del test; no se presupone que arrancó un emulador.
  - **CASILLA** — el propietario la corre en SU teléfono (o con su trámite) y la marca. Son las
    varas de la vida real: batería, OEM, dedos, luz del sol, red venezolana.
- Los números del dataset SON DE MENTIRA (cifras inventadas coherentes); lo que vale es la
  ARITMÉTICA entre cifras, reproducible por cualquiera. Si el código produce estos números, la
  aritmética es fiel; si no, FAIL.

## 1. DATASET CANÓNICO (oráculo numérico — añadido tras el asalto A)

Referencia histórica: `simulacion-caso-de-uso.md` está listada en la carpeta de estado externa
de HANDOFF-KABY §16; no está incluida en este repositorio y su contenido no se verificó en esta
auditoría. Las tablas de este apartado son la fuente reproducible para los tests; las cifras
viven en el código de prueba (fixture), JAMÁS hardcodeadas en la app. Horas en TZ Caracas
(−04:00). BCV del mes: 880,0000 Bs/USD.

### 1.1 Ancla y tasas
- `estado_cuentas.ancla_monto` = 12.500,00 Bs @ 2026-09-01 08:00
- t1: 880.0000 Bs/USD @ 2026-09-01 08:05 (CONFIRMADA por humano; fuente MANUAL)
- t2: 880.0000 Bs/USD @ 2026-09-20 09:00 (mismo valor, OTRA hora — sirve para probar "con su hora")

### 1.2 Movimientos (tabla `movimientos`; naturaleza NORMAL salvo m6)

| id | tipo | naturaleza | moneda | monto | fecha-hora | nota |
|----|------|-----------|--------|-------|-----------|------|
| m1 | GASTO | NORMAL | Bs | 1.000,00 | 2026-09-03 12:30 | comida (BCV 880) |
| m2 | GASTO | NORMAL | Bs | 350,00 | 2026-09-03 18:10 | transporte |
| m3 | INGRESO | NORMAL | Bs | 5.000,00 | 2026-09-05 10:00 | tercero: "papá — remesa septiembre" (proveniencia_tercero OBLIGATORIA) |
| m4 | INGRESO | NORMAL | Bs | 200,00 | 2026-09-12 15:00 | tercero: "María — pasaje" |
| m5 | GASTO | NORMAL | Bs | 120,00 | 2026-09-15 09:30 | hormiga |
| m6 | INGRESO | TURNO_BOLSO | Bs | 400,00 | 2026-09-16 11:00 | mi ronda la pagó María — "plata que vuelve que paga por ti": mueve el SALDO, no engorda métricas (regla signo/naturaleza del asalto A2) |
| m7 | GASTO | NORMAL | Bs | 118,50 → 120,00 | 2026-09-10 13:00 (EDITADO el 2026-09-21 09:00) | corrige el monto +1,50 → descubre diferencia y DESCUADRE_ABIERTO (regla TECH §3) |

### 1.3 Ventas P2P a mano (tabla `ventas_p2p_mano`; fuente=MANO, estado=VIVA — D-046)

| id | USDT | tasa P2P | bs_netos | fecha-hora | prima esperada (bs_netos − USDT×880) |
|----|------|----------|----------|-----------|--------------------------------------|
| v1 | 50,00 | 1.000,00 | 50.000,00 | 2026-09-07 11:00 | +6.000,00 |
| v2 | 25,00 | 998,50 | 24.962,50 | 2026-09-14 17:20 | +2.962,50 |
| v3 | 10,00 | 1.005,00 | 10.050,00 | 2026-09-20 10:00 | +1.250,00 |

Prima total del mes: **+10.212,50 Bs** (métrica visible; NO agrega al saldo — el saldo ya entró
por bs_netos).

### 1.4 Deudas y bolso (sembrados PARA LA ETAPA 4; NO entran en el oráculo del cuadre de etapa 1)
- d1: deuda COMERCIO "ferretería": 4.800,00 Bs en 4 cuotas de 1.200,00, MENSUAL, vto día 5.
  - p1 = 500,00 Bs @ 2026-09-03 (PARCIAL)
  - p2 = 700,00 Bs @ 2026-09-04 (cierra cuota #1 → PAGADA; deuda "al día" hasta el 5/oct)
  - p3 = 1.200,00 Bs @ 2026-10-06 (cierra cuota #2)
  - p4 = 10,00 USDT @ 2026-10-20 (pago en OTRA moneda; tasa_pago = 120,00 Bs/USDT, tipo OTRA,
    con su hora — cierra la cuota #3; JAMÁS re-valúa la deuda — D-044)
- b1: bolso "refrigerios del taller": 6 rondas, cuota FIJA 400,00 Bs, participantes
  Ana/María/José/yo; mi ronda 3 la pagó María (= m6 en §1.2).

### 1.5 TOTALES ESPERADOS (el ORÁCULO del cuadre de etapa 1 — escritos a mano por el Director)
- ingresos = 5.000,00 (m3) + 200,00 (m4) + 50.000,00 (v1) + 24.962,50 (v2) + 10.050,00 (v3)
  = **90.212,50 Bs** (definición operativa: decisión D-055)
- gasté = 1.000,00 (m1) + 350,00 (m2) + 120,00 (m5) + 120,00 (m7 ya corregido) = **1.590,00 Bs**
- turno = +400,00 (m6): mueve el SALDO; NO entra en ingresos ni en gasté
- saldo Mercantil @ 2026-09-21 (con la fila m7 ya editada) = 12.500,00 + 90.212,50 − 1.590,00
  + 400,00 = **101.522,50 Bs**
- ganancia neta @ la misma fecha = 90.212,50 − 1.590,00 = **88.622,50 Bs** (su nota de
  definición es visible al tap — PRODUCT §5)
- edición de m7 (118,50 → 120,00): el saldo baja 1,50 adicional y abre DESCUADRE_ABIERTO con
  diferencia 1,50 Bs (TECH §3)

INVARIANTES que los tests deben ROMPER para demostrar que la app pide caución: prima sin
tasa CONFIRMADA con hora ≤ la hora del movimiento → "sin valuar" (JAMÁS 0) · JUBILADA no
cuenta doble · el AJUSTE_CUADRE no engorda "gasté" (ni una vez; asalto A2: sí mueve el
saldo) · sin ancla no se inventa saldo.

## 2. PRUEBAS AUTOMÁTICAS (suite del repo)

### P-Gastos (PRODUCT §5)
- `testGastadoHoy_valuaSoloLaFechaLocalYExponeLosSinValuar` — agrupa por fecha Caracas,
  conserva el gasto en Bs, muestra su equivalencia con la tasa guardada y marca la fila sin
  tasa como parcial; AJUSTE_CUADRE no engorda "gasté".
- `testGastosAcumulados_usaTasaDeCadaRegistroYMarcaEquivalenciaParcial` — el total histórico
  suma Bs y USDT usando la tasa guardada en cada fila, excluye ingresos y ajustes, y deja
  identificada la equivalencia USD incompleta cuando falta tasa.

### P-Formato, fecha y rastro visible (DESIGN §3)
- `testEquivalenteBcv_usaUSDYLaVentaConservaUSDT` — etiqueta la equivalencia BCV como USD y
  conserva USDT como unidad de una venta.
- `testEquivalenciaDelMovimiento_usaLaTasaGuardadaYNoInventaUna` — usa la tasa guardada y devuelve
  ausencia de equivalencia si no hay tasa; no la sustituye por cero.
- `testListaAgrupaMovimientosPorFechaDeCaracas` — una hora UTC que cae la noche anterior en
  Caracas se agrupa y presenta con el día local correcto.
- `testRastro_marcaSoloUnaEdicionPosteriorAlRegistro` — detecta como edición solo un instante
  posterior al de creación; igualdad de instante y hora inválida no muestran edición.

### P-Manifiesto e identidad visual (DESIGN)
- `testManifest_iconoUsaLaPaletaKaby` — el manifest referencia el icono y el vector usa los
  colores tinta y piedra de la identidad vigente de Kaby.
- `python3 tools/verify-android-permissions.py` — post-build: compara los permisos efectivos del
  APK debug con `docs/permisos.md`, incluidos los incorporados por dependencias AndroidX. No es una
  prueba JVM ni sustituye el cotejo de cada variante.

### P-Cuadre (D-037/D-042; query de TECH §2)
- `testCuadre_datasetCanonico_saldoCoincide` — la query del cuadre reproduce el oráculo §1.5
  dígito a dígito: 101.522,50 @ 2026-09-21.
- `testCuadre_ventaP2P_casaUnicaAntiDoble` — el saldo suma bs_netos SOLO de ventas VIVA de
  `ventas_p2p_mano`; jubilarlas y meter las mismas ventas por API deja el saldo inalterado.
- `testCuadre_turno_saldoSi_metricasNo` — m6 (+400) mueve el saldo y NO engorda ingresos ni gasté.
- `testCuadre_editarAntesDeCuadre_reabre` — editar CUALQUIER fila contable con fecha ≤
  `ultimo_cuadre_at` (gasto o venta) reabre DESCUADRE_ABIERTO con la diferencia (aquí: 1,50 Bs).
- `testCuadre_ajusteCuadre_noEngordaDoble` — el ajuste tras cuadre queda con naturaleza
  AJUSTE_CUADRE y el test verifica que "gasté" no lo engorda (ni una vez; sí mueve el saldo
  — asalto A2).
- `testCuadre_otraPlata_provenienciaObligatoria` — m3/m4 entran SIEMPRE al cuadre (D-042) y la
  proveniencia de tercero es OBLIGATORIA (CHECK del esquema).
- `testCuadre_sinAncla_estadoSIN_ANCLA` — sin ancla no se inventa saldo: estado SIN_ANCLA y la
  pantalla pide el ancla.
- `testCuadre_anclaPuedeSerCero` — cero es un ancla explícita válida y produce saldo calculado
  cero, distinto de la ausencia de ancla.
- `testCuadre_movimientoAnteriorAlAnclaNoCambiaSaldoInicial` — un movimiento anterior al instante
  del ancla no altera el saldo inicial.
- `testGananciaNeta_datasetCanonico_definicion` — ingresos (D-055) − gasté = 88.622,50 @
  2026-09-21; la nota de definición acompaña al número.

### P-Prima (D-046; TECH §3)
- `testPrima_formulaConSigno` — prima = bs_netos − usdt_cantidad × tasa_bcv_al_momento;
  dataset: +6.000,00 / +2.962,50 / +1.250,00.
- `testPrima_casoNegativo_conSigno` — síntesis: 7,50 USDT cobrados en 6.450,00 Bs @ 880 →
  6.450,00 − 6.600,00 = −150,00 (negativa visible; no se esconde).
- `testPrima_sinBcvDelDia_sinValuar` — si no hay tasa CONFIRMADA con hora_fuente ≤ la hora del
  movimiento → "sin valuar" (columna de tasa NULL + bandera; JAMÁS 0). REGLA ÚNICA del
  desempate (TECH §3, unificada con asalto A2): la ÚLTIMA confirmada ANTES de la hora del
  movimiento; si no hay ninguna antes, sin valuar (sin excepciones; el "día" solo propone).
- `testPrima_editarFila_recalculaConRastro` — editar bs_netos de v1 recalcula la prima y escribe
  EDITAR en la bitácora; "congelada al guardar" aplica SOLO a cambios externos de tasa (no a
  ediciones del propio usuario).
- `testPrima_redondeoCentavos` — dos decimales; el total del día suma primas ya redondeadas por
  fila (constante con nombre; [DIRECTOR-DELEGACIÓN]).

### P-Bolso y cuotas (D-036/D-043; etapa 4)
- `testBolso_rondaAvanza` — pagar la ronda n marca PENDIENTE→PAGADA y avanza `ronda_actual`;
  el giro del bolso es TURNO_BOLSO (plata que vuelve, nunca métrica).
- `testCuota_pagosParciales_estados` — d1 con p1+p2 → cuota #1 PAGADA y deuda AL_DIA hasta el
  vto; solo p1 → PARCIAL; vto pasado sin cubrir → ATRASADA; el pago parcial conserva su fecha
  hasta reunir la cuota.
- `testDeuda_pagoOtraMoneda_tasaDelPago` — p4 guarda tasa_pago + tasa_pago_tipo (BCV|P2P|OTRA)
  + hora; el test verifica además que JAMÁS re-valúa el resto de la deuda (D-044).

### P-Tasas (D-034/D-040/D-044/D-051)
- `testTasas_parseoAmbasTZ` — el parser normaliza −04:00 (oficial) y Z (paralelo), ambos
  formatos demostrados del espejo (investigación F2).
- `testTasas_sinRed_ultimaConHoraAviso` — sin red se usa la ÚLTIMA con su hora y aviso "sin
  fuente vigente desde [hora]" (nunca inventa frescura).
- `testTasas_prellenado_espejoNoAutomatico` — el valor del espejo es PROPUESTA con etiqueta
  "espejo NO oficial"; Room permanece sin tasa guardada hasta `confirmProposal` y solo entonces
  persiste una fila confirmada por humano (D-051).
- `testBsEnMano_revaluaConBcvNueva` — entra t2 (mismo valor, otra hora) → los Bs en mano se
  re-valúan con su nueva hora; los gastos viejos NO (congelados a su momento — D-044).
- `testBsEnMano_montoCongeladoNoRevalua` — cambiar el monto en mano NO re-valúa los gastos ya
  valúados (cierre inverso de D-044).
- `testSinValuar_sePuedeValuarTrasUnaTasaConfirmada` — una fila antes sin tasa puede recibir la
  tasa efectiva cuando se edita, incluso si conserva el mismo instante de registro.
- `testTasa_usaUltimaConfirmadaAnteriorAlMovimiento` — selecciona la última tasa humana anterior
  al instante del movimiento e ignora tasas posteriores o no confirmadas.
- `testTasa_mismoInstanteDesempataConInsercionMasReciente` — para dos confirmaciones en el mismo
  instante, prevalece el mayor id de inserción.
- `testTasas_usaSoloLaConfirmadaMasRecientePorInstante` — el snapshot elige la confirmación más
  reciente por instante e ignora la propuesta sin confirmar.
- `testTasas_mirrorSinConfirmarNoEsTasaVigente` — una tasa del espejo no humana no se expone como
  tasa vigente.
- `testTasas_rechazaParaleloComoBCVOficial` — el parser no acepta una fila paralela como BCV.
- `testTasas_sinRed_sinTasaAnteriorNoInventaUna` — sin conexión y sin tasa confirmada anterior, el
  mensaje informa que no existe una tasa previa.

### P-Binance (etapa 2; D-013/D-052)
- `testC2C_dedupPorOrderNumber` — releer la misma ventana no duplica filas.
- `testC2C_ventanaMensualEncadena` — 3 llamadas de ≤30 días reconstruyen 3 meses sin hueco ni
  solape (D-052).
- `testC2C_erroresSinteticos_mensajes` — MockWebServer con 401/timeout/429 sintéticos → los
  mensajes quirúrgicos de TECH §9 y la app sigue navegable. Honradez: esto prueba la REACCIÓN
  de Kaby, no la semántica real que Binance emita; los mapeos 401→llave-muerta y Retry-After
  quedan [POR VERIFICAR] con la cuenta real.
- `testC2C_relojDesfasado_mensajeCanonico` — reloj del teléfono adelante 30 min → el error
  canónico de reloj (TECH §9), nunca "bug interno". (El cotejo exacto con el endpoint de hora
  del servidor: [POR VERIFICAR], sin atribución falsa a F1.)
- `testC2C_puenteJubilaSinDobleConteo` — la primera sincronía marca el puente JUBILADA y el
  saldo no cambia (baja limpia del puente — D-046).
- `testC2C_parserMapeoCampos` — con fixture capturado (montos de mentira): unitPrice→tasa_p2p,
  totalPrice→bs_netos, amount→usdt, createTime→fecha-hora (con TZ).

**Cobertura añadida 2026-10-21 (fixtures sintéticos, no cuenta real):** `BinanceClientTest`
comprueba vector HMAC, ventanas, capacidades de lectura exclusiva, uso de reloj servidor,
paginación, filtro completada SELL/USDT/VES, órdenes repetidas, total cambiante, respuestas
inválidas, reloj desfasado, fallo de red y límite 429. `BinanceRepositoryTest` comprueba transacción
atómica/deduplicación, no efecto sobre el saldo Mercantil al importar, conservación de la ancla y
hora de lectura al crearla después de Binance, conciliación individual, puente coincidente,
registro de auditoría y bloqueo de edición manual de venta API. `SettingsInteractionTest` recorre la
semántica Compose en Robolectric: navegación a Ajustes/Inicio, abrir/cancelar diálogo, consentimiento,
botón de respaldo y controles de BCV/M1/movimiento. Estos casos no sustituyen HTTP real, dibujo GPU,
interacción Appetize verificable ni comparación contra la cuenta del propietario. La conciliación
propia de ventas API sigue siendo una limitación de producto, aunque la fila se conserva sin
duplicarla.

### P-Respaldo y restauración (D-038; TECH §7)
- `testRespaldo_jsonLocalVersionadoSeDecodifica` — codifica y decodifica un snapshot v1 con un
  movimiento de prueba y conserva monto y categoría; no simula exportación SAF.
- `testRespaldo_rechazaVersionDeFormatoDesconocida` — devuelve rechazo para JSON inválido y para
  schema_version desconocido.
- `testRespaldo_intentoDiarioAlAbrirYAlPasarSegundoPlano` — comprobación estática del cableado de
  la programación al abrir y al pasar a segundo plano, y de la política KEEP; no mide ejecución
  temporal de WorkManager en un dispositivo.
- `testRespaldo_escriturasConcurrentesSeSerializan` — solicitudes concurrentes serializan la
  generación del snapshot y su reemplazo; verifica el coordinador en JVM, no la temporización de
  WorkManager ni fallos físicos del almacenamiento.
- `testRespaldo_snapshotInvalidoNoSeEscribe` — rechaza un snapshot inválido sin invocar la escritura.
- `testRespaldo_jsonImportaComparar` — exportar el dataset §1 → bytes del JSON (formato
  especificado en DATA-PRIVACIDAD §3) → importar a una DB nueva → número contra número. La
  parte "en segundo espacio del teléfono real" es CASILLA (§3, etapa 1).
- `testRespaldo_importarViejoAvisa` — el archivo más viejo que los datos del teléfono pide
  confirmación explícita antes de escribir.
- `testRespaldo_noContieneLlave` — recorre las claves JSON de un snapshot poblado y rechaza nombres
  de campos de credencial (apiKey, secret, credential o ciphertext). M1 no contiene una llave
  Binance real que pueda usarse como fixture; el test no demuestra exclusión de una llave real.
- `testRespaldo_baulNoTocaVivos` — la verificación corre en el baúl de prueba (destruido al
  final); los datos vivos quedan bit a bit idénticos (hash antes/después).
- `testRespaldo_reglasAndroid` — comprobación estática: verificar `allowBackup`,
  `fullBackupContent` (Android 11 y anteriores) y `dataExtractionRules` con exclusión separada
  de nube y D2D (Android 12 y posteriores). Esto no prueba el comportamiento de cada OEM;
  además se debe ejecutar una prueba de respaldo en nube y D2D en el teléfono del propietario
  antes de guardar la llave Binance o liberar la app. Android documenta que algunos fabricantes
  aún permiten D2D con `allowBackup=false`:
  [reglas de Android 12](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore).

### P-Bitácora, seguridad y migraciones (D-037/D-041; D-057)
- `testBitacora_editarDejaRastro` — prueba con Room en memoria que EDITAR persiste antes/después,
  sello de hora y conserva la hora de creación del movimiento. Corre con Robolectric en API 35;
  no prueba el comportamiento en un dispositivo Android.
- `testBitacora_fallaAlGuardarRastroRevierteEdicion` — un trigger de SQLite rechaza el INSERT del
  rastro; verifica que la transacción revierta la edición del movimiento y no deje un rastro EDITAR
  parcial. Es una prueba Room/Robolectric, no una prueba en dispositivo.
- `testRastro_edicionMuestraAntesDespuesEnPalabras` — prueba el texto presentado para una fila
  de rastro editada, sin JSON crudo. Es una prueba del formateador; no ejecuta persistencia Room.
- `testRastro_creacionDescribeVentaSinJson` — muestra el alta de una venta con descripción
  legible, sin JSON crudo. Es una prueba del formateador; la persistencia transaccional se cubre
  por separado con la prueba de persistencia de edición Room.
- `testErrores_copiableSinDatos` — el texto copiado NO lleva montos, llave, ni datos de terceros.
- `testLogs_sinSecretNiLlave` — intenta registrar llave y SECRET sintéticos como si fueran un
  código de tasa; el repositorio rechaza el texto antes de persistirlo y la bitácora Room solo
  admite los códigos permitidos o mensajes genéricos fijos. Cubre la bitácora persistida; no lee
  logcat. M1 no tiene llave Binance real ni interceptor de logging.
- `testMigracion_v1aV2_sinPerdida` — al incorporar una migración v1→v2, sembrar el esquema v1 con el
  dataset y cambiar de versión: TODAS las cifras del oráculo deben sobrevivir; una migración
  destructiva hace FAIL (D-057). No aplica todavía mientras Room siga en versión 1 y no exista esa migración.
- `testContraste_paletaAA` — los hex de la paleta (DESIGN §1) cumplen texto/fondo ≥ 4,5:1
  (WCAG 2.1 AA); lee los colores aplicados en Compose y mide Cream/Night y Cinnamon/CardColor.

### P-Esquema Room y restricciones (TECH §3)
- `testRoom_esquemaV1_exportadoConNombresCanonicos` — el schema exportado contiene las 14 tablas
  canónicas y las columnas requeridas de movimientos, tasas, cuentas y ventas.
- `testRoom_montosGuardadosComoTextoDecimal` — el schema asigna afinidad TEXT a los campos
  monetarios seleccionados.
- `testRoom_invariantesDeDominioDeclaradasEnCallbacks` — en Room en memoria, los triggers rechazan
  monto negativo, ingreso sin proveniencia y USDT sin tasa marcada como `SIN VALUAR`; confirma una
  fila válida y la ausencia de rutas DAO de borrado de movimientos/ventas.
- `testRoom_rotacionAcotadaSoloParaErrores` — genera 501 errores, conserva los últimos 500 y
  comprueba que el movimiento guardado permanece. Ambos casos Room corren en Robolectric API 35.

### P-Casillas y sello M1 (TECH §3)
- `testCasillas_sinResultados_noSeApruebanAutomaticamente` — una lista vacía no habilita ni sella
  el hito.
- `testCasillas_passRequiereReferenciaDeEvidencia` — PASS exige referencia no vacía.
- `testCasillas_blockedRequiereCausa` — BLOCKED exige una causa.
- `testCasillas_firmaPropietarioNecesitaTodosLosPass` — el modelo solo sella con todas las PASS y
  la confirmación del propietario; es prueba de lógica, no evidencia de aprobación real.

## 3. CASILLAS POR ETAPA (en SU teléfono)

### Etapa 1 (cuaderno honesto; sello M1)
- [ ] Ancla puesta con fecha y hora visibles (se teclea una sola vez).
- [ ] Registrar gasto en ≤ 3 toques. Definición de toque (asalto A): toque = interacción con
      un control; teclear el número no cuenta; el botón grande no cuenta. Si no hay tasa
      confirmada ANTERIOR a la hora del gasto: el gasto entra igual y la tasa se pide después
      en el detalle; la casilla NO exige tasa durante el registro.
- [ ] "¿Cuánto tengo?" y "¿Cuánto gasté?" con acumulado histórico y lista por fecha, usando la
      tasa guardada en cada registro y mostrando como parcial la equivalencia USD incompleta,
      contra el oráculo §1.5.
- [ ] "Ganancia neta" con SU nota de definición (D-055); el turno del bolso no engorda.
- [ ] "Otra plata que entra": de quién/por qué OBLIGATORIO (D-042); suma saldo y entra en
      ingresos.
- [ ] Venta P2P a mano (D-046): Bs netos + tasa + USDT; prima visible con signo y su
      definición; caso sin tasa confirmada ANTERIOR → "sin valuar" (JAMÁS 0).
- [ ] Fecha editable hacia atrás CON rastro visible: "editado el [fecha y hora]" + "ver
      rastro" con la lista CREAR/EDITAR en palabras (no JSON crudo — DESIGN §3.3).
- [ ] BCV a mano o espejo-propuesta con un toque (etiqueta "no oficial"); sin red → última con
      su hora + aviso (D-034/D-051).
- [ ] Respaldo: exportar (SAF) y VERDAD COMPLETA en el teléfono del propietario (segundo
      espacio/perfil): restaurar y comparar contra el oráculo (101.522,50).
- [ ] Errores sin red / BCV sin fuente / valuar USD sin BCV → mensajes quirúrgicos (con su
      id — CONVENTIONS §5) y la app sigue navegable.

NOTA: las casillas de deuda con cuota parcial y bolso vivían aquí POR ERROR en v1.0; mudadas a
etapa 4 (asaltos A/B/E — el alcance de etapa 1 lo fija el ROADMAP/D-030).

### Etapa 2 (lectura Binance; sello M2)
- [ ] Llave creada guiado (SOLO LECTURA); pantalla de pegado sin capturas y con el texto oculto.
- [ ] Primera lectura correcta frente a la app de Binance (histórico ~6 meses; el límite real
      hacia atrás y la forma exacta: [POR VERIFICAR — F1]).
- [ ] El saldo USDT se ve CON su hora de lectura (criterio del M2).
- [ ] Re-lectura no duplica; el puente queda JUBILADA sin doble conteo.
- [ ] Sin llave / llave muerta → errores canónicos; el registro a mano sigue 100%.
- [ ] Reloj desfasado → mensaje canónico de reloj.

### Etapa 3 (el panel; sello M3)
- [ ] Panel con saldo real, gasté, ganancia neta (D-055); histórico de USDT con su sello
      (D-019/D-052); toda cifra muestra hora y fuente en el detalle y en el pie.
- [ ] El número visible siempre (barra + panel), según D-027.

### Etapa 4 (deudas y bolsos completos; sello M4)
- [ ] Deudas de comercial y civil con moneda configurable por deuda (D-036); pagos parciales
      (d1/p1..p4 del §1.4) con estados AL_DIA/PARCIAL/ATRASADA; pago en otra moneda guarda su
      tasa del pago (PRODUCT §6).
- [ ] Bolso de 6 rondas, cuota fija; el aviso del vencimiento de la próxima ronda funciona
      (AlarmManager; casilla con ahorro de batería OFF y luego ON con la guía OEM de TECH §8).

### Etapa 5 (Play o APK; sello M5) [trámite POR VERIFICAR]
- [ ] APK/AAB firmado y corriendo en un teléfono ajeno al desarrollo (plan B si Play demora).
      Firma con la KEYSSTORE de distribución (D-056; custodia del propietario —
      DATA-PRIVACIDAD §8); misma firma = actualizar sin perder datos.
- [ ] Formulario de data-safety de Play coherente con DATA-PRIVACIDAD y con el comportamiento
      de respaldo en nube/D2D comprobado en el dispositivo objetivo.
- [ ] Cuestionario visual D-026/D-027 pantalla por pantalla (capturas); contraste medido (test
      de §2); 3 días de uso real sin empujones; ninguna tipografía por debajo de 14sp (vara de
      DESIGN §7).

## 4. ATRIBUTOS DE CADA RELEASE

- SEGURIDAD: llave/SECRET nunca en logs, JSON manual ni capturas (tests de §2); almacenamiento
  sin JetSec (VETADO — F5); reglas de backup Android verificadas en el dispositivo del dueño
  para nube y D2D (TESTING §2).
- PRIVACIDAD: sin red fuera de `api.binance.com` (solo lectura) y los espejos de tasa; el
  archivo sale del teléfono SOLO con permiso manual (SAF).
- TENACIDAD: sin internet / app matada por el OEM / reloj cambiado (+2 días) → sello viejo
  visible y NUNCA envenena cifras (D-040; TECH §9).
- RENDIMIENTO: arranque < 2 s en el teléfono del propietario [PROPUESTA — se congela al
  medirlo en su teléfono real].

## 5. INVENTARIO [POR VERIFICAR] QUE ESTA VARA MONITORIZA

Forma/límites del C2C y los códigos reales de error (F1, cuenta real); el cotejo de hora del
servidor (docs oficiales; URL exacta a confirmar cuando exista la cuenta — sin atribución
falsa); alarmas exactas en Android 14 (F4, el teléfono del dueño); identidad en Play (F7);
WorkManager en el OEM del dueño; keystore de distribución (custodia — D-056); gráficas si
DESIGN las pide (sin investigación aún).

## 6. EL ACTA (registro del sello de etapa/hito)

```
SELLO — etapa [N] / hito [Mn] — fecha y hora: [__] (TZ Caracas)
Corrida por: [constructor/propietario] | dispositivo: [modelo + versión Android]
Veredictos: casilla por casilla PASS/FAIL/NOT_TESTED/BLOCKED(causa)/PARTIAL + evidencia
Cifras contrastadas: saldo / gasté / ganancia neta / prima (números exactos)
Firma del propietario: [casilla final en la pantalla de prueba por hito / nota aquí]
```

El sello SOLO se pone si TODA la etapa está en PASS real (D-045); un FAIL/BLOCKED/PARTIAL
abierto NO sella (no existe etapa sellada con BLOCKED "aprobado"). Los veredictos viven en la
tabla `acta_casillas` (TECH §3) y en la pantalla de prueba por hito (DESIGN §3.10); las actas
viven en `docs/actas/` (por ejemplo: hito-M1.md — CONVENTIONS §7).
