# Sin Filtro Android

Aplicación Android local-first basada en HTML, CSS y JavaScript. Incluye 150 preguntas iniciales dentro del APK.

## Funciones

- Juego completamente offline con mazo integrado.
- Votos positivos y negativos en cada pregunta.
- Selección «Para ti» basada en categorías votadas y popularidad comunitaria.
- Añadir preguntas nuevas desde el móvil.
- Sincronización opcional con varios relays Nostr.
- Eventos Nostr firmados en código nativo Android. La clave privada se guarda en SharedPreferences y no se expone al WebView.
- Cola offline para preguntas y votos pendientes.
- Compartir preguntas por WhatsApp u otras apps.
- Compartir el propio APK desde la aplicación.

## Protocolo comunitario experimental

Se usa el kind NIP-78 `30078`:

- Preguntas: tag `t = sinfiltro-question-v1`, tag `d = q:<id>`.
- Votos: tag `t = sinfiltro-vote-v1`, tag `d = v:<id-pregunta>`.

Los votos son eventos direccionables, por lo que cada clave pública mantiene un único voto vigente por pregunta.

## Compilar

1. Abre la carpeta en Android Studio.
2. Instala Android SDK 35 cuando lo solicite.
3. Ejecuta `assembleDebug` o genera un APK firmado.

El APK de prueba puede instalarse directamente, pero para publicar actualizaciones debes firmar todas las versiones con la misma clave propia.

## Licencia

GPL-3.0-or-later. `nostr-tools` no se usa como dependencia: la firma BIP-340 está implementada localmente en Java.
