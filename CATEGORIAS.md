# Categorías

La lista de categorías es **cerrada a propósito**. La app usa esta lista como filtro de
seguridad: cualquier pregunta que llegue desde la red Nostr con una categoría que no esté
aquí se reetiqueta automáticamente a «Nuclear» en vez de crear una categoría nueva.

Si la lista fuese libre, en una semana habría «Fiesta», «fiesta», «FIESTA» y «asdfg»
conviviendo en el desplegable. Por eso las categorías nuevas se proponen aquí y las aprueba
Depbit editando este documento.

## Categorías activas

| Categoría | Descripción |
|---|---|
| Fiesta y vergüenza | Borracheras, ridículos en público, resacas y bodas. |
| Sexo y secretos | Lo que nadie confesaría delante de su familia. |
| Policía, delitos y cárcel | Multas, peleas, aduanas y malas decisiones legales. |
| Coches y trabajo | Jefes, despidos, carnets y accidentes tontos. |
| Fama, familia y política | Suegras, herencias, discusiones de sobremesa y el grupo de la familia. |
| Dinero | Deudas, tacañería, estafas, herencias y cuentas a medias. |
| Nuclear | Sin límites. La categoría que rompe amistades. |

## Propuestas pendientes

Añade tu propuesta al final de esta tabla mediante un *pull request*, o abre una incidencia
con la plantilla «Proponer categoría» si no sabes usar Git.

| Categoría propuesta | Descripción | Propuesta por | Estado |
|---|---|---|---|
| _(vacío)_ | | | |

## Cómo se aprueba

1. Alguien añade una fila a **Propuestas pendientes**.
2. Depbit la revisa. Aprobarla consiste simplemente en mover la fila a **Categorías activas**
   en este documento. Rechazarla, en borrarla o marcarla como `Rechazada` con un motivo.
3. Una categoría aprobada **todavía no aparece en la app**. Para que aparezca hace falta
   añadir preguntas de esa categoría al mazo integrado (`app/src/main/assets/questions.js`),
   porque la app construye la lista a partir de las categorías que encuentra en ese fichero.
4. En la siguiente versión del APK la categoría ya está disponible para todo el mundo.

## Qué hace buena a una categoría

- **Que no se solape** con una existente. «Alcohol» ya está cubierto por «Fiesta y vergüenza».
- **Que dé para 15 preguntas como mínimo.** Una categoría con tres preguntas se agota en una
  partida y estorba en el desplegable.
- **Que funcione en grupo.** El juego se juega señalando a alguien, así que la categoría tiene
  que permitir preguntas del tipo «¿Quién es más probable que…?».
- **Que aguante en cualquier grupo.** Si solo tiene gracia entre amigos muy concretos, funciona
  mejor como preguntas sueltas dentro de una categoría existente.

## Proponer preguntas, no categorías

Si lo que quieres es añadir preguntas a una categoría que ya existe, no hace falta pasar por
aquí: hazlo desde el botón **＋ Añadir** de la propia app. La pregunta se firma y se publica en
los relays Nostr, y el resto de jugadores la recibirá al sincronizar.
