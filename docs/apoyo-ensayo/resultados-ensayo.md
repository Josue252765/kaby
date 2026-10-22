# RESULTADOS DEL ENSAYO AISLADO (2026-10-21) — revisión best-effort, NO certificada

Mandato del propietario (D-059): mini proyecto aislado con 3 agentes que construyan desde
los docs e ITEREN para PASAR LAS PRUEBAS. Si pasa TODO MUY BIEN → canonicalizar + plasmar
la documentación. Si NO pasa → mega auditoría y corrección total.

## Los 3 agentes
| Rol | Sesión | Entrega |
|---|---|---|
| Constructor A | ses_ea17a1a91ffeoGj7VCaJ4iF2sH | Contrato ejecutable de la lógica crítica (esquema, regla de tasa, matriz de signo, prima, trampas); delató el hueco de la celda GASTO×TURNO_BOLSO |
| Constructor B (INDEPENDIENTE) | ses_ea1793c3cffeRcLCevlQOxnFtb | Contrato idéntico construido sin ver a A — convergencia total |
| Auditor C | ses_ea1785161ffeblPeVT5DrzA4mF | **CONTRATO VERDE — SÍ**: cada punto con cita respaldante; cero divergencias A↔B; 3 huecos menores |

## Huecos menores delatados y su disposición (CERRADOS)
| Hueco | Enmienda |
|---|---|
| GASTO×TURNO_BOLSO sin declaración explícita (solo derivable) | TECH §3: "Celdas derivadas que el ensayo aislado pidió EXPLÍCITAS" — ahora tiene fila canónica |
| "NULL a mano no deduplica" implícito | TECH §3 ventas: la fila a mano se JUBILA cuando la API trae su orderNumber |
| Redondeo por fila citado solo en TESTING | TECH §3: redondeo 2 decimales POR FILA; el total del día suma primas ya redondeadas |

## Banco ejecutable: harness-oraculo.js → **18/18 PASS (primera corrida)**
El contrato de los constructores, corrido de verdad (aritmética entera en centavos,
regla ÚNICA de tasa, matriz de signo). El oráculo D-037 reproducido dígito a dígito:

| # | Chequeo | Veredicto |
|---|---|---|
| 01 | gastados = 1.590,00 | PASS |
| 02 | ingresos = 90.212,50 | PASS |
| 03 | saldo @2026-09-21 = 101.522,50 | PASS |
| 04 | ganancia neta = 88.622,50 | PASS |
| 05–08 | primas +6.000,00 / +2.962,50 / +1.250,00; suma +10.212,50 que NO toca el saldo | PASS |
| 09 | m6 INGRESO/TURNO_BOLSO: saldo +400, métricas 0 | PASS |
| 10 | AJUSTE_CUADRE: mueve saldo, métricas 0 | PASS |
| 11–12 | edición m7: pre 101.524,00, delta −1,50, fecha ≤ cuadre deja DESCUADRE_ABIERTO; sin corrección, el estado sigue abierto y no se inventa ajuste | PASS |
| 13–14 | JUBILADA cuenta 0 + API idéntica: oráculo intacto; dos upserts de BN-77 conservan una fila | PASS |
| 15 | sin ancla → SIN_ANCLA, saldo NULL (no se inventa) | PASS |
| 16 | sin ninguna tasa: TODAS las primas sin valuar (NULL, JAMÁS 0); los Bs siguen sumando | PASS |
| 17 | prima NEGATIVA sintética: −150,00 con signo | PASS |
| 18 | venta antes de la 1a tasa: sin valuar, prima NULL (JAMÁS 0) | PASS |

### Revisión de las aserciones del harness en esta auditoría

La inspección del harness halló que las aserciones 12, 14 y 16 no probaban el comportamiento que
describía la tabla: la 12 además introducía una reconfirmación que podía cerrar el descuadre sin
la corrección guiada por PRODUCT §4. Se cambió por la regla canónica: sin registrar el gasto
olvidado o un ajuste con nota, el descuadre sigue abierto y no se agrega una fila. La 14 ahora
ejecuta dos upserts de la misma orden y la 16 recalcula el total sin tasas. La 11 comprueba
explícitamente `DESCUADRE_ABIERTO`. La corrida actual dio 18/18. Esto valida el modelo sintético
del harness; no valida la implementación Android.

## Límites honestos de este ensayo
- Ejecuta el CONTRATO lógico (aritmética y reglas del cuadre/prima/invariantes). NO compila
  Kotlin ni corre Android (sin toolchain en el mandato del Director): la suite M1 en código
  (TESTING §2) queda NOT_TESTED aquí; correrá en el proyecto real con el constructor.
- La UI (≤3 toques, pantalla 10) igual: NOT_TESTED, solo casillas.
- Una prueba no corrida NUNCA es PASS: ninguna fila de este acta declara PASS sin corrida.

## Veredicto
**ENSAYO SUPERADO.** Condición del propietario cumplida ("si pasa, y solo si todo sale muy
bien") → los 8 documentos del paquete quedaron **CANÓNICOS** (cabecera de cada doc) y fueron
publicados en `${HOME}/finanzas_app/docs/`, con autorización y verificación registradas en
D-059. La regla de autorización por ruta sigue aplicando a futuras publicaciones fuera de esa
ruta. Esta acta conserva los resultados históricos del ensayo; no afirma que el MD5 externo se
haya revalidado en auditorías posteriores.
La mega auditoría no se activó: no hubo nada que corregir salvo las 3 enmiendas menores,
ya aplicadas y citadas.
