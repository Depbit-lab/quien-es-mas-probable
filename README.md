# ¿Quién es más probable que…?

Juego de preguntas para grupos, en Android. Funciona **sin conexión**: las 150 preguntas
iniciales viajan dentro del APK. La parte comunitaria usa Nostr y es opcional.

Creado por **Depbit**.

## Funciones

- Juego completamente offline con el mazo integrado.
- Votos positivos y negativos en cada pregunta.
- Mazos: **Para ti**, **Populares**, **Nuevas**, **Mis favoritas** y **Mezcla completa**.
- «Para ti» aprende de las categorías que votas y de la popularidad comunitaria.
- «Mis favoritas» reúne las preguntas que has votado con ▲, para repetirlas en la próxima quedada.
- Añadir preguntas nuevas desde el móvil.
- Sincronización opcional con varios relays Nostr.
- Eventos Nostr firmados en código nativo Android. La clave privada se guarda en
  SharedPreferences y nunca se expone al WebView.
- Cola offline para preguntas y votos pendientes.
- Compartir preguntas por WhatsApp u otras apps.
- Compartir el propio APK desde la aplicación.

## Categorías

La lista de categorías es cerrada a propósito, porque la app la usa como filtro contra
preguntas basura que lleguen de la red. Para proponer una categoría nueva, consulta
[CATEGORIAS.md](CATEGORIAS.md).

Para proponer **preguntas** dentro de una categoría existente no hace falta pasar por GitHub:
usa el botón **＋ Añadir** de la app y se publicará en los relays.

## Protocolo comunitario experimental

Se usa el kind NIP-78 `30078`:

- Preguntas: tag `t = sinfiltro-question-v1`, tag `d = q:<id>`.
- Votos: tag `t = sinfiltro-vote-v1`, tag `d = v:<id-pregunta>`.

Los votos son eventos direccionables, por lo que cada clave pública mantiene un único voto
vigente por pregunta.

> Las etiquetas conservan el nombre `sinfiltro` del prototipo original. Son identificadores de
> red, no texto visible: cambiarlas dejaría huérfanas las preguntas y votos ya publicados en
> los relays. Lo mismo ocurre con el identificador de paquete `com.depbit.sinfiltro`, que es la
> identidad permanente de la app en Android.

## Compilar

**Con GitHub Actions:** pestaña *Actions* → *Build Android APK* → descarga el artefacto
`SinFiltro-APK`. Cada push a `main` lo compila automáticamente.

**Con Android Studio:** abre la carpeta, instala el SDK 35 cuando lo pida y ejecuta
*Build > Build APK(s)*.

### Firma

El APK que genera el CI está firmado con una clave de depuración **distinta en cada
ejecución**. Eso basta para probar, pero significa que una versión nueva no se puede instalar
encima de la anterior: Android rechaza la actualización si la firma no coincide.

Para repartir actualizaciones de verdad hay que generar una clave propia y usar siempre la
misma en todas las versiones.

## Licencia

GPL-3.0-or-later. `nostr-tools` no se usa como dependencia: la firma BIP-340 está implementada
localmente en Java, sin librerías externas.
