# Lira — Android nativo

Proyecto Android Studio para analizar enlaces multimedia, seleccionar una calidad y administrar descargas en segundo plano.

## Requisitos

- Android Studio con JDK 17
- Android SDK 35
- Android 7.0 o superior (minSdk 24)

Abre esta carpeta en Android Studio y permite que Gradle sincronice las dependencias. La app apunta a Android 14 (targetSdk 34).

## Funciones incluidas

- Interfaz nativa con Kotlin, Jetpack Compose y Material 3.
- Recepción de enlaces desde el menú Compartir de Android.
- Análisis de formatos y estimación de tamaño.
- Descarga de video MP4 y extracción de audio MP3/M4A mediante yt-dlp y el FFmpeg incluido por la biblioteca.
- Metadatos y miniatura incrustados cuando la fuente los ofrece.
- Descargas persistentes con WorkManager, progreso real y notificación de primer plano.
- Guardado en `Download/Movies` o `Download/Music` mediante MediaStore; compatibilidad con almacenamiento anterior a Android 10.
- Búsqueda de letras en LRCLIB y archivo `.lrc` junto al audio si existe una coincidencia.

## Dependencias importantes

El proyecto usa la publicación actual de `youtubedl-android` en Maven Central (`0.18.1`); el proyecto cambió desde las coordenadas JitPack indicadas en el brief. El módulo `ffmpeg` pertenece a la misma biblioteca y no es FFmpegKit.

`youtubedl-android` está publicado bajo GPL-3.0. Revisa sus condiciones antes de distribuir una versión de esta app.

LRCLIB es una API pública y no requiere clave. La app limita su uso a una búsqueda por descarga de audio; configura una URL o correo de contacto propio en el encabezado de cliente antes de distribuirla públicamente.

## Notas

- Usa la app para contenido que tengas derecho a descargar y respeta los términos de cada fuente.
- El acceso a formatos depende de lo que yt-dlp pueda analizar para cada URL.
- Esta carpeta contiene un proyecto Android Studio nativo; no es una app Expo ni una vista previa web.
