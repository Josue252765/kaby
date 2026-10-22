# DATA-PRIVACIDAD.md — La casa de tus datos (D-038/D-013/D-050/D-037)

Version: v1.0 CANÓNICO — ensayo aislado superado (18/18; 2026-10-21) | Fecha: 2026-10-21 | Director-delegación — supervisable.
La vara de este documento: TUS datos son TUYOS y viven donde TÚ decides; la app es el cargador,
no el señor. Este documento define el contrato de privacidad; no es un inventario de funciones
ya implementadas. A 2026-10-21, el código genera y valida una copia JSON privada local; la suite
prueba el codec con un movimiento de ejemplo, que el snapshot no declare campos con nombres de
credencial, el texto de diagnóstico de tasa, la bitácora Room rechaza texto no permitido y las
reglas estáticas del manifest. La prueba de bitácora usa llaves sintéticas; no inspecciona logcat.
El incremento Binance del 2026-10-21 permite introducir una llave HMAC en un diálogo con `SecureOn` y campos ofuscados. Solo se conserva en memoria mientras consulta; no se escribe en Room, preferencias, backup ni logs. No se implementó todavía el almacén cifrado: el gate de backup/D2D del dispositivo sigue pendiente. Los datos leídos (ventas completadas USDT/VES y Funding disponible) sí quedan localmente y entran en la copia privada. La lectura no se envía a servicios del desarrollador. No se han utilizado credenciales reales durante la construcción ni se deben introducir en Appetize. Exportar e importar por SAF siguen sin implementar, pendientes de
la decisión del propietario registrada en `docs/pendientes-del-dueño.md`. Dos pruebas JVM nuevas
comprueban que las solicitudes de respaldo local no generen/escriban snapshots concurrentemente y
que un snapshot inválido no se escriba; no miden temporización de WorkManager ni almacenamiento
físico Android.
`testRespaldo_jsonImportaComparar`, `testRespaldo_importarViejoAvisa` y
`testRespaldo_baulNoTocaVivos` son contratos de TESTING §2 aún no implementados.
`testErrores_copiableSinDatos` sí está implementado. La evidencia ejecutada se registra en
`CHANGELOG.md`.
[CONSTRUCTOR]

## 0. EL INVENTARIO (todo lo que la app guarda)

- Lo que TÚ registras: movimientos (gastos/ingresos con su fecha, categoría y nota),
  "otra plata que entra" CON SU proveniencia (quién/por qué — D-042), deudas y cuotas, tus
  bolsos (tandas) con participantes, ventas P2P a mano ( Bs netos, tasa, USDT, prima), la
  ancla del cuadre, y las tasas BCV que TÚ confirme.
- Lo que la app APORTA (etapa 2+): espejo del historial P2P leído de TU cuenta de Binance
  (tus órdenes, nada de terceros), sellos de hora de cada lectura.
- Lo que la app NUNCA guarda ni pide: tu contraseña de Binance, tu usuario de banco, la
  ubicación, tus contactos, tu cámara/micrófono, tu historial de WhatssApp. NINGÚN dato de
  identidad personal (ni nombre real localizado, solo lo que tú teclees en notas).

## 1. DÓNDE VIVEN (la arquitectura de la privacidad)

- TODO vive en la **base SQLite vía Room**, dentro del almacenamiento interno privado de la
  app en TU teléfono. Android restringe ese almacenamiento a la propia app; este paquete no
  especifica cifrado adicional para toda la base. El cifrado separado de la llave Binance con
  AES-GCM es un requisito pendiente, no una función implementada (§2; ver [archivos internos de la app](https://developer.android.com/training/data-storage/app-specific)).
  Con el teléfono desbloqueado, los movimientos se pueden leer desde la app;
  el PIN/biometría sigue como decisión abierta de §8 (TECH §3).
- NUBE de la app: **NINGUNA.** Sin Firebase, servidores, cuentas, analíticas, publicidad ni
  crash-reporters externos. Conexiones salientes previstas: `api.binance.com` (lectura C2C,
  etapa 2+) y `ve.dolarapi.com` / `rates.dolarvzla.com` (espejos de tasa; los endpoints
  firmados SAPI viven en `api.binance.com`). La lista se audita con Network Inspector
  (TESTING §4).
- Respaldo automático del sistema: el diseño exige `allowBackup=false`, reglas
  `fullBackupContent` para Android 11 y anteriores, y reglas `dataExtractionRules` separadas
  para nube y transferencia entre dispositivos (D2D) en Android 12 y posteriores. Algunos
  fabricantes pueden permitir D2D aunque `allowBackup=false`; el XML no basta para asegurar
  el comportamiento de todos los teléfonos. Antes de guardar la llave Binance o liberar la
  app, hay que probar nube y D2D en el teléfono del propietario. Hasta entonces no se afirma
  que Android nunca copie datos. [Documentación oficial de Android](https://developer.android.com/about/versions/12/behavior-changes-12#backup-restore).
- La exportación iniciada por Kaby sale del teléfono cuando TÚ la haces (§3): el archivo va a
  donde TÚ eliges (Descargas/Drive con SAF). El comportamiento de las copias del sistema se
  rige por §1 y debe comprobarse en el teléfono objetivo.

## 2. LA LLAVE DE BINANCE (el único secreto que existe en Kaby)

- apiKey/SECRET nunca se guardan en código, documentos, logs ni en el JSON de exportación
  manual. El diseño exige excluir el ciphertext de DataStore mediante reglas de respaldo
  Android; requiere verificación D2D/OEM (§1 y TECH §6) antes de afirmar exclusión del sistema.
- Estado actual: la API key y el SECRET se usan solo durante la consulta y no se guardan para
  consultas futuras; al terminar, cancelar o pasar a segundo plano se descartan las referencias.
  No se promete borrado forense de Strings administrados por la JVM. Antes de guardar una llave,
  el diseño exige AES-GCM con clave en Android Keystore y ciphertext en DataStore privado. El
  material de clave no sería exportable; el respaldo por hardware depende del dispositivo y debe
  verificarse con `KeyInfo`. Una llave de solo lectura permite ver el historial de la cuenta,
  pero no operar dinero (D-013).
- Pantalla de pegado con no-captura (no screenshot/no "recents") y llave ofuscada.
- Para la futura persistencia, el botón "borrar llave" deberá destruir el ciphertext local; se
  recomendará revocarla también en Binance (acción manual tuya). El JSON de exportación no incluye la llave.
  En un teléfono nuevo se registra una NUEVA guiado por la app; el ciphertext que pudiera
  transferir el sistema no se considera una llave restaurable.
- Muerte revisable: si un día TÚ quisieras una llave con permiso de operar (NUNCA
  recomendado), la respuesta del producto es NO — este doc es la vara (D-013 vetó).

## 3. EL RESPALDO (decisión del propietario pendiente; D-038; TECH §7)

- Contenido: TODO el paquete del §0 EXCEPTO la llave (§2).
- Contenido lógico: JSON versionado (`schema_version`). La codificación y protección del archivo
  SAF todavía no están elegidas; el snapshot JSON local existente no decide esa puerta.
- Comparación honesta [CONSTRUCTOR]: (a) JSON plano, sencillo de abrir y restaurar sin contraseña;
  cualquiera que obtenga el archivo puede leer montos, fechas, deudas y notas. (b) JSON cifrado con
  contraseña tuya, que protege el contenido mientras el archivo está cifrado y la pide al
  restaurar; si olvidas la contraseña, no hay recuperación. Una implementación correcta requiere
  derivar la clave desde la contraseña y usar cifrado autenticado. El constructor recomienda el
  cifrado por la sensibilidad del contenido; el propietario decide [ABIERTO].
- Advertencia de edad: al importar, la app avisa si el archivo es más VIEJO que los datos del
  teléfono (para no aplastar lo fresco con lo viejo — PRODUCT/ROADMAP).
- Contador honroso: "respaldo externo hace [X días]" en el pie del panel; aviso a los 7 días
  [propuesta configurable — ABIERTO al propietario].
- RESTAURACIÓN VERDAD (la promesa): restaurar es probar: la app restaura en "baúl de
  prueba", compara número contra número, y SOLO entonces toca los datos vivos; si no
  cuadra, ABORTA mostrando el resumen del diff (TESTING §2: testRespaldo_jsonImportaComparar
  + testRespaldo_baulNoTocaVivos; la VERDAD completa en tu teléfono real = casilla §3 etapa 1).

## 4. TRASLADO DE TELÉFONO (el "día de la mudanza" — paso a paso para TÚ)

1. En el teléfono VIEJO: botón "respaldo y salida" → exportar JSON (SAF → Drive/o donde
   prefieras). Ver el contador verde ("respaldo exitoso hace: hoy").
2. En el teléfono NUEVO: instalar Kaby → importar (SAF) → **aviso de edad + verificación
   VERDAD** en pantalla (el número comparado con lo que te diga el resumen, no "confías").
3. Crear la llave NUEVA de Binance guiado (2 minutos: D-013 — la vieja se borra en Binance).
4. BCV: la tasa reciente viaja en el respaldo CON SU HORA; si hoy hay una nueva, entra a mano
   o con espejo+toque (D-034/D-051).
5. HUECO heredado del ROADMAP §12 [POR PROBAR con teléfono real]: qué pasa exactamente
   si el respaldo nació en un Android muy viejo (schema viejo) — la app trae migrador de
   schema y lo declara en pantalla.

## 5. DIAGNÓSTICO Y ERRORES (por qué los bugs no te delatan)

- Toda pantalla de error trae "copiar diagnóstico": versión de la app, hora, fase, mensaje
  técnico. **PROHIBIDO contener**: montos, notas, nombres de terceros, la llave, el SECRET
  (TECH §9; TESTING lo comprueba con la lista prohibida en la mano).
- bitacora_errores vive en el archivo privado, con rotación; jamás se envía a nadie.
- Sin analíticas que lean lo que haces: la app NO sabe "qué botones tocas" fuera de lo que
  necesite para FUNCIONAR (y ni eso — la UI no te espía).

## 6. DATA SAFETY DE GOOGLE PLAY (trámite M5 — la declaración que se pide al publicar)

- Declarado (coherente con este doc): NO recolectamos datos (nada sale del teléfono excepto
  las lecturas de Binance/tasas del §1, que van del teléfono a Binance con TU llave — no a
  nosotros). NO vendemos. NO compartimos. Sin anuncios.
- [POR VERIFICAR el formulario actual de Play cuando toque el trámite (F7) + la verificación
  de identidad de la cuenta].

## 7. LA MUERTE DE LOS DATOS

- Desinstalar la app borra su almacenamiento local. Que Android restaure datos del sistema
  después depende de las reglas de respaldo y, para D2D, del fabricante (§1). No se puede
  asegurar que los datos nunca se restauren al desinstalar hasta verificar el comportamiento
  del teléfono objetivo; el exportable del propietario sigue siendo la copia cuya creación
  puede comprobar la app.
- La app no se roba datos al desinstalar: no hay "residuo" en almacenamiento compartido
  salvo los JSON que TÚ hubieras dejado en Downloads (te los marcaré en la guía de salida:
  "y borra los respaldos que te quedaron").

### 7b. LAS CASAS DE LOS SECRETOS (asalto E — añadido)
- La llave de Binance: apiKey/SECRET no aparecen en el JSON manual (§2); el ciphertext de
  DataStore depende de las reglas y la prueba del respaldo Android (§1).
- La KEYSTORE DE FIRMA de distribución (D-056): archivo + contraseñas en poder del
  PROPIETARIO (fuera del repo y del teléfono de trabajo); sin ella no hay actualización
  firmada (mismo paquete + misma firma = actualizar sin perder datos).
- El respaldo automático del sistema: se excluye mediante reglas del manifest (§1); la
  transferencia D2D debe comprobarse en el teléfono objetivo. No afirmar exclusión universal
  hasta superar esa comprobación.

## 8. HUECOS/DECISIONES ABIERTAS (etiquetadas, sin bloquear el paquete)

- [ABIERTO] PIN/biometría optativa para cuando TÚ prestes el teléfono (la auditoría A3 de la
  ronda 4 demostró el hueco: tus movimientos son legibles SIN cifrado con el teléfono
  desbloqueado).
- [ABIERTO] Cifrado optativo del respaldo (§3b).
- [ABIERTO] El umbral de días del aviso de export (propuesta: 7 — constante ajustable a
  la revisión del propietario).
- [POR VERIFICAR] Identidad/invitación de Play (F7).

FIN — este doc es el respaldo del DISCURSO de DATOS-SAFETY; su vida real se comprueba con
TESTING y la decisión del formato le corresponde al propietario (D-038).
