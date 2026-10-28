# Signos Daily

App Android para aprender lengua de signos española (LSE) un poco cada día: un signo y una expresión diarios. Es de la misma familia que [Phrasal Daily](https://github.com/andonialvarez/phrasal-daily).

## Qué incluye

- **Signo del día**: la palabra, su vídeo en bucle (velocidad normal o lenta, y en espejo para zurdos), una pista para recordarlo y las expresiones en las que lo usarás.
- **Expresión del día**: frases útiles ("Me llamo…", "¿Qué tal estás?", "No entiendo", "¿Dónde vives?"…) signo a signo, con la glosa en el orden de la LSE (por ejemplo `TÚ VIVIR ¿DÓNDE?`) y una nota de gramática: dónde va el verbo, cómo se marcan las preguntas con la cara, la negación, el tiempo…
- **Contenido diario** en Ajustes: signo y expresión (recomendado), solo signo o solo expresión, con botón para cambiar el de hoy.
- **Recordatorio diario** a la hora que elijas, con frases motivadoras que van cambiando, usando tu nombre y tu racha.
- **Rachas y logros**: días seguidos, mejor racha y 21 logros.
- **Estadísticas**: días esta semana, este mes, total y calendario mensual.
- **Repaso** tipo test: qué significa un signo (vídeo), cuál es la foto de una palabra y en qué orden se signa una frase.
- **Biblioteca** con buscador, favoritos y "por aprender".
- **Sin conexión**: cada vídeo se descarga una vez y se queda en el móvil; en Ajustes puedes descargarlos todos.
- **Perfil**: te saluda por tu nombre. Tema claro/oscuro y reinicio de progreso.

Contenido: 213 signos y 133 expresiones (`app/src/main/assets/content.json`). Tu progreso se guarda en el móvil; no hay servidor ni cuenta.

## De dónde salen los signos

Los vídeos y fotos son del **[DILSE, Diccionario de la Lengua de Signos Española](https://fundacioncnse-dilse.org)** de la Fundación CNSE, el diccionario de referencia de la LSE, publicado con licencia [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/deed.es) (reconocimiento, no comercial, compartir igual).

El workflow **Vídeos de los signos** (`.github/workflows/media.yml`) busca cada signo en DILSE, lo convierte a un MP4 ligero y una foto JPG, y lo publica en la Release `media`, de donde lo descarga la app. Cuando cambias `content.json`, solo procesa los signos nuevos. El informe de lo que ha elegido para cada palabra queda en la rama `media-report`; si eligió la acepción equivocada, añade al signo `"file": "<nombre en DILSE>"` en `content.json`.

Las glosas, notas de gramática y pistas son de esta app, no del diccionario.

Como la licencia de los vídeos no permite uso comercial, **la versión de Google Play no muestra anuncios** (`playAdsEnabled=false` en `gradle.properties`) hasta tener permiso por escrito de la Fundación CNSE. La guía de Play incluye el correo para pedirlo.

## Instalar en el móvil y recibir actualizaciones automáticas

Cada push a `main` lanza GitHub Actions, que compila el APK y lo publica como *Release*.

1. Abre en el móvil https://github.com/andonialvarez/signos_app/releases/latest/download/SignosDaily.apk y abre el archivo descargado.
2. Android te pedirá permitir instalar apps desde el navegador: acéptalo e instala.
3. Cada vez que abras la app, comprueba si hay una versión nueva en GitHub y te ofrece actualizarla (también desde Ajustes > Buscar actualizaciones).

## Google Play

La versión `play` no se actualiza sola (lo prohíbe Google Play) y lleva AdMob con consentimiento RGPD preparado pero apagado; la de GitHub muestra anuncios de prueba. Requisitos y pasos: [PUBLICAR_EN_GOOGLE_PLAY.md](PUBLICAR_EN_GOOGLE_PLAY.md). Privacidad: [PRIVACY.md](PRIVACY.md).

## Compilar en local

Android Studio (Ladybug o posterior): abre la carpeta y pulsa Run. O desde terminal: `./gradlew assembleGithubDebug` (o `assemblePlayDebug`).

## Firma

Por defecto el APK se firma con `app/debug.keystore` (credenciales públicas estándar `android`/`androiddebugkey`), para que cada versión se instale encima de la anterior. La versión de Play usa la clave de subida guardada en los secretos `SIGNING_KEYSTORE_BASE64` y `SIGNING_PASSWORD`.

## Créditos

- Vídeos y fotos de los signos: DILSE, © Fundación CNSE, CC BY-NC-SA 3.0.
- Icono de la mano: Material Symbols de Google, licencia Apache 2.0.
