# Publicar Signos Daily en Google Play

Revisión del 28 de octubre de 2026 frente a los requisitos actuales de Google Play, con los mismos pasos que seguiste con Phrasal Daily.

## 1. Lo que ya cumple el código

| Requisito | Estado |
|---|---|
| Nivel de API objetivo: desde el 31/08/2026, las apps nuevas deben apuntar a Android 16 (API 36) | ✅ `targetSdk = 36` y `compileSdk = 36` |
| Formato Android App Bundle (.aab) obligatorio para apps nuevas | ✅ `./gradlew bundlePlayRelease`; GitHub lo genera solo cuando existe la clave de subida |
| Las apps de Play no pueden actualizarse fuera de Play | ✅ Versión `play` sin el aviso de actualización y sin `REQUEST_INSTALL_PACKAGES`. GitHub lo comprueba en cada compilación |
| Permiso de notificaciones (Android 13+) pedido en contexto | ✅ Se pide en la bienvenida y en Ajustes, y la app funciona sin él |
| Alarmas exactas | ✅ `SCHEDULE_EXACT_ALARM` solo hasta Android 12L; en versiones nuevas usa alarmas inexactas |
| Pantalla de borde a borde (obligatorio con API 35+) | ✅ `enableEdgeToEdge()` y márgenes del sistema respetados |
| Páginas de memoria de 16 KB | ✅ La app no tiene código nativo propio |
| Política de privacidad pública | ✅ [PRIVACY.md](PRIVACY.md), enlazada también desde Ajustes > Acerca de |
| Licencia de los vídeos (DILSE, CC BY-NC-SA 3.0) | ✅ Créditos y enlace a la licencia en Ajustes > Créditos y en la Release `media`. ⚠️ Prohíbe el uso comercial: por eso la versión de Play sale **sin anuncios** (apartado 3) |
| Contenido propio | ✅ Las glosas, notas y pistas de las 130 expresiones y 211 signos están escritas para esta app |
| Nombre | ✅ No he encontrado otra app llamada "Signos Daily", pero revísalo tú en Play antes de publicar |

## 2. Lo que tienes que hacer tú

Usa la misma cuenta de Play Console que para Phrasal Daily: no hay que pagar otra vez.

1. **Crear la app** en https://play.google.com/console > **Crear aplicación**: nombre "Signos Daily", idioma español (España), aplicación, gratuita.
2. **Prueba cerrada obligatoria**: si tu cuenta es personal, necesitas al menos **12 testers** con la app instalada durante **14 días seguidos** antes de pedir producción. Puedes reutilizar el grupo de testers de Phrasal Daily.
3. **Clave de subida**: dímelo y te la genero. Tú la guardas en GitHub (**Settings > Secrets and variables > Actions > New repository secret**) como `SIGNING_KEYSTORE_BASE64` y `SIGNING_PASSWORD`. Desde entonces cada compilación deja el `.aab` listo en **Actions > el último build > Artifacts**. Activa **Play App Signing** (viene por defecto).
4. **Rellenar la ficha** con los textos de abajo, el icono de 512×512 (`store/icon-512.png`), la imagen destacada de 1024×500 (`store/feature-1024x500.png`) y al menos 2 capturas del móvil (en **Actions > Capturas para Google Play > Run workflow** se generan solas en la rama `screenshots`).

> Si instalas la versión de Play, antes tendrás que desinstalar la de GitHub una vez, porque van firmadas con claves distintas.

## 3. Anuncios: apagados hasta tener permiso de la CNSE

Los vídeos son del diccionario DILSE de la Fundación CNSE y su licencia (CC BY-NC-SA 3.0) **no permite uso comercial**. Una app con anuncios reales se considera uso comercial, así que la app sale sin anuncios. Mientras tanto ni siquiera incluye el SDK de AdMob, así que no pide el identificador de publicidad. Todo lo de AdMob (banner y consentimiento RGPD) está preparado y se activa cambiando una línea.

### Paso 1: pedir permiso (correo listo para copiar)

Envíalo a **fundacion.cnse@fundacioncnse.org**:

> **Asunto:** Solicitud de autorización para usar vídeos del DILSE en una app educativa
>
> Hola:
>
> Me llamo Andoni Alvarez y he desarrollado Signos Daily, una app gratuita para Android que ayuda a aprender lengua de signos española con un signo y una expresión al día, recordatorios y repasos. El código es público: https://github.com/andonialvarez/signos_app
>
> La app muestra vídeos y fotos del DILSE respetando su licencia CC BY-NC-SA 3.0: cita a la Fundación CNSE en cada vídeo y en los créditos, enlaza al diccionario y no cambia el contenido de los signos (solo los convierte a un formato más ligero para el móvil).
>
> Para poder mantenerla gratis en Google Play me gustaría mostrar un pequeño banner de publicidad. Como eso podría considerarse uso comercial, os escribo para pedir vuestra autorización por escrito, o conocer las condiciones en que sería posible. Si preferís que la app no lleve publicidad, la mantendré sin anuncios.
>
> También estaré encantado de recibir cualquier corrección sobre las frases y notas de gramática, o de colaborar con vosotros si os interesa.
>
> Muchas gracias,
> Andoni Alvarez

### Paso 2: si dicen que sí

1. Entra en https://admob.google.com con tu cuenta (la misma de Phrasal Daily) > **Apps > Añadir app** > Android > Signos Daily.
2. Crea un **bloque de anuncios** de tipo **Banner**.
3. Pásame el **ID de aplicación** (`ca-app-pub-…~…`) y el del **banner** (`ca-app-pub-…/…`). Los pongo en `gradle.properties` y cambio `adsEnabled=true`. No son secretos.
4. En **Privacidad y mensajes**, crea y publica el mensaje de **RGPD** para la app.
5. Actualizo PRIVACY.md y la ficha de Play ("Contiene anuncios") antes de publicar esa versión.

> **Nunca pulses tus propios anuncios reales**: AdMob puede cerrarte la cuenta. Con los anuncios activados, la versión de GitHub muestra siempre anuncios de prueba ("Test Ad"), que no generan ingresos.

## 4. Formularios de Play Console (versión sin anuncios)

- **Seguridad de los datos**: la app **no recoge ni comparte** datos de usuario. El nombre, el progreso y los ajustes se quedan en el móvil y no cuentan como recogidos. La descarga de vídeos desde GitHub es tráfico técnico normal (no se envía nada del usuario).
- **Anuncios**: **no, no contiene anuncios**.
- **ID de publicidad** (en Contenido de la aplicación): **no** usa el ID de publicidad. (Si algún día se activan los anuncios, hay que cambiar estas dos respuestas y la de seguridad de los datos, como en Phrasal Daily.)
- **Acceso a la app**: toda la funcionalidad está disponible sin restricciones.
- **Público objetivo**: 13 años o más.
- **Clasificación de contenido (IARC)**: categoría Educación; sin violencia, sexo, drogas, apuestas ni interacción entre usuarios.
- **Categoría**: Educación.

## 5. Textos de la ficha (listos para copiar)

**Nombre (máx. 30):** Signos Daily: lengua de signos

**Descripción breve (máx. 80):** Aprende lengua de signos española: un signo y una expresión al día, en vídeo.

**Descripción completa:**

> Aprende lengua de signos española (LSE) un poco cada día. Signos Daily te enseña cada día un signo nuevo y una expresión útil, con vídeos del diccionario oficial DILSE de la Fundación CNSE.
>
> Empieza por lo básico ("Me llamo…", "¿Qué tal estás?", "No entiendo", "Más despacio, por favor") y ve ampliando: la casa, la familia, la comida, el transporte, el tiempo, las emociones…
>
> • Signo del día en vídeo, en bucle, a cámara lenta y en espejo para zurdos
> • Expresión del día signo a signo, con el orden de la LSE y notas de gramática
> • Recordatorio diario a tu hora, con frases motivadoras que cambian cada día
> • Rachas de días seguidos y más de 20 logros por desbloquear
> • Estadísticas semanales, mensuales y calendario de estudio
> • Repasos tipo test: qué significa este signo, cuál es su foto, en qué orden se signa
> • Biblioteca con buscador y favoritos
> • Abecedario dactilológico: escribe una palabra y mírala deletreada
> • Funciona sin conexión con los signos que ya has visto
> • Te llama por tu nombre, con tema claro u oscuro
>
> Gratis, sin anuncios y sin registro: tu progreso se guarda en tu móvil.
>
> Vídeos: DILSE, © Fundación CNSE, licencia CC BY-NC-SA 3.0.

## 6. A tener en cuenta

- **Requisito de API cada año**: Google sube el nivel de API obligatorio cada agosto. Para seguir publicando actualizaciones habrá que subir `targetSdk` (lo puedo hacer yo).
- **Verificación de desarrolladores**: desde 2027 Google exigirá que las apps instaladas fuera de Play (como la descarga de GitHub) vengan de un desarrollador verificado. Con tu cuenta de Play Console ya la tendrás; solo habrá que registrar también esta app.
- **Variantes de la LSE**: la LSE tiene variantes regionales y el DILSE recoge la forma de referencia. En la ficha y en la app se aclara que, si en tu entorno se signa distinto, se siga a la comunidad.
