# TECH.md — Tecnología y Arquitectura de Kaby

Version: v1.2 CANÓNICO — ensayo aislado superado (18/18; 2026-10-21) (D-045: ronda 4 con 3 auditores — lector-constructor,
verificador y fiscal, ver lista de sesiones en review-results.md — más el ASALTO 1 del
enjambre D-053 con 5 destructores; 8 críticos + decenas de menores, todos integrados;
revisión best-effort, NO certificada)
Fecha: 2026-10-19 | Redactado por: Director, en ejercicio de la delegación D-029 + D-048.
Todo lo técnico aquí es supervisable: el propietario lo revisa y puede vetar.

## 0. CÓMO LEER ESTE DOCUMENTO (para el agente constructor y para el propietario)

- Este documento fija la TÉCNICA. El QUÉ se construye vive en PRODUCT.md (canónico v1.2) y el
  CUÁNDO en ROADMAP.md (estable v1.2). Si este doc contradice PRODUCT o ROADMAP, ganan ellos
  y este doc se corrige.
- Regla de oro del camino (D-045): cada etapa deja funcionando un "número que no miente" antes
  de abrir la siguiente. Nada de arquitectura por moda: todo elegido para entregar etapas
  cortas verificables.
- Principios duros que la técnica NO puede romper: D-037 (cuenta real y firme, sin mentiras ni
  datos inflados); D-041 (aislamiento quirúrgico: si un componente falla, NO derriba lo demás);
  D-040 (sellos de tiempo honestos con zona horaria; "sin fuente" nunca se inventa); D-013
  (llave Binance SOLO-LECTURA, creada por el propietario guiado, jamás en el código).
- GLOSARIO mínimo (_pedido del auditor A1_): **C2C** = el mercado de comercio entre personas de
  Binance, donde tu USDT se vende por Bs (por eso "peer-to-peer", P2P); **SAF** = Storage Access
  Framework, el selector de archivos nativo de Android para exportar/importar; **baúl de
  prueba** = base de datos segregada para restauración (se restaura dentro de un espacio de pruebas
  propio, se compara, y luego se destruye — JAMÁS toca los datos vivos); **espejo** = sitio que
  publica la tasa del BCV sin SER el BCV (dolarapi/dolarvzla).
- Declaración permanente: la implementación final valida TODO lo afirmado aquí con las pruebas
  de TESTING.md — este doc es best-effort, no certificado.
- La evidencia técnica original vive en `investigacion-tecnica.md` y el ledger completo en
  `decisions-ledger.md`, ambos en la carpeta de estado externa listada en HANDOFF-KABY §16;
  no están incluidos en este repositorio y su contenido no se revalidó en esta auditoría.
  Las citas "investigación F#" apuntan al primer archivo. El apéndice A resume D-042..D-052;
  verifica fuentes oficiales vigentes antes de implementar integraciones externas.

## 1. STACK ELECTO [DIRECTOR-DELEGACIÓN D-049 — supervisable]

| Pieza | Elección | Por qué (varas objetivas) |
|---|---|---|
| Lenguaje | Kotlin | Es el lenguaje dominante en las apps Android modernas que se citan como referencia (Ivy Wallet: 100% Kotlin — investigación F6, repo archivo, NO vivo); docs oficiales en Kotlin. |
| Interfaz | Jetpack Compose (Material 3) | Elección de la casa (D-049); la identidad vigente es «Cartera de piedra y tinta» y sus colores exactos están en DESIGN.md. La anterior dirección «café de noche» queda como antecedente de D-027. [elección supervisable, no "recomendación citada por fuente" — A3] |
| Base de datos | SQLite vía **Room** | Room = "recommended persistence library" oficial en arquitectura offline-first (investigación F3); Single Source of Truth; migraciones versionadas + esquema exportable a tests. [D-039 queda CONFIRMADA por delegación D-048 con esta elección] |
| Red | Retrofit + OkHttp | Elección de la casa (D-049, supervisable): timeouts/reintentos configurables (necesario para D-041); el camino menos exótico para un constructor único. (Ktor sería válida; no elegimos la exótica.) |
| Tareas de fondo | WorkManager + AlarmManager (`setAlarmClock`) | WorkManager para copia/lecturas periódicas; `setAlarmClock` = la de mejor garantía ENTRE las APIs de AlarmManager (F4: "strongest guarantee among AlarmManager APIs"); degradación documentada (§8). |
| Guardar la llave | **Diseño pendiente:** Android Keystore (AES-GCM) + DataStore; Tink optativa canónica | La consulta actual usa credenciales temporales sin persistencia. Cuando se implemente, el material de clave no será exportable desde Android Keystore; respaldo por hardware depende del dispositivo y del algoritmo, y se verificará con `KeyInfo` antes de afirmarlo. EncryptedSharedPreferences/JetSec está vetado por D-050. |
| Nube | **NINGUNA de la app.** Sin Firebase, sin servidores, sin cuentas. | La app no sube datos a un servicio propio; las copias del sistema se excluyen por manifest y se verifican en el dispositivo objetivo (§6). La única salida iniciada por Kaby es la exportación manual del propietario. |
| minSdk | 26 (Android 8.0) — provisional | Se puede usar 26 para el desarrollo inicial. Antes de instalar en el teléfono del propietario, confirmar su nivel de API y verificar que las dependencias elegidas lo soporten. Si el teléfono está por debajo de API 26, solo se baja `minSdk` si todo el stack es compatible; de lo contrario, el propietario decide el límite de soporte. Hasta esa comprobación, 26 sigue provisional. |
| Librerías extra | Vico (gráficas Compose, open source) SOLO si DESIGN las pide | [POR VERIFICAR al activarla — SIN investigación propia aún] (A3: hoy es un nombre sin evidencia). El núcleo no depende de ello. |
| Firma de la app | Keystore de DISTRIBUCIÓN propio, generado en la etapa 5, custodiado por el propietario (archivo + contraseñas fuera del repo y del teléfono de trabajo) | Misma firma = instalar actualizaciones sin perder datos (paquete + firma idénticos); perder la llave = no más actualizaciones firmadas (plan: nueva app + importar respaldo). [D-056 — asalto E] |
| Versiones del stack | AGP / Kotlin / BOM Compose / Room / Retrofit / OkHttp / WorkManager / targetSdk | Se CONGELAN en versión EXACTA antes de M1 (apuntadas en el CHANGELOG interno); después suben SOLO por decisión explícita con TODO el paquete re-corrido. Dos constructores con este doc deben producir el mismo binario. [asalto A5] |

Prohibiciones del stack: nada de librerías de "secure storage" deprecadas; nada de SDKs de
nube; ningún permiso del Manifest fuera de los que §8 justifica y `docs/permisos.md` enumera.
El APK M1 también fusiona permisos de WorkManager y AndroidX; el permiso efectivo se coteja
contra el artefacto compilado con `tools/verify-android-permissions.py`. Los permisos de
notificaciones/alarmas de M4 no se incluyen antes de implementar esa función.

## 2. ARQUITECTURA GENERAL (capas y reglas)

```
[Pantallas Compose]      ← solo pintan; sin lógica de datos
   ↑ observan Flow
[ViewModels]             ← estado de pantalla; NO toca red ni base
   ↑
[Repositories]           ← CASA de las reglas: el cuadre, los saldos, la valuación
   ↑ guarda/lee
[Room DB]                ← EL NÚMERO QUE NO MIENTE (D-037)
   ↑
[Clientes del exterior]  ← BinanceClient (C2C), TasaClient (espejos BCV) — aquí viven las fallas
                             (timeouts, reintentos, degradación — D-041)
```

Reglas arquitectónicas:
1. **SSOT**: Room es la ÚNICA fuente de verdad de los números (investigación F3). La red
   JAMÁS es fuente de verdad: la red solo TRAE datos y entrega al Repositorio, que decide.
2. **Flujo unidireccional**: datos suben (Room → Repository → ViewModel → UI); intenciones
   bajan (UI → ViewModel → Repository). No hay atajos pantalla→red.
3. **Aislamiento (D-041)**: cada cliente exterior vive detrás de SU interfaz Kotlin con SUS
   fallas. Binance cae → la app sigue 100% para registro manual. Tasa cae → la app sigue;
   BCV entra a mano (D-034). Alarmas mueren (OEM/reinicio) → compensación al abrir (§8).
   NINGÚN error de red se presenta como error de la app entera.
4. **Número reproducible**: toda cifra mostrada proviene de una consulta Room reproducible.
   Definición textual de las INSUMOS del cuadre (el corazón, corregida tras A1/A2):

   **CUADRE (D-042; definición canónica en el cuadro de PRODUCT y el dataset D-032/D-033):**
   saldo_del_Mercantil = **ancla** (saldo real tecleado por el propietario una vez, CON su
   fecha/hora) + sum(INGRESOS en Bs que tocaron el Mercantil: normales, TURNO_BOLSO, y
   ventas_p2p_mano VIVA de cualquier casa — API o mano) − sum(GASTOS en Bs, INCLUIDAS las
   compras de USDT pagadas en Bs: la plata SALIÓ del Mercantil aunque se convierta en USDT).
   El cuadre cuadra EL MERCANTIL: los USDT en Binance no son parte del saldo en Bs (su saldo
   vive en estado_cuentas.saldo_usdt, etapa 2); las métricas de PRODUCT (ingresos/ganancia)
   usan la NATURALEZA de cada fila para no engordar (el TURNO no es ganar, el AJUSTE no es
   gasto).
   **Regla anti-doble-conteo**: una venta P2P vive en UN SOLO sitio: su fila tiene CASA ÚNICA
   en `ventas_p2p_mano` (fuente MANO en puente, fuente API desde etapa 2) y NO se replica en
   `movimientos`; la plata de terceros ("otra plata que entra", D-042) SÍ vive en
   `movimientos` como INGRESO con proveniencia_tercero. Las filas JUBILADA del puente NO
   cuentan (retira el doble conteo del puente→API). El algoritmo exacto y datasets van a
   TESTING.md como caso canónico.

Los ESTADOS VIVOS de las cuentas (pedidos por el auditor A1; faltaban en v1.0): viven en la tabla
**estado_cuentas** (§3): ancla, último cuadre con hora, descuadre abierto con su cifra, y
saldo USDT con su hora de lectura (etapa 2).

## 3. MODELO DE DATOS (Room / SQL — esquema v1.1, corregido tras auditoría)

Esquema v1.1 (Room exporta su schema al repo para tests de migración). La base vive en el
almacenamiento interno privado de la app; este paquete no especifica una capa de cifrado
adicional para toda la base. La llave Binance se cifra por separado en TECH §6.
Tablas:

- **movimientos** (gastos e ingresos):
  id, **tipo** (GASTO|INGRESO — DIRECCIÓN del dinero), **naturaleza** (NORMAL|AJUSTE_CUADRE|
  TURNO_BOLSO — COMO CUENTA: el AJUSTE cierra descuadres sin engordar "gasté" ni "ganancia";
  el TURNO_BOLSO suma a la plata pero NO a ingresos/ganancia, tal como PRODUCT define "plata
  que vuelve") [corregido tras A1: con solo GASTO|INGRESO el constructor habría inflado
  cifras contra D-037], moneda (Bs|USDT), monto, tasa_bcv_al_momento (guardada CON la hora de
  su fuente — D-040; la tasa que VALE = la ÚLTIMA fila CONFIRMADA de `tasas_bcv` con
  hora_fuente ≤ la fecha-hora del movimiento; si NO existe NINGUNA anterior, el registro
  entra "sin valuar" y la tasa se pide a mano al propietario (regla ÚNICA de valuación —
  regla textual unificada tras el asalto A2: el "día" solo elige qué fila PROPONE la UI;
  la validez la fija esta regla)), fecha_hora (con TZ; editable hacia atrás, editación con
  rastro en `bitacora`), categoría, motivo/nota, origen (MANO|BINANCE_API),
  proveniencia_tercero (texto OPCIONAL: de quién/por qué — usado por "otra plata que entra"
  D-042), ref_deuda + ref_cuota (si el pago salda una cuota — A1 #5), tasa_pago +
  tasa_pago_tipo (BCV|P2P|OTRA) + su hora — SOLO si el pago va en otra moneda que la deuda
  (PRODUCT §6; hallazgo A1 #6), ref_bolso + ref_ronda (si el movimiento es la recepción del
  turno de una ronda de un bolso — A1 #7), timestamp_creacion, timestamp_ultima_edicion.
  **La línea base M1 no incluye BORRAR**: solo CREAR/EDITAR con rastro. La puerta de `PUERTAS-M1.md` debe confirmar esa línea base antes de cerrar la unidad CRUD/bitácora; hasta entonces no se implementa borrado. Corregir es una edición con rastro en bitácora.
- **deudas**: id, nombre, tipo_ente (COMERCIO|CIVIL|BOLSO — D-043), moneda_de_cuota
  (configurable — D-036), periodicidad (SEMANAL|MENSUAL|OTROS; hallazgo A1 #5), monto_total,
  cuota_periodo, fecha_inicio, saldo_pendiente, estado (ACTIVA|PAGADA|QUIETA), notas.
  El estado exhibible "al día / parcial / atrasada" + "próximo pago" NUNCA se guarda: es
  DERIVADO de la tabla **cuotas** (query) — si se guardara, envejecería y mentiría.
- **cuotas** (nueva — A1 #5): id, deuda_id, secuencia, vencimiento (fecha con TZ), monto,
  estado (PENDIENTE|PARCIAL|PAGADA). La cuota parcial se cierra cuando el acumulado de
  pagos ref == monto; el próximo vence SOLO después de la anterior cubierta.
- **bolsos** (tandas — D-043): id, nombre, modo_cuota (FIJA|VARIABLE), cuota_por_ronda (si
  fija), ronda_actual. + **bolso_participantes** (nueva — A1 #7): id, nombre, notas
  (los bolseros se repiten entre rondas). + **bolso_participaciones**: id, bolso_id,
  participante_id, ronda, cuota_comprometida, estado (PENDIENTE|PAGADA).
  **Deuda del bolso**: vive UNICAMENTE como fila de `deudas` con tipo_ente=BOLSO (casa de
  los avisos, según D-043); `bolso_participaciones` solo anota el ORDEN/rondas; el derivado
  "lo que debes por bolso" NO se duplica (se cuenta desde la fila de deudas — A1 #7).
- **ventas_p2p_mano** (D-046 — puente etapa 1; misma forma que las filas de API, para que la
  transición no cambie la casa):
  id, fecha_hora (con TZ; editable hacia atrás como cualquier registro, editación con
  rastro), bs_netos, tasa_p2p (Bs/USDT capturada a mano), usdt_cantidad,
  **tasa_bcv_al_momento + hora de tasa** (añadida por A1/A2 CRÍTICO: sin el BCV no hay
  prima contra BCV), **prima** = bs_netos − usdt_cantidad × tasa_bcv_al_momento (CON SIGNO,
  como PRODUCT §5; misma REGLA ÚNICA de tasa que movimientos — asalto A2: la ÚLTIMA
  CONFIRMADA con hora_fuente ≤ la hora de la venta; sin ninguna anterior → "sin valuar",
  JAMÁS 0; redondeo a 2 decimales POR FILA — el total del día suma primas ya redondeadas
  [enmenda del ensayo C, 2026-10-21]),
  proveniencia (quién/por qué), orderNumber (NULL en el puente a mano; los de la API lo
  traen — clave de dedup; NULL a mano NO deduplica: si la misma venta llega por API con
  su orderNumber, la fila a mano se JUBILA y la API queda como única VIVA [enmenda del
  ensayo C]; hallazgo A1 #2), fuente (MANO|API), estado (VIVA|JUBILADA).
- **tasas_bcv**: id, valor (Bs/USD), hora_fuente (CON TZ — D-034/D-040), fuente
  (MANUAL|ESPEJO_DOLARAPI|ESPEJO_DOLARVZLA), confirmada_por (HUMANO|—).
- **lecturas_binance** (etapa 2+): orderNumber (clave de dedup), tradeType,
  fecha_hora_orden (del createTime, con TZ), unitPrice, totalPrice (Bs), amount (USDT),
  leido_en (hora local de la lectura — D-040), ventana_consulta.
- **estado_cuentas** (nueva — A1 #1): fila única: ancla_monto, ancla_fecha_hora (TZ),
  ultimo_cuadre_at, estado_cuadre (SIN_ANCLA|CALCULADO|DESCUADRE_ABIERTO), cifra_descuadre,
  saldo_usdt + leido_en (etapa 2: "el saldo USDT se ve con su hora de lectura").
- **bitacora**: id, tabla, fila_id, acción (CREAR|EDITAR — ver BORRAR arriba), antes/después
  (JSON), timestamp.
- **bitacora_errores**: id, capa (RED_BINANCE|TASAS|DB|AVISO|—; corregido el typo REDE_ —
  A1 #14), mensaje, detalle técnico, timestamp (rotación local). Para el diagnóstico §9.
- **settings**: moneda de referencia de pantalla [propuesta — nota tras A2: D-044 habla de
  la VALUACIÓN, no de la moneda de pantalla; esta elección es supervisable aparte], aviso de
  exportación (última vez), guía de alarmas por OEM vista o no, versión del layout de
  respaldo, PIN/biometría optativa si el propietario la pide (hueco §12).

Invariantes con CHECK (lista completa → TESTING.md): monto >= 0; tipo define el sentido;
si moneda=USDT debe existir tasa_bcv_al_momento válida (o entra "sin valuar" — nunca cero
silencioso, D-037); prima congelada al guardar (honestidad del puente); JUBILADA cuenta 0.

Reglas añadidas tras el ASALTO 1 del enjambre (valen para TODAS las tablas contables):
- **Edición y cuadre** — editar CUALQUIER fila contable con fecha ≤ `ultimo_cuadre_at`
  (gasto, venta o pago) reabre DESCUADRE_ABIERTO con la diferencia visible (no solo
  movimientos; asalto A6).
- **"SIN VALUAR"** — representación explícita: `tasa_bcv_al_momento` NULL + bandera de
  valuación 'SIN VALUAR'; JAMÁS se rellena con 0 ni con la tasa de otro día (D-037).
- **Desempate de tasa** — para un movimiento vale la ÚLTIMA tasa CONFIRMADA cuya hora_fuente
  sea ≤ la hora del movimiento; si no hay ninguna antes → SIN VALUAR (sin excepciones).
- **CHECK de proveniencia** — fila INGRESO con naturaleza NORMAL exige proveniencia_tercero
  ("de quién + por qué", D-042); la venta P2P no es INGRESO del cuadro de movimientos (vive
  en ventas_p2p_mano, casa única — D-046).
- **acta_casillas** (tabla nueva — asalto E8) — id, hito, casilla_id, veredicto
  (PASS|FAIL|NOT_TESTED|BLOCKED|PARTIAL), causa_bloqueo (texto opcional), evidencia (ruta a
  log/captura), timestamp. La llena la pantalla de prueba por hito (DESIGN §3.10) y el ACTA
  de sellado (TESTING §6) la lee de aquí.
- **Migraciones (D-057)** — con datos vivos la migración NUNCA es destructiva; respaldo
  automático ANTES de migrar (baúl de prueba); `testMigracion_v1aV2_sinPerdida` por cada
  versión del esquema.
- **Logs (asalto C)** — no se instala interceptor de logging de red en M1; la bitácora Room solo
  admite códigos de tasa permitidos o mensajes genéricos fijos; `detalle_tecnico` JAMÁS contiene
  una pila completa. `testLogs_sinSecretNiLlave` verifica la ruta persistida con canarios
  sintéticos, no lee logcat. La llave/SECRET no deben poder asomar en ningún log del sistema.
- **Signo del saldo (asalto A2)** — lo fija el TIPO: INGRESO suma al saldo, GASTO resta;
  la NATURALEZA solo decide qué MÉTRICAS engorda (D-055). La "plata que vuelve" que PAGA
  POR TI (la ronda del bolso que pagó un tercero) llega como INGRESO + TURNO_BOLSO: mueve
  el saldo, no engorda métricas. Cuando TÚ pagas tu propia cuota, es GASTO (dinero que sale).
  Celdas derivadas que el ensayo aislado pidió EXPLÍCITAS (2026-10-21): GASTO×TURNO_BOLSO
  = saldo−, métricas 0 (pagas tu cuota del bolso); AJUSTE_CUADRE existe como GASTO o
  INGRESO según el sentido de la corrección — el signo lo fija el TIPO, las métricas
  NUNCA se engordan.
- **AJUSTE_CUADRE (asalto A2)** — SÍ es fila contable: mueve el saldo (signo de su tipo)
  PERO no entra en "ingresos" ni en "gasté"; su meta es cerrar la diferencia visible del
  cuadre, una sola vez.

## 4. INTEGRACIÓN BINANCE (plan concreto para el constructor)

**Incremento implementado 2026-10-21 (no sello M2):** consultas limitadas a hora, permisos, historial y Funding. La documentación oficial vigente confirma el endpoint C2C de §4.2, 30 días por ventana, máximo 100 filas y seis meses de historial. `GET /api/v3/time` ajusta la firma; `GET /sapi/v1/account/apiRestrictions` comprueba lectura exclusiva; `POST /sapi/v1/asset/get-funding-asset` consulta Funding (POST de lectura, no orden ni transferencia). `estado_cuentas.saldo_usdt` almacena únicamente Funding `free`; la UI lo identifica y no lo llama saldo total Binance.

La transición contable de §4.3 queda afinada por seguridad: guardar la lectura no suma bolívares; el propietario confirma el neto Mercantil y selecciona el puente manual que corresponde, o declara que aún no lo anotó. Una transacción jubila solo ese puente e inserta la venta API con `orderNumber` único y bitácora. Un puente no seleccionado sigue vivo. No se asume que todo el historial manual esté cubierto por los seis meses ni que `totalPrice` equivalga al neto bancario (D-033 todavía requiere cuenta real).

Hasta superar la prueba OEM de §6, las credenciales solo viven durante la consulta, sin DataStore ni otra persistencia. Se descartan las referencias al terminar/cancelar y se cancela al pasar la app a segundo plano; no se promete borrado forense de Strings administrados por la JVM. El diseño de guardado AES-GCM + DataStore sigue pendiente y no se presenta como implementado. Cadencia actual: botón explícito. Fuentes, contrato ejecutado, filtros y limitaciones: `INTEGRACION-BINANCE.md`.


4.1 **La llave (trámite del propietario, guiado por la app):** pantalla paso a paso: en Binance
   → API Management → crear llave (system generated) → habilitar SOLO lectura ("Enable
   Reading"), JAMÁS retiros/operaciones → copiar API key y SECRET → pegar en la app (una vez).
   La app valida con una lectura real del historial (lectura exitosa = llave sana).
   La SAPI requiere firma HMAC-SHA256 del query string con el SECRET → el SECRET vive cifrado
   EN EL TELÉFONO (§6), nunca en el código ni en documentos [D-013].
   Antes de la primera lectura, la app ajusta el tiempo con el servidor (time-sync; la firma
   depende del reloj — hallazgo A1 #8) [endpoint de hora del servidor: POR VERIFICAR — los
   docs oficiales y la cuenta real lo confirman al implementar; F1 no lo cita].
   [POR VERIFICAR con la cuenta real: toggles exactos de 2026 y si el C2C exige algo más — F1.]

4.2 **Llamada canónica** (etapa 2, hito M2) [endpoint POR VERIFICAR: cuál variante está viva
   en 2026 — orderMatch vs trading-order (A3 #1; F1)]:
   `GET https://api.binance.com/sapi/v1/c2c/orderMatch/listUserOrderHistory`
   params: tradeType (SELL primero — ventas propias), startTimestamp/endTimestamp (ms;
   **ventana MÁXIMA 30 días por llamada** — literal confirmado en DOS espejos de la doc
   (hexdocs, Postman); la atribución "doc oficial" la corrigió A3 #2; CONFIRMAR en cuenta
   real junto con el límite hacia atrás), page/rows (rows tope 100 [POR VERIFICAR]),
   timestamp + recvWindow + firma HMAC-SHA256, header X-MBX-APIKEY.
   **Reconstrucción del historial** [D-052, DIRECTOR-DELEGACIÓN — corrección A2 #10: citada]:
   ventanas mensuales encadenadas hacia atrás (~6 meses del ROADMAP — cuánto atrás de verdad
   permite el servicio: [POR VERIFICAR], F1), paginando hasta vaciar cada ventana;
   deduplicación por orderNumber en `lecturas_binance`.

4.3 **Mapeo** C2C → tablas: unitPrice → tasa_p2p; totalPrice → bs_netos; amount →
   usdt_cantidad; createTime → fecha_hora_orden. La sincronización etapa 2 pasa por
   `lecturas_binance` (dedup) y DE AHÍ pobla `ventas_p2p_mano` (upsert por orderNumber,
   estado=VIVA) — la casa del cuadre no cambia (regla anti-doble-conteo §2). El puente a
   mano D-046 se marca JUBILADA por coincidencia confirmada al conciliar su orden; nunca por el mero hecho de conectar. Resto de campos de la respuesta:
   [shape completa POR VERIFICAR con la cuenta real — F1].

4.4 **Cuándo lee Binance** [propuesta [DIRECTOR-DELEGACIÓN] — nota tras A1/A2: ROADMAP §6/§12
   tiene "cuándo leer" como ABIERTO fino del propietario; el ABIERTO sigue ABIERTO
   canónicamente; esta es la propuesta supervisable]: (a) al abrir la app si hay red
   (principal), (b) botón manual "traer Binance" (siempre). Lector diario WorkManager: la
   propuesta es default-OFF (el toggle existe SOLO si el propietario lo pide). Sin red NO
   llama; con error → aviso quirúrgico + sigue con la última lectura buena CON SU HORA.

4.5 **Errores canónicos de Binance** (D-041 aplicada; mensajes textuales en §9).
   [Códigos y valores: [DIRECTOR-DELEGACIÓN — constantes ajustables; los CÓDIGOS REALES que
   devuelve el C2C con la cuenta real — F1] (A2/A3 #12, #7)]:
   - llave muerta o revocada: "Tu llave Binance expiró o se revocó — ve a Llave y crea otra"
     (la app no se rompe; registro manual sigue).
   - reloj desfasado (por eso el time-sync 4.1): "La hora de tu teléfono no concuerda con
     Binance — ajusta tu hora (o revisa el receptor de hora)".
   - SECRET mal pegado/incompleto: "Revisa que el SECRET esté completo y sin espacios".
   - "bug interno" SOLO después de descartar los dos anteriores (hallazgo A1 #8).
   - timeout/latencia (D-041): timeout 15 s + 2 reintentos con backoff (1 s, 4 s) → degradar
     a cache con la hora del último bueno + aviso honesto "sin contacto con Binance desde
     [hora]".
   - 429 / límite de ritmo: respetar el tiempo que la respuesta pida esperando (Retry-After);
     nunca martillar la API.

## 5. TASAS BCV (técnica D-034/D-040/D-044)

- La app NUNCA inventa el BCV. Tres puertas, en orden:
  1) **A mano** (siempre disponible): el propietario teclea el valor + la hora del tablero
     BCV; la app propone "ahora" como hora default editable (D-040).
  2) **Pre-llenado con espejo + confirmación humana** [D-051, DIRECTOR-DELEGACIÓN]:
     con red, TasaClient consulta `https://ve.dolarapi.com/v1/dolares` (VERIFICADO EN VIVO,
     investigación F2; espejo alterno: rates.dolarvzla.com) y la app PROPONE el valor con su
     `fechaActualización` y etiqueta honesta "espejo NO oficial — confírmalo con tu tablero
     si quieres exactitud". UN TOQUE del propietario confirma → fila con fuente=ESPEJO,
     confirmada_por=HUMANO. Esto NO es automático puro: respeta D-034 (ninguna fuente hoy
     GARANTIZA el BCV oficial: solo espejos — F2). NOTA de trazabilidad: PRODUCT §6 describe
     "a mano o última guardada"; el flujo de pre-llenado es una EXTENSION traída por
     delegación D-048 (D-051) — supervisable por el propietario; PRODUCT/su decisión ganan
     (A1 #12).
  3) **Respira (D-044)**: los Bs en mano se valúan a la tasa vigente y SE RE-VALÚAN cuando
     entra una tasa nueva; los gastos/deudas NO se re-valúan (congelados a la tasa de su
     momento).
- **Cuándo corre TasaClient** [asignación textual — A1 #12]: al abrir la app (con red) y al
  entrar a la pantalla de tasas. El "intento diario" que PRODUCT marca [CONFIRMADO D-006;
  declaración "deseado, no infraestructura obligatoria" D-011] usa WorkManager diario
  best-effort [DIRECTOR-DELEGACIÓN — asignada; puedo fallar silenciosamente como ya aprobó
  F4; la apertura compensa]: si no corre, nada se rompe (la tasa a mano es la vara).
- **Sin red y sin fuente vigente**: la app usa la ÚLTIMA tasa guardada, la MUESTRA CON SU
  HORA en cada pantalla donde importe + aviso "BCV sin fuente vigente desde [hora]"
  (D-034/D-040).
- **Parseo** (F2, corregido por A3 #8): campos `fuente` ("oficial" | "paralelo"), `promedio`
  (compra/venta = null en Venezuela), `fechaActualización` en ISO con TZ EXPLÍCITA que
  PUEDE VENIR como `−04:00` (oficial) o como `Z`/UTC (paralelo) — el parser NORMALIZA ambas
  al guardar (D-040), junto con la hora local de lectura.
- **Tasa del momento con fecha editable hacia atrás** (regla textual — A1 #13, unificada
  con el asalto A2 en UNA regla): la ÚLTIMA fila CONFIRMADA de `tasas_bcv` con hora_fuente
  ≤ la fecha-hora del movimiento vale para ese registro; si no hay NINGUNA anterior, el
  registro entra "sin valuar" y la tasa se pide a mano al propietario (con su hora), tal
  como ya permite D-034. (El "día del movimiento" solo decide qué fila PROPONE la UI; la
  validez la fija la regla del desempate.)

## 6. GUARDAR LA LLAVE EN EL TELÉFONO [D-050, DIRECTOR-DELEGACIÓN]

- Patrón (investigación F5): AES-GCM; la CLAVE de cifrado vive en **Android Keystore**
  (material no exportable; respaldo por hardware sujeto a compatibilidad del dispositivo y
  por verificar con `KeyInfo.getSecurityLevel()` o `isInsideSecurityHardware()`; ver
  [documentación oficial de Android Keystore](https://developer.android.com/privacy-and-security/keystore);
  el ciphertext
  de apiKey+SECRET vive en el **DataStore** del
  almacenamiento privado de la app. Tink queda como capa optativa canónica de la industria
  [detalle fino POR VERIFICAR al construir, incluida la rotación del keyset si toca].
- VETADO: EncryptedSharedPreferences/JetSec (deprecado — F5); SECRET en texto plano; SECRET
  en logs, en el JSON exportado o en capturas de pantalla (la pantalla de pegar la llave usa
  flag de no-captura y muestra la llave ofuscada). La exclusión del ciphertext frente a copias
  Android se configura en el manifest y se prueba en el dispositivo objetivo (§6); hasta esa
  prueba no afirmar que ningún respaldo del sistema pueda contenerlo.
- Muerte de la llave: botón "borrar llave de la app" (destruye el ciphertext local) + guía
  "y borra la llave en Binance" (trámite del propietario). El JSON manual no incluye la llave;
  cualquier copia del sistema queda sujeta a las reglas y prueba de §6. En un teléfono nuevo,
  el propietario registra una llave NUEVA guiado por la app.
- **Reglas de respaldo del sistema:** el manifest debe declarar `allowBackup=false`, reglas
  `fullBackupContent` para Android 11 y anteriores, y `dataExtractionRules` que excluyan por
  separado `cloud-backup` y `device-transfer` en Android 12 y posteriores. En algunos OEM,
  `allowBackup=false` por sí solo no impide la transferencia dispositivo a dispositivo (D2D);
  por eso, la prueba estática del manifest no demuestra el comportamiento de cada teléfono.
  Antes de guardar la llave Binance o liberar la app, probar nube y D2D en el dispositivo del
  propietario y registrar el resultado. Si no se puede comprobar, no afirmar que Android nunca
  copia datos. Fuente: [reglas de Android 12 para respaldo y D2D](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore).

## 7. RESPALDO Y RESTAURACIÓN (técnica D-038)

- **Qué respalda**: TODO el contenido de las tablas (§3) EXCEPTO la llave Binance (§6). El
  contenido lógico es JSON versionado con `schema_version`. (Export CSV de solo lectura para
  referencia externa es optativa; NO es el formato del respaldo.) La representación y protección
  del archivo SAF siguen [ABIERTO]: el JSON local actual no aprueba un formato de exportación.
- **Comparación SAF** [CONSTRUCTOR]: JSON plano se abre sin contraseña y quien obtenga el archivo
  puede leer movimientos, deudas, notas y demás contenido; JSON cifrado con contraseña protege
  el contenido del archivo mientras permanece cifrado y exige esa contraseña al restaurar; si se
  pierde, Kaby no puede recuperarlo. El formato cifrado necesita una clave derivada de la
  contraseña y cifrado autenticado para detectar alteraciones. Por el contenido financiero, el
  constructor recomienda cifrar el respaldo. El propietario conserva la decisión [ABIERTO]; la
  llave Binance JAMÁS viaja.
- **Cuándo**: (1) intento de copia local diaria en el almacenamiento privado de la app (WorkManager —
  de ejecución inexacta, sujeta a restricciones del sistema: F4); (2) intento oportunista de
  copia al pasar la app a segundo plano y al volver a abrirla, sin tratar los callbacks de
  ciclo de vida como garantía: Android puede terminar el proceso sin llamar a `onDestroy()`
  ([ciclo de vida](https://developer.android.com/guide/components/activities/process-lifecycle);
  [PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)).
  Cada guardado se confirma primero en Room; la copia local es recuperación, no el almacén
  primario; (3) cuando el propietario elija el formato, **exportación manual SIEMPRE**
  (SAF → Downloads/Drive del propietario) — la copia que el propietario puede conservar y
  restaurar independientemente del teléfono; no se sustituye por el respaldo del sistema
  (cuyo comportamiento debe verificarse según §6). La
  app lo dice en pantalla con su contador honesto "respaldo externo hace [X días]" + aviso
  si pasan **7 días** [DERIVADO de D-037 — valor propuesto, constante configurable; el
  número se somete al propietario — A2 #9] (aviso, no regaño: el propietario manda).
- **Restauración** (orden textual — A1 #10/A2 tally): import (SAF) → **1) comparar ANTIGÜEDAD
  del archivo vs los datos del teléfono** (aviso si importas algo más viejo — PRODUCT/ROADMAP
  exigen la advertencia) → 2) verificación VERDAD: restauración en **baúl de prueba** (glosario
  §0), comparar (número de movimientos, saldo, cuadre; método del criterio 5 de la etapa 1 del
  ROADMAP + detalles en DATA-PRIVACIDAD.md — la cita errónea "M0" corrigió A1 #15), si
  desigual → ABORTAR y mostrar el resumen de diferencias. Jamás importa por encima sin doble
  confirmación.
- Teléfono nuevo (hueco de ROADMAP §12, VINCULADO D-038 → DATA-PRIVACIDAD — ubicación
  corregida por A2/#14): instalar → restaurar (§7) → crear llave NUEVA guiado (§6).

## 8. AVISOS DE CUOTA/BOLSO Y LA BATERÍA (D-041 aplicada; permisos textuales — A1 #9)

- **Permisos del APK M1 actual**, verificados en el manifest combinado del APK debug del
  2026-10-21 y enumerados en `docs/permisos.md`:
  - `INTERNET`: conexiones HTTPS a los espejos de tasa; si falta, queda la entrada manual.
  - `WAKE_LOCK`, `ACCESS_NETWORK_STATE` y `RECEIVE_BOOT_COMPLETED`: los fusiona WorkManager
    para ejecutar workers, evaluar restricciones como `NetworkType.CONNECTED` y reprogramar
    trabajo persistente al reiniciar. WorkManager conserva un wakelock mientras ejecuta workers
    ([documentación oficial](https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/identify-wls)).
    El worker de tasa y la copia local son de mejor esfuerzo; no bloquean el registro manual.
    La [referencia oficial de WorkManager](https://developer.android.com/reference/androidx/work/package-summary)
    enumera estos permisos y su función.
  - `com.mofoxx.kaby.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`: permiso de nivel `signature`
    definido por AndroidX Core para proteger receptores dinámicos no exportados; no es una
    solicitud de permiso runtime al propietario.
- `FOREGROUND_SERVICE` aparece en la dependencia WorkManager pero el manifest de la app lo
  elimina explícitamente; no está en el APK M1 verificado.
- **Permisos previstos para avisos M4; no presentes en el APK M1**: `POST_NOTIFICATIONS`
  (runtime desde Android 13) y `SCHEDULE_EXACT_ALARM` para alarmas de cuota. Antes de
  implementar esa función se verifica la documentación vigente, el comportamiento del API
  objetivo y la configuración del teléfono; en Android 14+ el acceso a alarmas exactas no se
  asume concedido.
- Cualquier permiso nuevo se debe añadir con su motivo, pantalla y consecuencia a
  `docs/permisos.md`; el verificador compara esa lista con el APK generado.
- Avisos M4: NotificationChannel propio + `AlarmManager.setAlarmClock()` (la de mejor garantía
  ENTRE las APIs de AlarmManager — F4, corregido por A3 #4 que rechazó "la más fiable del
  sistema"). Play puede RECHAZAR usos no justificados (literal F4): la
  justificación declarada es "avisos de cuotas" y es [POR VERIFICAR el desenlace del trámite]
  — la compensación al abrir es el plan B siempre (A3 #3).
- **Compensación al abrir (la garantía dura)**: cada apertura ejecuta "recalcular vencidos":
  consulta Room de cuotas vencidas sin registro y muestra el resumen quirúrgico AUNQUE las
  alarmas hayan muerto (OEM/reinicio — F4). El aviso importante NO depende de la batería:
  depende del hábito del propietario de abrir la app.
- Guía por teléfono: pantalla "si tus avisos llegan tarde": instrucciones para los OEM
  comunes (Xiaomi/Huawei/Realme: autostart + sin restricción de batería para la app) — de F4.

## 9. ERRORES QUIRÚRGICOS — LA CONVENCIÓN (D-041 formalizada)

Reglas para el constructor:
1. **Ningún error genérico.** Todo mensaje nombra: QUÉ falló (capa), POR QUÉ probable, y QUÉ
   puedes hacer. Ejemplo: "No pude llegar a Binance (¿internet cortado?). Tus números siguen
   con tu registro manual. Última lectura con éxito: ayer 10:32."
2. **Aislamiento por módulo**: la falla de un cliente exterior (§2) nunca derriba el resto;
   la UI sigue navegable y el error se muestra EN su módulo.
3. **El estado nunca se miente**: si un dominio está caído, sus pantallas muestran "riesgo de
   datos" con el sello de la última hora buena (D-040). Cero NUNCA disimula una falla (cero
   es un dato, no una excusa — D-037).
4. **Reintentos con tope**: 2 reintentos backoff; después degradar. JAMÁS reintentos
   infinitos (batería + rate limits).
5. **Diagnóstico copiable**: toda pantalla de error trae "copiar diagnóstico" (versión de la
   app, hora, mensaje — SIN montos del propietario, SIN llave, SIN datos personales).
6. Cada excepción se escribe en `bitacora_errores` (§3) para que la auditoría de TESTING sea
   reproducible.

## 10. PRUEBAS TÉCNICAS INHERENTES (TESTING.md las atesta después)

- Tests unitarios del CUADRE (la query de §2) con el dataset canónico de la simulación
  D-032/D-033 (prima visible) + casos del puente JUBILADO (no doble conteo).
- Tests de migración Room (con schema exportado al repo).
- Test del parser C2C: fixture construido con una respuesta REAL capturada en etapa 2 con el
  propietario, sustituyendo antes los montos por cifras de mentira (privacidad).
- Tests de red con MockWebServer (timeout/429/401) → verificar los mensajes quirúrgicos de §9
  (los códigos reales del C2C se confirman en cuenta real — F1).
- Test del parser de TASAS con el JSON EN VIVO verificado (F2), probando ambos prefijos de TZ
  (−04:00 y Z — hallazgo A3 #8).
- Test del respaldo VERDAD (§7): exportar → destruir → restaurar en baúl de prueba → comparar.
- Test de avisos bajo restricciones reales (battery saver + manual OEM §8): documentar lo que
  pasó DE VERDAD en cada caso (vara del ROADMAP: PASS solo si se ejecutó).

## 11. LO QUE ESTE DOC NO DECIDE

- Nombres de pantallas, colores exactos y vocabulario visual vigente «Cartera de piedra y tinta» → DESIGN.md.
- Casos canónicos y varas por etapa → TESTING.md.
- Qué se publica y cuándo → ROADMAP (hitos) + autorización del propietario por ruta.
- Comportamientos de producto aún no canónicos que tocaron: borrado de filas (ABIERTO), PIN/
  biometría optativa (ABIERTO), umbral de aviso de export (propuesta), cuándo lee Binance
  (ABIERTO fino del ROADMAP — la propuesta está en §4.4), cifrado del respaldo (ABIERTO).
  Cada uno con su puerta y su etiqueta.

## 12. HUECOS TÉCNICOS POR VERIFICAR (heredados + nuevos, todos con puerta)

- ¿Cuál variante del endpoint C2C está viva en 2026 (orderMatch vs trading-order)? [cuenta
  real, guiado — F1] (añadido por A3 #1 — crítico de trazabilidad que faltó en v1.0).
- Shape/weight/ventana histórica exacta del C2C y el límite hacia atrás [cuenta real — F1].
- ¿"Enable Reading" basta para el C2C o pide toggle adicional? [cuenta real — F1].
- Toggles exactos de la creación de llave en 2026 [cuenta real — F1].
- Endpoint de time-sync (firma) y códigos de error reales del C2C [cuenta real — F1 §4.1/4.5].
- minSdk del teléfono del propietario (probar) [teléfono real].
- Comportamiento de `SCHEDULE_EXACT_ALARM` en Android 14+ del teléfono del propietario
  (toggle del sistema) [teléfono real — §8].
- Detalles Tink/Keystore al implementar (keyset y su rotación si toca) [al construir — F5].
- Pasos reales de invitación del closed testing de Play + verificación de identidad de la
  cuenta nueva (desde Sept 2026 según la fuente — F7) [M5].
- Comportamiento del WorkManager diario en el OEM del teléfono del propietario [al construir — F4].
- Vico: investigación propia si DESIGN pide gráficas [pendiente — §1].

## APÉNDICE A. RESUMEN ILUSTRADO DE LAS DECISIONS D-042..D-052 (para la trazabilidad del
constructor; texto completo en decisions-ledger.md)

- D-042: Mercantil es el atracadero final de TODO el dinero; "otra plata que entra" (Bs de
  terceros) se registra con motivo, suma a saldo e ingresos, fecha editable; el cuadre
  valida lo que la app no ve.
- D-043: cada bolso (tanda) = entidad con nombre, rondas y cuota por ronda (fija/variable);
  varios bolsos a la vez; su deuda vive en deudas con avisos.
- D-044: Bs en mano se valúa a BCV y RESPIRA (re-valúa con cada BCV nueva); gastos/deudas
  NO se re-valúan (tasa de su momento); sin fuente vigente → última BCV CON SU HORA + aviso.
- D-045: regla de oro de la ruta (número que no miente antes de la etapa siguiente) +
  proceso de documentos (volumen completo + pruebas robustas + volver solo cuando esté listo).
- D-046: registro tipado "venta P2P a mano" (Bs netos + tasa + USDT, prima visible) COMO
  PUENTE de la etapa 1; se jubila al conectar la API; queda como respaldo.
- D-047: visto del propietario: ROADMAP v1.1 estable.
- D-048: mandato de investigación exhaustiva + paquete completo; decisiones técnicas por
  delegación del Director, supervisables.
- D-049: stack electo (§1).
- D-050: llave guardada con Keystore (AES-GCM) + DataStore; material no exportable; hardware
  condicionado a dispositivo/algoritmo y comprobación con `KeyInfo`; el JSON manual excluye la
  llave y el respaldo del sistema requiere las reglas y prueba de §6.
- D-051: pre-llenado de tasa con espejo + confirmación humana (no automático puro).
- D-052: reconstrucción C2C por ventanas máximas de 30 días/llamada, encadenadas hacia atrás,
  dedup por orderNumber.

FIN — TECH.md v1.2 CANÓNICO (D-059; ensayo aislado 18/18, 2026-10-21). Estado del documento:
CANÓNICO; la implementación sigue pendiente y se valida con las pruebas de TESTING.
