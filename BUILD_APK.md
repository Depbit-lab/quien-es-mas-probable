# Obtener el APK

## Android Studio

1. Abre esta carpeta como proyecto.
2. Instala Android SDK 35 cuando Android Studio lo solicite.
3. Selecciona **Build > Build APK(s)**.
4. El archivo aparecerá en `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions

1. Sube esta carpeta a un repositorio de GitHub.
2. Abre la pestaña **Actions**.
3. Ejecuta **Build Android APK**.
4. Descarga el artefacto `SinFiltro-APK`.

La compilación de prueba usa la firma de depuración. Para publicar actualizaciones estables, genera una clave de firma propia y conserva la misma clave para todas las versiones.
