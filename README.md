# ¿Quién es más probable que…?

Juego de preguntas para grupos, en Android. Funciona **sin conexión**: las 172 preguntas
iniciales viajan dentro del APK. La parte comunitaria usa Nostr y es opcional.

Creado por **Depbit**.

## Funciones

- Juego completamente offline con el mazo integrado.
- Votos positivos y negativos en cada pregunta.
- Mazos: **Para ti**, **Populares**, **Nuevas**, **Mis favoritas** y **Mezcla completa**.
- «Para ti» aprende de las categorías que votas y de la popularidad comunitaria.
- «Mis favoritas» reúne las preguntas que has votado con ▲, para repetirlas en la próxima quedada.
- Añadir preguntas nuevas desde el móvil, una vez terminada esa sección.
- Sincronización opcional con varios relays Nostr.
- Eventos Nostr firmados en código nativo Android. La clave privada se guarda en
  SharedPreferences y nunca se expone al WebView.
- Cola offline para preguntas y votos pendientes.
- Compartir preguntas por WhatsApp u otras apps.
- Denunciar preguntas y bloquear autores, con ocultación automática por umbral comunitario.

## Moderación

Para añadir una pregunta a una sección hay que haber visto antes todas las preguntas integradas de
esa sección. Es la forma más simple de evitar las repetidas: quien propone ya conoce lo que hay
dentro. Solo cuenta el mazo del APK, así que el listón es fijo, se comprueba sin conexión y nadie
puede subirlo inundando una categoría desde los relays. Terminar las siete secciones deja abierto
el mazo entero.

Las preguntas de la comunidad se ocultan solas cuando 3 personas distintas las denuncian, o
cuando acumulan 5 votos o más con menos del 25 % positivos. Cada jugador puede además ocultar
una pregunta y bloquear a su autor desde el botón ⚑, sin esperar a nadie.

Las reglas y los límites reales del sistema están en [NORMAS.md](NORMAS.md).

## Categorías

La lista de categorías es cerrada a propósito, porque la app la usa como filtro contra
preguntas basura que lleguen de la red. Para proponer una categoría nueva, consulta
[CATEGORIAS.md](CATEGORIAS.md).

Para proponer **preguntas** dentro de una categoría existente no hace falta pasar por GitHub:
usa el botón **＋ Añadir** de la app y se publicará en los relays. El desplegable solo ofrece las
categorías que ya has terminado de jugar; las demás aparecen con las preguntas que te faltan.

## Protocolo comunitario experimental

Se usa el kind NIP-78 `30078`:

- Preguntas: tag `t = sinfiltro-question-v1`, tag `d = q:<id>`.
- Votos: tag `t = sinfiltro-vote-v1`, tag `d = v:<id-pregunta>`.
- Denuncias: tag `t = sinfiltro-report-v1`, tag `d = r:<id-pregunta>`.

Los votos son eventos direccionables, por lo que cada clave pública mantiene un único voto
vigente por pregunta.

> Las etiquetas conservan el nombre `sinfiltro` del prototipo original. Son identificadores de
> red, no texto visible: cambiarlas dejaría huérfanas las preguntas y votos ya publicados en
> los relays. Lo mismo ocurre con el identificador de paquete `com.depbit.sinfiltro`, que es la
> identidad permanente de la app en Android.

## Compilar

**Con GitHub Actions:** pestaña *Actions* → *Build Android APK* → descarga el artefacto
`QuienEsMasProbable`, que contiene el APK firmado y el AAB para Google Play. Cada push a
`main` lo compila automáticamente.

**Con Android Studio:** abre la carpeta, instala el SDK 36 cuando lo pida y ejecuta
*Build > Build APK(s)*. Sin la clave de firma saldrá una release sin firmar, válida solo para
pruebas.

### Firma

Las versiones se firman siempre con la misma clave, así que una versión nueva se instala
encima de la anterior sin perder los votos ni las preferencias.

La clave llega al CI desde tres secretos del repositorio y nunca está en el código:

| Secreto | Contenido |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | El almacén PKCS12 codificado en base64. |
| `ANDROID_KEYSTORE_PASSWORD` | Su contraseña. |
| `ANDROID_KEY_ALIAS` | El alias de la clave, `upload`. |

Para compilar firmado en local, exporta `ANDROID_KEYSTORE_PATH` apuntando al `.p12` junto a
las otras dos variables. Si no existe la clave, el proyecto compila igual pero sin firmar.

Certificado actual, huella SHA-256:

```
32:2A:9C:58:F0:36:76:C4:D0:44:9D:83:A4:58:01:9D:D5:12:49:E4:43:BC:82:8F:6A:B6:FF:CE:02:50:8C:4F
```

Cada compilación comprueba la firma y falla si el APK sale sin firmar.

## Licencia

GPL-3.0-or-later. `nostr-tools` no se usa como dependencia: la firma BIP-340 está implementada
localmente en Java, sin librerías externas.
