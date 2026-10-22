# Puertas antes de M1 — hoja de decisión

El entorno Android y sus versiones base están verificados (`docs/ENTORNO-ANDROID.md`). Esta hoja
registra las decisiones abiertas de M1 y el momento en que cada una bloquea una unidad o una
instalación. Los momentos siguen `CONVENTIONS §12` y `BUILD-RUNBOOK §7`; las recomendaciones son
del constructor y **no sustituyen tu decisión**.

| # | Puerta | Recomendación | Efecto si no se decide |
|---|---|---|---|
| 1 | `applicationId` | `com.mofoxx.kaby` provisional para desarrollo | Confírmalo antes de instalar con datos del propietario; cambiarlo después requiere plan de migración/exportación |
| 2 | `minSdk` | 26 provisional (TECH §1) | Antes de instalar, confirmar API del teléfono y compatibilidad de dependencias; si el teléfono es anterior a API 26, no bajar `minSdk` sin verificar el stack |
| 3 | Categorías/tipos | M1 permite texto libre; catálogo y sugerencias quedan para aprobación | No bloquea el registro libre; no inventar una lista de sugerencias |
| 4 | Respaldo | Recomendación del constructor: JSON versionado cifrado con contraseña. Plano: sencillo, sin contraseña y legible por quien obtenga el archivo. Cifrado: exige contraseña para restaurar y esa contraseña perdida vuelve irrecuperable el archivo. La recomendación no aprueba el formato; la decisión queda abierta al propietario. | Confirma el formato antes de implementar exportación/importación de M1 |
| 5 | BORRAR | Recomendación: no en M1; CREAR/EDITAR con rastro | Confirma antes de cerrar la unidad CRUD/bitácora; esa decisión cambia el contrato de acciones y el rastro |
| 6 | Criterio "terminado" por función | El constructor propone lista; tú la apruebas antes de sellar | Bloquea el sello de M1, no las unidades independientes |

**Regla:** no se instala una versión con datos del propietario hasta confirmar `applicationId` y
compatibilidad de `minSdk` con el teléfono real. El desarrollo inicial puede usar los valores
provisionales de arriba. La decisión de cada puerta se registra antes de su unidad dependiente.
