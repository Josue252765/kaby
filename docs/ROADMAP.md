# ROADMAP.md — Kaby (hoja de ruta)

- **Estado:** v1.2 — ESTABLE (visto del propietario — D-047; sincronización de estados D-058).
  Volumen completo + auditorías (ronda 4: 3 auditores; asaltos 1-2 del enjambre D-053 con
  cicatrices aplicadas y verificadas) + ENSAYO AISLADO SUPERADO (banco 18/18, 2026-10-21;
  revisión best-effort, NO certificada). El ABIERTO
  "venta P2P a mano" quedó RESUELTO con D-046 (el puente → casa única). Pedazo 1 se aprobó
  en chat justo antes de la instrucción "volumen completo + pruebas".
- **Fecha:** 2026-10-18
- **Autor:** Director de Documentación (a partir de D-030 y PRODUCT.md v1.2 — CANÓNICO).
- **Publicación:** D-059 registra la autorización del propietario y la publicación de este canon en `docs/` el 2026-10-21. Cualquier publicación o copia futura fuera de esta ruta requiere autorización explícita por ruta.
- **Convención:** [CONFIRMADO] = aprobado por el propietario (con su referencia); [POR VERIFICAR] = se comprueba en la fase técnica; [ABIERTO] = pendiente de decidir con el propietario; [DERIVADO] = lo deduce el Director de decisiones vigentes; (pN) = respuesta N de la entrevista.

## 1. Qué es esta hoja de ruta

Para el lector fresco: **Kaby** es la app de finanzas personales de UNA persona —
Android, en Venezuela, Bs y USDT — y este es su orden de construcción. Todo el detalle
del producto vive en PRODUCT.md (canónico). Se asume UN teléfono y UNA persona
[DERIVADO de D-017]; varios dispositivos quedan como puerta futura.

La ruta adoptada [CONFIRMADO D-030] es de **5 etapas**, ordenadas para que cada una termine
con algo **USABLE** — nunca a medias. No hay plazos decididos: la ruta **ordena**, no
calendariza [ABIERTO: ritmo].

**La regla de oro** [CONFIRMADO D-045]: cada etapa deja funcionando un **"número que no
miente"** antes de abrir la siguiente. Si una etapa rompe el número, se arregla primero —
no se avanza. Y al final de cada hito se re-corre lo de las etapas anteriores
(regresión — con método y registro definidos en el hito 10), porque la prueba de verdad
es que TODO pase: todos los criterios, no solo lo nuevo; el cuadre del saldo es el
ejemplo maestro de "cuadra". [DERIVADO de D-045]

**Proceso de este documento** (instrucción del propietario, p51): pedazo 1 se aprobó en
chat; el volumen completo se escribió de una vez y se le aplican **pruebas robustas**
(auditoría del Director — la vara es dura: best-effort, **NO certificada**) antes de
volver al propietario.

## 2. Las 5 etapas (vistas de arriba) [CONFIRMADO D-030]

1. **El cuaderno** — registro de gastos + cálculo base (el ancla, el cuadre,
   "cuánto tengo / cuánto gasté"; incluye "otra plata que entra", D-042).
2. **La conexión Binance** — leer, organizar y guardar vía API solo-lectura.
3. **La telemetría** — panel con los últimos 3 P2P, USDT movidos y equivalencias.
4. **Las deudas** — sección completa (deudas normales + bolsos como entidades — bolso
   = tanda/grupo de aportes por turnos; D-043).
5. **El acabado** — pulido visual de la identidad vigente «Cartera de piedra y tinta» + Google Play privada.

## 3. Trámites en paralelo (desde ya; no dependen de programar)

- **Cuenta de desarrollador de Google Play:** se inicia temprano porque los trámites
  tardan; ahí se verifican los requisitos de la Play privada y la disponibilidad del
  nombre "Kaby". [D-020/D-028 — POR VERIFICAR]
- **Llave de Binance SOLO-LECTURA:** la crea el propietario guiado cuando se acerque la
  etapa 2. La llave NUNCA vive en el código ni en este documento.
  [DERIVADO de D-013; mecanismo fino de custodia: TECH.md — POR VERIFICAR]
- **Mapa trámite→hito:** la cuenta de Play SOLO bloquea el criterio de distribución de
  la etapa 5; la llave, SOLO la etapa 2. Ningún trámite demorado cancela etapas
  previas; plan B de distribución: APK firmado instalado a mano — que es TAMBIÉN
  cómo corre la app en las etapas 1-4 (la Play privada llega al final).
  [DERIVADO técnico; los trámites son del propietario]

## 4. Guardias que cruzan TODAS las etapas (encendidas desde la etapa 1)

- **Cuenta firme [D-037]:** nada se borra en silencio; marca de frescura siempre visible;
  cuadre; día a día = "calculado", cuadrado = "firme".
- **Respaldo [D-038] desde el día uno:** intento de copia local diaria de mejor esfuerzo + archivo exportable.
  Los datos existen desde la etapa 1 — una app que no respalda no guarda nada.
  [DERIVADO de D-038: aplica desde que hay datos, no solo al final]
- **Errores quirúrgicos [D-041]:** a cada etapa se arma su TESTING.md ANTES de
  programarla; si algo falla, falla SOLO esa pieza — nunca derriba lo demás.
  [la cadencia "por etapa": DERIVADO de D-041]
- **Sellos de tiempo [D-040 — CONFIRMED por delegación D-048, formal en TECH]:** cada dato
  lleva su hora (ocurrido / capturado); lo externo nunca se disfraza de fresco.
  [la sustancia en firme: D-037/D-041]
- **Datos y llave protegidos:** la regla fina vive en DATA-PRIVACIDAD (doc del paquete,
  hito M0); el ROADMAP la cita, no la repite.
- **Delegación supervisada [D-029 + D-048]:** el Director decide lo técnico; el propietario
  supervisa y puede vetar.

## 5. Etapa 1 — "El cuaderno"

### Alcance
- El ancla inicial del Mercantil + el cuadre con estados definidos: "sin ancla"
  ("cuánto tengo" = "—" con su explicación; los gastos se guardan igual), "calculado",
  "descuadre abierto" con su cifra. [D-032/D-037]
- Registrar gasto: qué / cuándo (hoy, editable hacia atrás) / cuánto / moneda (Bs o
  USDT) / tasa del momento si es Bs / nota / tipo. [D-016/D-025/D-036]
- Registrar **"otra plata que entra"** (Bs de un tercero): de quién + por qué + cuánto +
  cuándo (hoy; editable hacia atrás — diseño D-037) → suma al saldo y a ingresos.
  [D-042]
- Cálculo base: "cuánto tengo" (se ve en dólares y su equivalente en bolívares), "cuánto gasté" (desglose provisional,
  tasa guardada por movimiento); prima P2P con signo — la diferencia por vender por
  encima del BCV — solo cuando una venta se conozca con su tasa: a mano en esta etapa
  [ABIERTO: registro tipado de venta, abajo] o automática desde la etapa 2.
- Registro tipado **"venta P2P a mano"** [CONFIRMADO D-046]: los Bs que llegan netos +
  la tasa + la cantidad de USDT; suma al saldo; prima visible. Es el registro de las
  ventas PROPIAS SOLO durante el puente (etapas 1→2): se jubila cuando la API entra
  (etapa 2) y queda como respaldo si la lectura fallara.
- **BCV a mano con su hora + aviso** (mientras no haya fuente garantizada — D-034; BCV
  = Banco Central de Venezuela, la tasa oficial). Es la referencia en pantalla [D-008]
  y el valuador del Bs en mano, que se re-valúa cuando el BCV se actualiza [D-044].
  La fuente automática del BCV (candidata ve.dolarapi.com — no oficial) es POR
  VERIFICAR en la fase técnica [D-010/D-006]: hasta que exista, el "respira" vive de
  que el propietario teclee el BCV de vez en cuando.
- **Respaldo vivo:** intento de copia local diaria de mejor esfuerzo + exportable; al importar, avisa si el archivo es más
  viejo que los datos del teléfono. [D-038]

### Criterio de terminado (lo que el propietario puede probar)
1. La primera pantalla pide el ancla — el ancla es TU saldo real del Mercantil anotado
   una vez como punto de partida — en una pantalla corta que NO bloquea la app (los
   estados "sin ancla" funcionan desde el día uno). Hecho el ancla, "cuánto tengo"
   muestra el número calculado. [D-032/D-037]
2. Registro 3 gastos reales de una tarde; "cuánto gasté" los suma cada uno con su tasa.
3. Registro una entrada de tercero ("otra plata que entra") y el saldo sube.
4. El cuadre coincide (o muestra la diferencia exacta; el cierre guiado no borra nada
   en silencio). [D-032/D-037]
5. Apago y abro al día siguiente: los datos confirmados en Room siguen disponibles. La copia
   local diaria se intenta en segundo plano (WorkManager es inexacto); cuando existe una copia,
   se restaura en baúl y coincide número contra número con los datos vivos. Si está corrupta,
   se avisa y se conserva la buena anterior. El exportable
   sale y vuelve con el mismo estándar. [D-038; procedimiento fino: DATA-PRIVACIDAD]
6. Si algo falla en cualquier pieza, falla SOLO esa pieza y el resto sigue. [D-041]

### Qué NO cabe aquí (y cómo se vive la falta mientras tanto)
- Leer Binance. **Nota honesta:** entre etapas 1 y 2, una venta P2P propia no se ve
  sola — el cuadre la caza como cualquier entrada invisible y la cierra por el camino
  guiado (el cierre guiado = la conversación de cerrar una diferencia: registrar lo
  olvidado o un ajuste con su nota; D-032/D-037); el registro tipado "venta P2P a
  mano" (arriba, CONFIRMADO D-046) le da casa propia. Y declarado en pantalla: hasta la
  etapa 2, la cara USDT de "cuánto tengo" es ciega — el número que esta etapa sella es
  el de bolívares: parcial, y lo dice. Y para que no quepa duda: NADIE propone que el
  DESTINO final sea a mano — la verdad del número es la API de la etapa 2; el registro
  manual, si el propietario lo aprueba, es SOLO el puente, y se jubila con la API
  (queda como respaldo si un día fallara la conexión).
- Panel de telemetría (etapa 3), deudas/bolso (etapa 4), acabado visual y Play (etapa 5).
- Cuentas, nube o servidores: NUNCA sin permiso explícito. [D-038]

### Verificación
El criterio de terminado se corre completo; los casos finos de error (los canónicos, los
históricos y los verificables) los formaliza TESTING.md **antes** de escribir código.
[D-041; auditoría = best-effort, no certificada]

## 6. Etapa 2 — "La conexión Binance"

**Actualización 2026-10-21 — autorización directa:** el propietario pide implementar extracción Binance y remodelar Ajustes antes del sello M1. Se autoriza esa construcción, sin declarar M1 ni M2 aprobados. Primer incremento: consulta manual con credenciales transitorias, historial SELL/COMPLETED/USDT/VES local y saldo disponible de Funding identificado por su alcance. La lectura automática y la custodia persistente siguen pendientes; TECH §6 exige primero probar backup/D2D en el teléfono. La conciliación bancaria confirma el neto y vincula cada puente manual antes de jubilarlo: no se retiran ventas en bloque ni se presume que `totalPrice` sea el neto Mercantil. Detalle y evidencia en `INTEGRACION-BINANCE.md`.


### Alcance
- **Aquí entra la verdad por API (D-013).** Si el puente manual de la etapa 1 existió,
  se jubila: la API trae el historial P2P REAL (hasta ~6 meses hacia atrás, según la
  ventana — POR VERIFICAR) y desde esta etapa TODO entra solo. El registro manual
  queda documentado como respaldo, por si un día la lectura fallara.
- Llave SOLO-LECTURA creada por el propietario guiado (trámite paralelo); la llave nunca
  está en el código. [D-013] Y la llave SOLO-LECTURA BASTA para todo esto: leer de
  Binance y escribir en la base local son casas distintas — la base es el archivo
  privado de la app en el teléfono y no le pide permiso a nadie. La llave NUNCA tendrá
  permisos de escritura, retiro ni operación: menos permisos, menos riesgo.
  [DERIVADO de D-013; detalles del endpoint C2C con la cuenta real: POR VERIFICAR]
- Leer el historial de operaciones P2P (C2C): **unitPrice** (la tasa de la operación) +
  **totalPrice** (Bs totales) + quantity (USDT), respetando la ventana que la API
  permita (~6 meses: hipótesis sujeta a fuentes — por confirmar la ventana exacta, y
  los endpoints exactos — incluido el de balances para el saldo USDT — con la cuenta
  real y supervisión del propietario). [D-013 — POR VERIFICAR]
- Leer el saldo USDT; guardar TODO local con sellos de tiempo; nada disfrazado de
  fresco. [D-040 — CONFIRMED por delegación D-048; sustancia: D-037/D-041]
- El "cuánto tengo" deja de depender de reportar ventas a mano: los P2P entran solos,
  con su hora y su tasa; el "neto" por operación queda POR VERIFICAR con la cuenta real
  [D-033].
- Cuándo leer (al abrir la app, cada X horas, botón): [ABIERTO fino]. Lo que la ruta
  exige mientras tanto: NUNCA disfrazar frescura y que un fallo de lectura no derribe
  nada.
- Regla de transición si la ventana real resultara menor que el ancla: el patrón
  manual queda documentado de respaldo y el cuadre lista ese hueco POR SU NOMBRE
  ("por esa fecha no hay lectura Binance") — causa conocida, no error del propietario.
  [DERIVADO de D-037; ventana: POR VERIFICAR]

### Criterio de terminado
1. Con la llave puesta, el historial P2P REAL aparece con sus tasas (unitPrice) y sus
   totales (totalPrice) guardados localmente. [D-013]
2. El saldo USDT se ve con su hora de lectura.
3. Una venta hecha después de conectar aparece sola (sin meterla a mano) la próxima vez
   que la app lea.
4. Si la API no responde, la app trabaja con lo que ya tiene local y AVISA con su
   hora — nunca finge frescura. [sellos D-040 arriba; aislamiento D-041]
5. Un fallo de conexión NO toca cuaderno, gastos ni respaldo. [D-041]

### Qué NO cabe aquí
- Operar en Binance (comprar/vender desde la app): la app SOLO LEE. [D-013]
- Panel visual (etapa 3), tasas paralelas de otros mercados (fuera de la v1),
  integraciones nuevas.

### Verificación
Con la llave real y supervisión del propietario; endpoints y ventanas exactas se
confirman ahí mismo (es el bloque POR VERIFICAR grande de la fase técnica).

## 7. Etapa 3 — "La telemetría"

### Alcance
- Panel: **últimos 3 P2P** con su cantidad y hora + **resumen de USDT movidos
  históricamente** [D-019] + **equivalencias** claras $↔Bs + **las cifras clave del
  panel** que PRODUCT §6 pone en la telemetría: número real, cuánto gasté, ganancia
  (su presentación fina está ABIERTA en PRODUCT §12). En etapa 3 el "número real" es
  cuánto tengo. [D-030 + D-019/D-032/D-033]
- Datos frescos con su sello; datos viejos con su aviso. [D-040 — PROPOSED en
  delegación; sustancia: D-037/D-041]
- (La tarjeta de "próximo pago" de deudas pertenece a la etapa 4, cuando existan
  deudas.) [D-024]

### Criterio de terminado
1. Abro la app: al primer vistazo veo los 3 últimos P2P (monto, tasa, hora) y el USDT
   movido — sin navegar.
2. Todo lo que muestra el panel coincide con los datos crudos de la etapa 2 (número
   contra número, sin retoques).
3. Los datos viejos se declaran viejos (su aviso, su hora). [D-040]
4. Con la app recién conectada y el panel casi vacío, la pantalla dice qué falta y
   cómo se llena — texto de instrucción, no un error.

### Qué NO cabe aquí
- Gráficas diarias o cierres mes a mes [ABIERTO: presentación fina del resumen — ya
  está en la lista de ABIERTAS de PRODUCT.md].
- Avisos de deudas (etapa 4), acabado visual (etapa 5).

### Verificación
Comparación directa panel ↔ historial crudo en el teléfono del propietario.

## 8. Etapa 4 — "Las deudas"

### Alcance
- Sección completa de deudas [D-024]: pestañas Pendientes/Saldadas, historial completo,
  fechas de pago al registrar, avisos **locales** sin internet (con ojo técnico: Android
  puede atrasarlos si el teléfono ahorra batería — POR VERIFICAR), "pago confirmado"
  baja el saldo, orden por próximo pago, tarjeta de próximo pago en la telemetría.
- Pagos de cualquier monto: "al día / parcial / atrasada"; el próximo pago solo avanza
  al cubrir la cuota; si el pago cambia de moneda, se pide y guarda la tasa de ese pago.
  [diseño del Director bajo D-024/D-036/D-041]
- **Ente** — a quién se le debe: comercio (negocio) o civil (persona, familia);
  moneda configurable por caso. [D-036/D-024]
- **Bolsos como entidades [D-043]:** nombre, rondas, cuota fija o variable, varios a la
  vez; lo pendiente de aportar vive en "lo que debes" con avisos; el turno recibido es
  "plata que vuelve" — suma a tu plata, NO a ingresos. [D-032/D-035]

### Criterio de terminado
1. Registro una deuda con cuotas y fecha: aparece en Pendientes, ordenada por urgencia,
   con su ente y su moneda. [D-024/D-036]
2. Un pago parcial la deja "parcial" y el próximo pago conserva su fecha y su orden
   (baja solo cuando la cuota queda cubierta). [diseño D-024]
3. Registro un bolso: sus aportes pendientes suben "lo que debes"; cuando toca el turno,
   la entrada suma a tu plata como "plata que vuelve". [D-032/D-035/D-043]
4. Dos bolsos a la vez NO se cruzan: cada uno su deuda, su ronda, su aviso. [D-043]
5. El aviso suena sin internet (con la salvedad honesta de la batería — POR VERIFICAR).
   [D-024]
6. Fallo de avisos o de la sección de deudas no toca el cuaderno ni Binance. [D-041]

### Qué NO cabe aquí
- Recordatorios por redes/servicios externos: los avisos son LOCALES. [D-024]
- Cálculo automático de intereses comerciales: [ABIERTO: si aplica en los casos reales
  del propietario — se pregunta si hace falta].

### Verificación
Casos reales del propietario (una deuda conocida + un bolso conocido) contra la app;
los avisos se dejan durmiendo el teléfono (ahorro de batería) para ver si llegan.

## 9. Etapa 5 — "El acabado"

### Alcance
- Identidad vigente **«Cartera de piedra y tinta»**: paleta mineral clara, texto tinta,
  acentos sobrios teal y terracota, información financiera ordenada por procedencia,
  tarjetas delimitadas y movimiento breve. El propietario seleccionó esta dirección el
  2026-10-22 para el rediseño; la especificación está en DESIGN.md y PRODUCT.md §11.
  La dirección «café de noche» [D-026/D-027] queda como antecedente histórico. La
  aceptación visual de todas las pantallas sigue pendiente de prueba nativa.
- **Google Play privada [D-020]:** distribución cerrada; requisitos POR VERIFICAR con el
  trámite paralelo; nombre "Kaby" verificado [D-028]; instalación sana en el teléfono.

### Criterio de terminado
1. La identidad se ve en TODAS las pantallas — y **no rompe funciones**: se re-corre el
   criterio de terminado de las etapas 1-4 (regla de oro, D-045).
2. La identidad cumple su checklist en cada pantalla — textura de papel, acentos
   sobrios, tipografías, sellos — y EL PROPIETARIO la aprueba: la vara es la decisión
   D-026/D-027 + su gusto, declarado como puerta de cierre.
3. La app instala y arranca desde la Play privada, y el teléfono NO la marca como
   sospechosa. [objetivo D-020; requisitos POR VERIFICAR]

### Qué NO cabe aquí
- Temas o personalizaciones (una sola identidad v1). [D-027]
- Cualquier cosa que pida cuentas o nube. [D-038]

### Verificación
Las pruebas de las etapas 1-4 se vuelven a correr con el acabado puesto (regresión);
la distribución privada se prueba end-to-end: subir → instalar → arrancar.

## 10. Hitos de verificación (el collar que ordena la ruta)

| Hito | Qué dice que ocurrió | Permite avanzar |
|---|---|---|
| **M0 — Casa en orden** | Paquete de 8 documentos canónicos con el mínimo verificable de etapa 1: 1) PRODUCT, 2) ROADMAP, 3) CONVENTIONS, 4) DESIGN (pantallas de etapa 1), 5) TECH (SQLite y arquitectura adaptable), 6) DATA-PRIVACIDAD (respaldo y custodia), 7) TESTING (casos de etapa 1), 8) BUILD-RUNBOOK. Documentar ANTES de programar [D-041]; el resto de cada documento se completa por etapa [DERIVADO del proceso del Director, bajo D-029]. D-059 registra que los ocho documentos se canonicalizaron y publicaron en `docs/` el 2026-10-21. | Sin M0 no hay M1; un documento en retrabajo bloquea solo lo que depende de él |
| **M1 — El cuaderno** | Criterio de terminado de la etapa 1 pasado completo (regresión incluida) | Abre etapa 2; la llave Binance ya está en su trámite (no es condición del hito) |
| **M2 — Binance conectada** | Etapa 2 pasada con llave real; historial P2P + saldo en local | Abre etapa 3 |
| **M3 — Telemetría** | Etapa 3 pasada; panel = datos crudos | Abre etapa 4 |
| **M4 — Deudas y bolsos** | Etapa 4 pasada con casos reales del propietario | Abre etapa 5 |
| **M5 — El acabado** | Identidad puesta + regresión completa 1-4 + salida pública | TRES caminos SIN saltar el sello (D-045 — corregido tras asalto B): (a) Play end-to-end → sello M5 normal; (b) trámite de Play demorado → se sella M5a "APK firmado privado" con SUS casillas, y las casillas de Play quedan BLOCKED(causa trámite del propietario) para el M5b-Play — JAMÁS un sello con BLOCKED abierto | LISTA PRODUCTO v1 |

Cada hito exige su **regresión**: los criterios de terminado de TODAS las etapas previas
se re-corren. El "número que no miente" es el juez de cada paso [D-045]; y por etapa,
el número es CONCRETO:

- Etapa 1: el cuadre del Mercantil coincide número contra número — con sus cegueras
  declaradas (sin Binance, cara USDT pendiente).
- Etapa 2: lo que la cuenta real muestra en Binance = lo guardado localmente.
- Etapa 3: el panel = los datos crudos, número contra número.
- Etapa 4: "lo que debes" (deudas + bolsos) = la suma de los casos reales del
  propietario.
- Etapa 5: TODO lo anterior, con el acabado puesto. [DERIVADO de D-032/D-037/D-045]

Cómo se corre una regresión: TESTING.md entrega por etapa una lista de casillas (una
por criterio); el propietario las corre EN SU TELÉFONO con casos reales; fecha y
resultado quedan en **el registro de hitos** — acta por hito (fecha, resultado por
criterio, notas), un archivo aparte del proyecto cuyo formato fija CONVENTIONS.
[DERIVADO; TESTING.md lo formaliza — D-041]

## 11. Si algo sale mal (el plan, no el deseo)

- **Nada derriba lo demás [D-041]:** la pieza que falla se degrada sola; el resto de la
  app sigue viva y el aviso es honesto.
- **Paso bloqueado, no escondido [D-045]:** si un hito no pasa su verificación, se marca
  FALLA en el registro y no se abre la siguiente etapa hasta arreglarlo.
- **Sin plazos falsos:** la ruta no promete fechas [ABIERTO: ritmo — lo decide el
  propietario]. Mejor un ancla muerta que una promesa mentirosa.
- **Lo técnico se resuelve con supervisión [D-029]:** el Director propone lo técnico,
  el propietario supervisa; los trámites (llave, cuenta dev) son del propietario.
- **Re-apertura [DERIVADO de D-045/D-041]:** un cambio que toque una etapa YA cerrada
  es un mini-ciclo de esa etapa: cambio + regresión desde esa etapa hasta la actual +
  re-sellado del hito en el registro, con visto del propietario. Nada entra en
  silencio.
- **Cadencia de pruebas [DERIVADO de D-041]:** antes de programar cada etapa se arma
  su TESTING.md (semillas: los criterios de terminado de este doc).

## 12. Huecos ABIERTOS de la ruta

- **Quién programa la construcción y con qué herramientas:** fase técnica, bajo la
  delegación supervisada de D-029. [ABIERTO]
- **Ritmo/plazos.** [ABIERTO — revisados arriba]
- **Ventanas y límites exactos de la API de Binance** (incluido el endpoint de balances
  para el saldo USDT) y el "neto" del P2P: cuenta real. [POR VERIFICAR]
- **La fuente del BCV** (candidata ve.dolarapi.com — no oficial; intento diario:
  D-006): fase técnica. [POR VERIFICAR]
- **Cuándo lee Binance** (al abrir, cada X horas, botón). [ABIERTO fino]
- **Restauración completa en un teléfono nuevo** (y qué pasa con la llave al cambiar
  de equipo): DATA-PRIVACIDAD + la prueba del criterio 5 de la etapa 1. [VINCULADO
  D-038]
- **Requisitos de la Play privada y del nombre "Kaby":** con el trámite en paralelo.
  [POR VERIFICAR]
- **Formalización de casos de prueba por etapa:** TESTING.md (se escribe ANTES de
  programar cada etapa) — los criterios de terminado de este doc son sus semillas.
  [VINCULADO a D-041]
