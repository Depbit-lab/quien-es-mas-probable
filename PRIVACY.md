# Política de privacidad

**Aplicación:** ¿Quién es más probable? (`com.depbit.sinfiltro`)
**Responsable:** Depbit
**Contacto:** depbit@proton.me
**Última actualización:** 31 de julio de 2026

Esta aplicación es un juego de preguntas para grupos de adultos. Está pensada para funcionar
sin conexión. No hay cuentas, no hay registro y no se pide ningún dato personal en ningún
momento.

## Resumen

- **No pedimos ni recogemos** nombre, correo, teléfono, ubicación, contactos ni identificadores
  publicitarios.
- **No hay anuncios** ni redes publicitarias.
- **No hay analítica** ni SDK de seguimiento de terceros.
- **No vendemos ni compartimos datos** con nadie con fines comerciales.
- La app funciona **completamente sin conexión** si no usas la parte comunitaria.
- La parte comunitaria es **opcional** y publica en **relays Nostr públicos**. Lo que publicas
  ahí es público y permanente.

## Datos que se guardan en tu dispositivo

Todo esto se queda en tu móvil y nunca se envía a Depbit:

| Dato | Dónde se guarda | Para qué |
|---|---|---|
| Tus votos (▲/▼) y preguntas favoritas | Almacenamiento local del navegador integrado | Construir el mazo «Para ti» y «Mis favoritas» |
| Preguntas ya vistas por sección | Almacenamiento local | Evitar repeticiones y desbloquear la sección |
| Preguntas ocultas y autores bloqueados | Almacenamiento local | Que no vuelvan a aparecerte |
| Lista de relays configurada | Almacenamiento local | Saber a qué servidores conectarse |
| Cola de envíos pendientes | Almacenamiento local | Reintentar cuando vuelva la conexión |
| Clave privada Nostr | `SharedPreferences` privadas de la app | Firmar lo que publicas |

**La clave privada Nostr nunca sale del dispositivo.** Se genera de forma aleatoria la primera
vez que se necesita, se guarda en el almacenamiento privado de la aplicación y solo se usa
dentro del código nativo de Android para firmar. No se expone al contenido web de la app ni se
transmite a ningún servidor.

Desinstalar la aplicación borra todos estos datos, incluida la clave privada. Si la borras
pierdes el control de lo que ya hayas publicado en los relays.

## Datos que salen del dispositivo

Solo cuando **tú** decides publicar algo o sincronizar la comunidad.

### Qué se envía

Al añadir una pregunta, votar o denunciar, se construye un evento Nostr firmado que contiene:

- El texto y la categoría de la pregunta, o el voto o denuncia correspondiente.
- Tu **clave pública** Nostr (un identificador aleatorio, no ligado a tu identidad real).
- La marca de tiempo y la firma criptográfica.

También, por el propio funcionamiento de una conexión de red, los relays pueden ver tu
**dirección IP**.

### A dónde se envía

A los relays Nostr que tengas configurados. Por defecto:

- `wss://relay.damus.io`
- `wss://nos.lol`
- `wss://relay.primal.net`

Puedes cambiarlos o quitarlos desde la pantalla de comunidad de la app.

**Estos relays son servidores de terceros independientes. Depbit no los controla, no los
gestiona y no tiene acceso privilegiado a ellos.** Cada relay aplica sus propias políticas de
retención y privacidad. Consulta las suyas si te preocupa.

### Qué implica

La red Nostr es pública por diseño. Cualquier persona con un cliente Nostr puede leer lo que
publiques. **Una vez publicado, no se puede garantizar el borrado**: los relays pueden
conservar y replicar el contenido indefinidamente, y no existe un botón que lo retire de
todas partes.

Por eso: **no publiques nada que no quieras que sea público y permanente**, y no incluyas datos
personales tuyos ni de nadie en las preguntas que escribas. Las [normas de uso](NORMAS.md)
lo prohíben expresamente.

Depbit **no opera ningún servidor** para esta aplicación y por tanto no almacena ninguna copia
de lo que publicas.

## Contenido generado por usuarios y moderación

La app permite añadir preguntas que ven otras personas. Para limitar el abuso:

- Solo se puede publicar tras haber visto todas las preguntas integradas de esa sección.
- Cada pregunta tiene un botón ⚑ para denunciarla y bloquear a su autor, con efecto inmediato
  en tu dispositivo.
- Una pregunta se oculta automáticamente cuando 3 personas distintas la denuncian, o cuando
  acumula 5 votos o más con menos del 25 % positivos.
- Las categorías son una lista cerrada; cualquier categoría desconocida se reetiqueta.

Las reglas completas están en [NORMAS.md](NORMAS.md). Como la moderación es distribuida y los
relays son independientes, Depbit no puede retirar unilateralmente contenido ya publicado en
la red.

## Edad mínima

**Esta aplicación es solo para mayores de 18 años.** El contenido es deliberadamente incómodo
e incluye temas sexuales, consumo de alcohol y situaciones adultas. No está dirigida a menores
y no recogemos a sabiendas datos de menores.

## Permisos de Android

La aplicación solicita un único permiso:

- **INTERNET**: exclusivamente para conectarse a los relays Nostr cuando usas la parte
  comunitaria. Sin usarla, la app no hace ninguna petición de red.

No pide cámara, micrófono, contactos, ubicación, almacenamiento ni identificadores del
dispositivo.

## Tus derechos

Como no guardamos ningún dato tuyo en ningún servidor, no hay una base de datos de la que
extraerte o borrarte. Puedes:

- **Borrar todos tus datos locales**: desinstala la aplicación o borra sus datos desde los
  ajustes de Android.
- **Cambiar de identidad**: borrar los datos de la app genera una clave Nostr nueva.
- **Dejar de enviar cualquier cosa**: no uses el botón de añadir ni el de sincronizar, o
  vacía la lista de relays.

Para lo ya publicado en relays de terceros, tendrías que dirigirte a cada operador de relay.

## Cambios en esta política

Si cambia, se actualizará en esta misma página y en el repositorio del proyecto. La fecha de
arriba indica la última revisión.

## Contacto

depbit@proton.me

Código fuente: <https://github.com/Depbit-lab/quien-es-mas-probable>
