# PRODUCT.md — Kaby (documento madre)

- **Estado:** v1.2 — CANÓNICO: asaltos 1-2 del enjambre superados y ENSAYO AISLADO superado (banco ejecutable 18/18, 2026-10-21; best-effort, no certificada), correcciones aplicadas, canon ratificado por el propietario vía mandato D-059. Quedan ABIERTAS finas listadas en §12.
- **Fecha:** 2026-10-18 (canónico v1.2) · actualizado el 2026-10-19 tras el enjambre D-053
  (sincronización de estados/fuente — D-058; contenido canónico SIN cambios)
- **Autor:** Director de Documentación (a partir de la entrevista y las decisiones D-001..D-048).
- **Publicación:** D-059 registra la autorización del propietario y la publicación de este canon en `docs/` el 2026-10-21. Cualquier publicación o copia futura fuera de esta ruta requiere autorización explícita por ruta.
- **Convención:** [CONFIRMADO] = aprobado por el propietario (con su referencia); [POR VERIFICAR] = se comprueba en la fase técnica; [ABIERTO] = pendiente de decidir con el propietario; (pN) = respuesta N de la entrevista.
- **Plan de revisión:** pedazos 1-4 revisados (1 y 3 aprobados pleno; 2 cerrado vía decisiones; 4 condicional → ronda 2 → canon).

## 1. Qué es Kaby (en una frase)

Kaby es una aplicación de finanzas personales para una sola persona: su dueño. Su trabajo
es responder tres preguntas que hoy no tienen respuesta — cuánto tuve en realidad, cuánto
gasté y cuál fue mi ganancia neta comparada con mis gastos — en un país donde conviven
bolívares, dólares y USDT, todos con tasas que se mueven a diario.

## 2. El problema

1. **La devaluación borra los registros.** Un gasto anotado en bolívares "pierde sentido"
   al día siguiente. Cualquier registro que no guarde el valor del momento nace viejo.
   (entrevista, 2026-10-16)
2. **Hoy no existe registro alguno** ("no tengo donde anotarlo"; entrevista). La app
   compite contra "no anotar": si anotar cuesta más de unos segundos, no se usará.
   [CONFIRMADO]
3. **La plata no está quieta.** Entra como USDT a Binance, se convierte a bolívares por
   P2P, vive en Mercantil, se gasta en la calle y en compras por internet. (p3-p5)
4. **Resultado:** "pierdo la cuenta". No sabe cuánto tuvo, cuánto gastó, ni cuál fue su
   ganancia neta real.

## 3. Para quién es

- **Un solo usuario: el propietario.** Sin cuentas ni multiusuario ("para mí por ahora
  single owner"). [CONFIRMADO D-017]
- **Su vida del dinero, así como es hoy** [CONFIRMADO; detalles p3, p4, p5, p25, p26]:
  - Cobra su trabajo por internet en USDT, que llegan a Binance (red BNB Smart Chain).
  - "Apenas llegan a Binance los envío a bolívares": los vende por P2P.
  - El día a día se paga SOLO en bolívares; el USDT SOLO para compras por internet.
  - A veces "suelta" USDT antes de retirar (gastos hormiga) y, de vez en cuando, paga
    cuotas o transferencias a terceros. [CONFIRMADO D-036: la moneda es de cada registro,
    configurable caso por caso]
- **No maneja** efectivo, ni otras cuentas, ni Zelle. ("solo de ahí") [CONFIRMADO; p12]

## 4. El mapa del dinero

- **Mercantil (bolívares):** donde vive la plata de verdad — y el ÚLTIMO sitio donde cae
  TODO el dinero, de cualquier proveniencia; de ahí sale cuando lo gastas (salvo los
  gastos hormiga, que salen de Binance en USDT antes de retirar). [CONFIRMADO D-042]
- **Binance (USDT):** donde la plata transita. La app lo lee (API solo-lectura, llave del
  propietario) y **guarda su propio historial en el teléfono**: si la lectura falla algún
  día, lo ya guardado no se pierde. [CONFIRMADO D-013; limitaciones de la API: POR
  VERIFICAR con la cuenta real]
- **P2P:** la puerta entre los dos mundos. Se vende USDT y llegan bolívares, a la tasa del
  anunciante que se elija (por operación). Los bolívares llegan netos. [POR VERIFICAR el
  "neto" con la cuenta real]
- **Otra plata que entra (Bs):** cuando un tercero te paga directo a Mercantil (cliente
  local, reintegro, familia), se registra con su motivo — de quién y por qué; suma al
  saldo y a ingresos, con fecha editable hacia atrás. [CONFIRMADO D-042; diseño D-037]
- **BCV:** la tasa oficial. En la app, es la referencia para pasar $↔Bs en pantalla
  [CONFIRMADO D-008]; según el propietario, los comercios aplican esta tasa al cobrar
  (entrevista p13); y es además el valuador del Bs en mano [D-044].
- **Bolso:** grupo que aporta cuotas; la plata se junta y por turnos cada quien recibe el
  monto completo (estilo susu/tanda — el giro: aportes por turnos). Cuando toca el turno: se registra la entrada con
  tipo "plata que vuelve" — suma a tu plata, NO a tus ingresos ni a tu ganancia.
  Y si te toca recibir ANTES de haber aportado todo, los aportes que te quedan pendientes
  cuentan como deuda en "lo que debes": así el recaudo no parece plata tuya.
  [CONFIRMADO D-032/D-035/D-043: cada bolso es una entidad con nombre, rondas y cuota por
  ronda (fija o variable); pueden convivir varios bolsos a la vez; su deuda pendiente
  vive en la sección de deudas, con avisos como cualquier otra]
- **Deudas y cuotas:** se registran; cada "pago confirmado" se registra con su tipo y baja
  el saldo de la deuda. Cada deuda identifica a su ente (a quién le debes: comercio o
  civil) y su moneda se configura caso por caso (Bs o USDT). [CONFIRMADO D-024/D-036]

## 5. Las tres preguntas clave (el corazón del producto)

**1) Tu número real** [CONFIRMADO D-032]:
= lo que tienes (Mercantil + Binance) − lo que debes (deudas pendientes, incluidos los
aportes del bolso que aún no pagas — D-035).
- "Lo que debes" se pasa a dólares: cada deuda lleva su moneda configurable (según el
  ente: comercio o civil) y su tasa del momento si es Bs. [CONFIRMADO D-036]
- **"Cuánto tengo", al día y casi sin trabajo:** la entrada de Bs se lee SIN abrir el
  banco — se infiere de las ventas P2P que registra Binance; lo que sale (gastos) lo
  anota el propietario a mano, porque eso no se puede leer. La plata de terceros entra
  por su registro propio "otra plata que entra" [D-042], y el cuadre valida que TODO
  cuadre — lo que la app no ve, el cuadre lo caza. [CONFIRMADO D-033/D-042; el "neto"
  del dato de Bs: POR VERIFICAR con la cuenta real]
- **Valuación del Bs en mano:** tu saldo en bolívares aparece en dólares valorado a BCV —
  y RESPIRA: se re-valúa conforme se actualiza el BCV (si sube, tu saldo en dólares sube
  aunque no toques nada; en pantalla se ve con qué tasa y su hora). Los gastos y las
  deudas NO se re-valúan: guardan la tasa de su momento. [CONFIRMADO D-044; sin fuente
  vigente de BCV, se usa la última guardada CON SU HORA y su aviso — D-034/D-040]
- **LA CUENTA FIRME (D-037, principio del propietario):** "una cuenta real y firme, sin
  mentiras ni datos inflados — crucial, cubierta de forma robusta". Así la cubre la app:
  1. **El ancla:** al empezar, el saldo real de Mercantil una vez, punto de partida. La
     primera pantalla lo pide sin agobio, y hasta que exista, "cuánto tengo" se muestra
     con "—" y su explicación — los gastos se guardan igual.
     [CONFIRMADO D-032; estado "sin ancla": diseño del Director bajo D-037]
  2. **El cuadre:** cuando el propietario quiera, en 5 segundos.
  3. **La marca de frescura:** "sin cuadrar hace X días" junto al saldo — discreta al
     principio, más notoria si pasan los días; nunca desaparece. Sin notificaciones extra
     por cuadre (esas quedan solo para las deudas).
  4. **Al detectar un descuadre:** la app muestra la diferencia clara y guía el cierre:
     registrar el gasto olvidado o un ajuste con su nota. Mientras un descuadre quede
     abierto, la app muestra el estado "descuadre abierto" con su cifra a la vista.
     [diseño del Director bajo D-037/D-041]
  5. **Nada se borra en silencio:** todo ajuste queda en el historial; no existen botones
     de "desaparecer" descuadres.
  6. **Nada se disfraza:** el día a día se presenta como "calculado desde el ancla"; lo
     firme es lo cuadrado. La fecha del último cuadre siempre a la vista.
  [CONFIRMADO D-037 (principio, respuesta 44); el sistema lo diseñó el Director y se
  revisa con el propietario]

**2) Cuánto gasté** [CONFIRMADO D-016/D-022]:
= la suma de los gastos registrados, cada uno con la tasa de su momento, mostrada en
dólares con equivalencia en bolívares. Los totales se arman con la tasa guardada en cada
movimiento; el BCV es la referencia en pantalla [D-008], no la tasa de cada gasto.
- Se muestra desglosado por tipo: gasto del día / cuota o deuda / transferencias y pagos —
  porque una cuota no es lo mismo que un almuerzo (lo confirmado es D-022: cuotas y
  transferencias cuentan como gastos y se clasifican; el desglose concreto es provisional
  hasta cerrar la lista final de tipos [ABIERTO]).

**3) Ganancia neta comparada con los gastos** [DEFINICIÓN FINA ABIERTA]:
- **Ganancia neta** = ingresos − gastos (lo que sobró).
- **Cuánto se movió tu plata** = el cambio total de tu plata, que además incluye la
  **prima P2P** y la plata que vuelve.
- **Prima P2P:** vender USDT a una tasa mayor que la BCV entrega más bolívares de los que
  esa plata valdría a BCV; esa diferencia es prima — con signo: si vendes por debajo de
  la BCV, la prima sale negativa y se muestra igual [diseño del Director bajo D-037].
  (La simulación lo midió en un mes inventado: ~9,94 $ de prima dentro de +358,58 $ de
  cambio del monto total.)
- Los dos números conviven y la app muestra por qué difieren. [ABIERTO: cómo se presentan exactamente]

## 6. Cómo se usa (flujos principales)

- **Entrada:** abrir la app → animación de entrada → transición suave a la telemetría
  (panel) con el botón "registrar un gasto" y tarjetitas de información, cada una con su
  acceso directo. [CONFIRMADO D-027/D-015; detalle de tarjetitas: se afina]
- **Registrar un gasto:** qué (texto libre) + cuándo (automático = hoy; editable hacia
  atrás, para el gasto olvidado que sale al cuadrar — lleva su fecha verdadera) + cuánto +
  moneda (Bs o USDT) + tasa del momento (si es Bs) + nota; tipo con sugerencias y opción
  libre. [CONFIRMADO D-016/D-025/D-036; la fecha editable es diseño del Director para
  "nada se disfraza" (D-037/D-041), a revisión del propietario]
- **Registrar "otra plata que entra" (Bs de terceros):** de quién + por qué + cuánto +
  cuándo (hoy, editable hacia atrás) → suma al saldo y a ingresos. [CONFIRMADO D-042]
- **La tasa del momento** — REGLA PARA TODO REGISTRO con moneda extranjera (gasto, deuda,
  venta u "otra plata"; sacada de la sangría tras el asalto D para que nadie la lea como
  regla solo-de-la-plata-de-terceros): AUTOMÁTICA solo cuando exista una fuente que garantice
  el precio oficial BCV (hoy ninguna está confirmada — candidata ve.dolarapi.com, no
  oficial); mientras tanto, se escribe a mano o queda la última guardada CON SU HORA y
  su aviso. Cuando haya la fuente: la app propone la tasa del día y el propietario la
  corrige si el comercio usó otra; siempre editable. [D-034 CONFIRMADO con condición;
  la actualización muy frecuente es referencia de eficiencia del propietario, no
  requisito]
- **Ingresos:** todo lo de Binance se lee solo (API solo-lectura) y se guarda. El "cuánto
  tengo al día" (D-033) pende de esa lectura y del "neto" del P2P [POR VERIFICAR].
  [CONFIRMADO D-013]
- **Cuadre:** ancla una vez al empezar + cuando el propietario quiera (5 segundos); la
  marca de frescura siempre visible; si algo no cuadra, la app muestra la diferencia y
  guía el cierre, y ningún ajuste se borra en silencio. [CONFIRMADO D-032/D-037 — "la
  cuenta firme"; y ojo: el cuadre es del saldo de Mercantil, lo que la app no puede
  leer; Binance se confía a su lectura automática]
- **Deudas:** pestañas Pendientes/Saldadas, historial completo, fechas de pago pedidas al
  registrar, avisos locales (sin internet) — con ojo técnico: Android puede atrasar
  avisos si el teléfono ahorra batería [POR VERIFICAR] — "pago confirmado" baja el saldo,
  orden por próximo pago (lo más urgente arriba), tarjeta de próximo pago en la
  telemetría. Pagos de cualquier monto (parcial incluido): la deuda queda "al día /
  parcial / atrasada" y el próximo pago solo avanza cuando la cuota queda cubierta; si el
  pago va en otra moneda que la deuda, se pide y guarda la tasa de ese pago.
  [CONFIRMADO D-024; pagos parciales y conversión: diseño del Director bajo
  D-024/D-036/D-041]
- **Telemetría:** últimos 3 P2P con su cantidad + resumen general de USDT movidos
  históricamente + las cifras clave (tu número real, gastos, ganancias). [CONFIRMADO
  D-019 + D-032/D-033]

## 7. Funciones (lista v1.1 — se afina)

1. Registro de gastos de fricción mínima (texto libre + moneda + tasa del momento + nota).
2. Clasificación por tipo, con sugerencias y opción libre (compra online / deuda, cuota o
   bolso / transferencias y pagos).
3. Lectura automática de Binance (solo-lectura) con historial propio en el teléfono: la
   investigación del 17/10 (docs oficiales de Binance) arroja una ventana limitada de
   historial (~6 meses: hipótesis con fuente; por confirmar con la cuenta real la ventana
   exacta); guardar local permite mirar más atrás incluso si mañana la ventana cambia — la historia
   larga vive SOLO en el teléfono: si se pierde sin exportar, no se recupera.
   [POR VERIFICAR con la cuenta real]
4. Telemetría con cifras clave y accesos directos.
5. Cuadre de saldo (ancla + chequeos) + "cuánto tengo" al día + "tu número real".
6. Sección de deudas completa (con avisos locales): ente (comercio o civil) y moneda
   configurable por deuda. [D-036]
7. Tasas: del momento por movimiento; BCV como referencia en pantalla; la app intenta
   actualizarlas a diario (deseado por el propietario [CONFIRMADO D-006]; no es
   infraestructura obligatoria [CONFIRMADO D-011]); si no puede, usa la última guardada y
   avisa. Referencia de frescura del propietario: cuanto más frecuente mejor (ej. cada ~20
   min; evaluación técnica luego). [fuentes POR VERIFICAR — la automática necesita fuente
   que garantice el BCV, D-034; tecnología: TECH.md]
8. Registro del turno del bolso como "plata que vuelve" (no ingreso), con la deuda de
   aportes pendientes cubierta. [CONFIRMADO D-032/D-035]
9. Todo guardado en el almacenamiento privado de la app en el teléfono (SQLite vía Room:
   motor local D-039, formal en TECH.md). Respaldo: intento de copia local diaria de mejor
   esfuerzo + archivo
   exportable del propietario; sin cuentas ni nube sin permiso explícito. Pacto completo:
   la copia diaria se intenta dentro del teléfono; si el teléfono muere sin haber exportado,
   la historia puede no recuperarse, salvo que la copia del sistema restaure datos (comportamiento
   por verificar en el teléfono objetivo). Exportar de vez en cuando es PARTE del respaldo. Al
   importar, la app avisa si el archivo es más viejo que los datos del teléfono.
   [CONFIRMADO D-038; avisos de importación: diseño del Director bajo D-037/D-041;
   detalle fino: DATA-PRIVACIDAD]

10. Registro de "otra plata que entra" (Bs de terceros, con motivo). [CONFIRMADO D-042]

[ABIERTO: lista final de tipos; lista concreta de botones/acciones.]

## 8. Lo que Kaby NO es (fuera de alcance v1.1 — se afina)

- No es multiusuario: es de una sola persona (por ahora, como dice D-017). [D-017]
- No es para iPhone: solo Android. [CONFIRMADO D-031]
- No lee el banco: la clave de Mercantil no se toca (regla de seguridad). Los Bs entran
  por lectura P2P + cuadre. En la práctica descartado; formalmente, la parte de Mercantil
  del D-012 sigue sin cerrar. [CONFIRMADO en los hechos; D-012: PROPOSED]
- No maneja efectivo ni otras cuentas: no existen en su mapa del dinero. [CONFIRMADO; p12]
- No reutiliza Klaris ni el código viejo de la carpeta: se construye de cero. [CONFIRMADO]
- No es una "cabina de avión": "si no es necesario, no debe ocupar espacio". [CONFIRMADO D-015]

## 9. Criterio de terminado

- "Cuando pueda completar todas estas acciones exitosamente y no me ande tirando errores."
  [CONFIRMADO D-021]
- Distribución: Google Play, versión privada solo para el propietario ("que mi teléfono no
  diga que es sospechosa"). [CONFIRMADO D-020; requisitos POR VERIFICAR; verificar también
  la disponibilidad del nombre Kaby en Play — nota D-028]
- [ABIERTO: criterio de terminado por función, a afinar.]

## 10. Orden de construcción

5 etapas, con el orden ya decidido [CONFIRMADO D-030; nada se recorta, todo entra D-018]:
1) El cuaderno → 2) La conexión Binance → 3) La telemetría → 4) Las deudas → 5) El
acabado + Google Play.
**Trámites en paralelo** (no son etapas, toman tiempo): la cuenta de desarrollador de
Google Play (verificación de identidad) y la llave Binance solo-lectura para la etapa 2.
Detalle en ROADMAP.md.

## 11. Identidad visual y de uso

**Cartera de piedra y tinta** (dirección elegida por el propietario en el selector visual el
2026-10-22): una interfaz financiera contemporánea, serena y legible, que muestra cada cifra
junto a su procedencia y estado. Sustituye para esta etapa la identidad anterior “café de
noche”, según el pedido de remodelar Kaby con un estilo nuevo. La paleta usa piedra clara
como fondo, tinta azul profunda como ancla de lectura, verde petróleo para acciones y
valores positivos, y terracota para estados de atención. La composición prioriza números,
horas y origen de datos; evita adornos, ilustraciones y cifras ficticias. Las tarjetas son
superficies claras delimitadas, con elevación discreta. La navegación sigue patrones nativos
de Android y el movimiento respeta la preferencia de la app y la escala del sistema.

El acceso a Inicio, Registro, P2P, Tasas y Ajustes queda visible y agrupado por tarea. La
señal distintiva es mostrar separadas, en Inicio, la hora/fuente de la tasa BCV confirmada
por el propietario y la hora de lectura de Binance Funding; ninguna se presenta como un
saldo combinado ni como “tiempo real” si no existe una lectura que lo demuestre. Tokens y
reglas en `docs/DESIGN.md`.

[CONFIRMADO por selección directa del propietario en esta conversación; contenido funcional
y financiero sigue gobernado por este PRODUCT.md.]

## 12. Decisiones y huecos

- Fuente de verdad: libro de decisiones (D-001..D-048) y archivo de preguntas abiertas.
- POR VERIFICAR (fase técnica): llave Binance solo-lectura y campo "neto" del P2P;
  endpoints de depósitos/balances; límites reales de la API con la cuenta real; requisitos
  de Google Play privado; fuentes de tasas (ve.dolarapi.com candidata, no oficial);
  avisos vs ahorro de batería; stack/arquitectura (TECH.md).
- ABIERTAS (a decidir con el propietario, de a una): cómo se presentan ganancia neta y
  "cuánto se movió tu plata"; lista final de tipos; botones/acciones; fuera de alcance
  fino; criterio de terminado por función.
- Técnicas delegadas (D-029): SQLite local como motor de datos y arquitectura adaptable
  (local-first: conectores intercambiables + sellos de tiempo) [D-039/D-040 CONFIRMED por
  delegación D-048;
  PostgreSQL descartado para v1]; formal en TECH.md con supervisión del propietario.
- Requerimiento del propietario (D-041): manejo de errores quirúrgico y documentado —
  cada pieza falla sola sin derribar el resto; toda espera con timeout y respuesta
  degradada honesta; errores en lenguaje llano; casos canónicos, históricos y
  verificables. Técnica: TECH.md; plan de casos y pruebas: TESTING.md.

## 13. Glosario breve

- **Bs / bolívares:** moneda del día a día; vive en Mercantil.
- **USD / $:** moneda en la que se muestran los resultados.
- **USDT:** los "dólares digitales" que llegan a Binance y se cambian por P2P.
- **P2P:** venta/compra directa USDT ↔ Bs con anunciantes; tasa por operación.
- **BCV:** tasa oficial; referencia de $↔Bs en pantalla; según el propietario, la que
  aplican los comercios al cobrar (p13).
- **Tasa del momento:** la tasa guardada en cada movimiento al registrar; la app la propone
  sola cuando hay fuente que garantice el BCV oficial, y siempre es editable; sin conexión
  se escribe a mano. [D-034 CONFIRMADO con condición]
- **Prima P2P:** Bs extra que deja el P2P respecto a la valuación por BCV.
- **Otra plata que entra:** Bs de un tercero que caen directo en Mercantil; se registran
  con su motivo y suman a ingresos. [D-042]
- **Valuación del Bs:** la tasa con la que el saldo en bolívares aparece en dólares —
  BCV, y se actualiza cuando el BCV se actualiza. [D-044]
- **Bolso:** grupo de aportes por turnos; al recibir el turno es "plata que vuelve", no
  ingreso; si te toca recibir antes de haber aportado todo, lo pendiente de aportar cuenta
  como deuda. Cada bolso es una entidad (nombre, rondas, cuota fija o variable) y pueden
  convivir varios; su deuda vive en la sección de deudas con avisos. [D-032/D-035/D-043]
- **Ente:** a quién le debes — a un comercio o a una persona ("civil"); la moneda de la
  deuda se configura caso por caso. [D-036]
- **Cuenta firme:** el principio del propietario: números reales, sin mentiras ni datos
  inflados; la marca de frescura nunca desaparece y nada se borra en silencio. [D-037]
- **Cuadre:** chequeo del saldo de Mercantil — lo que la app NO puede leer — contra lo
  que calcula (ancla al inicio + chequeos; Binance se confía a su lectura automática).
- **Número real:** lo que tienes − lo que debes.
- **Gastos hormiga:** compritas online en USDT antes de retirar.
