---
name: Kaby — Cartera de piedra y tinta
description: Interfaz financiera serena que muestra cada cifra con su origen y estado.
colors:
  piedra: "#F3F1EA"
  marfil: "#FFFEFA"
  mineral: "#E7ECE8"
  tinta: "#172B3A"
  papel: "#F6F4EE"
  pizarra: "#5B686E"
  verde-petroleo: "#287366"
  terracota: "#B45644"
  peligro: "#A83D3D"
  linea: "#D7DDD9"
  verde-suave: "#D8EAE4"
  terracota-suave: "#F3DDD6"
  tinta-interior: "#243D4A"
  tinta-borde: "#304555"
  blanco: "#FFFFFF"
  verde-inverso: "#AAD4C8"
  error-suave: "#F4DEDC"
  linea-suave: "#E7EAE6"
  verde-enlace: "#9FD5C8"
  texto-sobre-tinta: "#C1D0D2"
  texto-sobre-tinta-claro: "#D5DEDF"
  aviso-sobre-tinta: "#FFC4B8"
  estado-verde-suave: "#DDECE6"
  propuesta-fondo: "#F0F5F2"
  propuesta-borde: "#C7DCD4"
typography:
  display:
    fontFamily: "sans-serif"
    fontWeight: 700
    fontFeature: "tnum"
  headline:
    fontFamily: "sans-serif"
    fontWeight: 700
  title:
    fontFamily: "sans-serif"
    fontWeight: 600
rounded:
  control: "14dp"
  inset: "16dp"
  tile: "18dp"
  card: "20dp"
  hero: "26dp"
spacing:
  compact: "8dp"
  regular: "12dp"
  tile: "14dp"
  card: "18dp"
  page: "20dp"
components:
  button-primary:
    backgroundColor: "{colors.verde-petroleo}"
    textColor: "#FFFFFF"
    rounded: "{rounded.control}"
    height: "48dp"
  button-keypad:
    backgroundColor: "{colors.mineral}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.control}"
    height: "52dp"
  card-hero:
    backgroundColor: "{colors.tinta}"
    textColor: "{colors.papel}"
    rounded: "{rounded.hero}"
    padding: "20dp"
  card-section:
    backgroundColor: "{colors.marfil}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.card}"
    padding: "18dp"
  card-source:
    backgroundColor: "{colors.marfil}"
    textColor: "{colors.tinta}"
    rounded: "{rounded.tile}"
    padding: "14dp"
---

# Design System: Kaby

## Overview

**Creative North Star: "Cartera de piedra y tinta"**

Kaby usa superficies minerales claras, tinta azul profunda y acentos contenidos. La cifra principal tiene espacio para leerse; el texto cercano dice de dónde proviene y qué tan firme es. Esta dirección fue elegida por el propietario para la etapa descrita en [PRODUCT.md](PRODUCT.md), §11. El código actual la expresa con Compose y Material 3 mediante un único `lightColorScheme`.

**Key Characteristics:**

- Saldo Mercantil protagonista sobre una tarjeta de tinta; la equivalencia en USD solo aparece con una tasa BCV confirmada.
- BCV y Binance Funding ocupan tarjetas separadas; cada una identifica la lectura y su hora cuando existen.
- Superficies claras, bordes definidos y elevación discreta; etiquetas compactas para orientar la lectura.
- Navegación y controles nativos de Android. El contenido no usa saldos de muestra para completar vacíos.

## Colors

Los tokens del frontmatter reproducen los valores declarados en `KabyApp.kt`; el tema Compose es claro.

### Primary

- **Verde petróleo** (`verde-petroleo`): acción principal, icono seleccionado, etiquetas de sección y valores positivos. Su variante clara (`verde-suave`) marca selección en la navegación.

### Secondary

- **Tinta azul** (`tinta`): lectura principal, tarjeta Mercantil y mensajes de confirmación. `tinta-interior` separa la equivalencia dentro de esa tarjeta; `tinta-borde` define su contorno. Los tonos `texto-sobre-tinta` y `texto-sobre-tinta-claro` ordenan el texto secundario; `blanco` marca cifras y acciones de máximo contraste.

### Tertiary

- **Terracota** (`terracota`): atención y estados que requieren revisión. `terracota-suave` aparece como superficie de advertencia; `peligro` identifica errores, `error-suave` su contenedor y `aviso-sobre-tinta` las diferencias sobre la tarjeta Mercantil.

### Neutral

- **Piedra** (`piedra`): fondo de la aplicación.
- **Marfil** (`marfil`): tarjetas ordinarias y barra inferior; **mineral** (`mineral`): superficies secundarias y teclado numérico.
- **Papel** (`papel`): contenido claro sobre tinta; **pizarra** (`pizarra`): descripción secundaria; **línea** (`linea`) y `linea-suave`: contornos del tema. `estado-verde-suave` acompaña estados positivos; `verde-inverso` pertenece al esquema Material y `verde-enlace` al enlace de Tasas dentro de la tarjeta oscura. `propuesta-fondo` y `propuesta-borde` delimitan la propuesta de tasa aún no confirmada.

**The Provenance Rule.** Un color de estado siempre acompaña texto que nombra el estado o la fuente; no debe cargar solo el significado financiero.

## Typography

Compose usa `FontFamily.SansSerif` para `displaySmall`, `headlineSmall` y `titleLarge`, con los pesos declarados en el frontmatter. Los tamaños y alturas de línea de esas tres funciones vienen de `Typography()` de Material 3; este archivo no los redefine. Las demás funciones tipográficas conservan los valores base de Material 3 salvo modificaciones locales.

- **Display:** saldo y monto de edición; dígitos tabulares (`tnum`) y peso fuerte para estabilizar cifras.
- **Headline:** títulos de formularios y cifras destacadas en pantallas como Tasas.
- **Title:** títulos de tarjetas y valores intermedios.
- **Label:** encabezados breves como “TU RESUMEN” y “SALDO MERCANTIL”; algunas etiquetas usan espaciado adicional declarado localmente en Compose.

**The Honest Number Rule.** El saldo Mercantil sin ancla y el monto de formulario vacío muestran `—`; otras lecturas ausentes llevan una explicación. Ninguna jerarquía tipográfica debe hacer parecer confirmado un dato desconocido.

## Layout

El contenido principal usa listas verticales. En Inicio y las listas, el margen horizontal es `20dp`; las secciones se separan habitualmente por `12dp` o `14dp`. La tarjeta Mercantil aparece antes de dos tarjetas de fuente en una fila de pesos iguales. Secciones posteriores muestran métricas, movimientos y ventas P2P. Los formularios usan una columna desplazable, respetan el teclado con `imePadding()` y mantienen acciones al pie.

El código no declara puntos de quiebre por ancho; la disposición observada en fuente es la de Compose con pesos y desplazamiento. No se afirma una adaptación visual comprobada en tamaños concretos de teléfono.

## Elevation & Depth

La profundidad procede de superficies, contornos y elevación nativa de Material 3. Las tarjetas ordinarias declaran `1dp`; la tarjeta Mercantil y el contenedor de la barra inferior, `4dp`; el bloque de monto del formulario, `6dp`. Son valores de elevación de Compose, no sombras CSS. El borde común usa `1dp` con el token `linea`.

La navegación aplica un fundido desde opacidad `0.65` a `1` con base de `180ms` ajustada por la escala de animación del sistema y tope de `300ms`. Solo se aplica cuando la preferencia local de movimiento está activa y la escala del sistema es mayor que cero.

## Shapes

Los controles principales y el teclado numérico usan esquinas de `14dp`. El recuadro de equivalencia usa `16dp`; las tarjetas de fuente y vacíos, `18dp`; las secciones, `20dp`; la tarjeta Mercantil, `26dp`. Las etiquetas de estado usan extremos redondeados mediante `RoundedCornerShape(50)` en Compose. Los bordes de tarjeta son finos y las superficies permanecen lisas, sin textura.

## Components

### Buttons and fields

`PrimaryButton` usa el color primario del tema, `14dp` de radio y altura mínima de `48dp`; durante guardado muestra progreso y se deshabilita. Las acciones secundarias usan `OutlinedButton`. Los campos usan `OutlinedTextField` de Material 3; el monto se introduce mediante un teclado tonal de teclas de `52dp`, en superficie mineral. No hay estilos web de hover en estos componentes nativos.

### Cards and source tiles

`SectionCard` usa marfil, `20dp` de radio, borde común y `18dp` de espacio interior. `ServiceTile` usa marfil, `18dp` de radio y `14dp` de espacio interior; cada tarjeta identifica la fuente, el valor o ausencia de lectura y su detalle temporal. La tarjeta Mercantil usa tinta y contiene el estado de cuadre y una equivalencia anidada solo si existe tasa confirmada.

### Navigation and status

La barra inferior nativa muestra Inicio, Registro, P2P, Tasas y Ajustes; su indicador seleccionado usa verde suave. El botón flotante `+` abre el registro desde Inicio y Registro. `StateTag` y `ReconciliationPill` combinan texto, color y forma para el estado. Los errores aparecen en una superficie mineral con texto de peligro y acciones Copiar/Cerrar.

## Do's and Don'ts

### Do:

- **Do** mostrar fuente, hora y estado junto al dato financiero cuando estén disponibles.
- **Do** mantener BCV confirmado y Binance Funding separados en Inicio.
- **Do** usar las cifras reales del estado de la aplicación y explicar faltas de ancla, tasa o lectura.
- **Do** respetar controles Material nativos, entrada con teclado y la preferencia de movimiento.

### Don't:

- **Don't** llenar tarjetas vacías con cifras ficticias o presentar una propuesta de espejo como tasa BCV confirmada.
- **Don't** presentar Funding como saldo total de Binance ni mezclarlo con Mercantil.
- **Don't** usar color sin texto para transmitir conciliación, advertencia o error.

Esta documentación se contrastó con el código fuente y la dirección aprobada. No constituye una inspección de píxeles ni una prueba de ejecución en dispositivo o emulador. Tampoco acredita una lectura de una cuenta real de Binance.
