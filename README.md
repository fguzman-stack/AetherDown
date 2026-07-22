# AetherDown

**Version:** 2.0.0  |  **Creado por:** FGuz20  |  **Arquitectura:** Clean Architecture + MVVM  |  **Target:** Android 15 (API 35)

AetherDown es un gestor de descargas multimedia universal para Android. Soportayt-dlp para extraer streams desde YouTube, Twitter/X, TikTok, Instagram, Facebook, Reddit, Vimeo, Twitch, SoundCloud, etc. Integra un motor de descarga por chunks con reanudación, soporte para torrents, transformación de medios con Media3, modo incógnito, cola de descargas, historial y más.

---

## Indice

1. [Stack Tecnologico](#stack-tecnologico)
2. [Estructura del Proyecto](#estructura-del-proyecto)
3. [Arquitectura y Flujo de Datos](#arquitectura-y-flujo-de-datos)
4. [Modulos Principales](#modulos-principales)
   - [Extraccion con yt-dlp](#extraccion-con-yt-dlp)
   - [Motor de Descarga (DownloadEngine)](#motor-de-descarga-downloadengine)
   - [Descarga Directa (SimpleDownloader)](#descarga-directa-simpledownloader)
   - [Media3 Transformer](#media3-transformer)
   - [Tema y Colores Dinamicos](#tema-y-colores-dinamicos)
   - [Sistema de Idiomas (LocaleHelper)](#sistema-de-idiomas-localehelper)
   - [Persistencia con Room + DataStore](#persistencia-con-room--datastore)
   - [DI con Hilt](#di-con-hilt)
   - [Servicios en Background](#servicios-en-background)
5. [Problemas Conocidos y Codigo Relacionado](#problemas-conocidos-y-codigo-relacionado)
   - [Idiomas no aplican correctamente](#idiomas-no-aplican-correctamente)
   - [Descarga de videos devuelve HTML](#descarga-de-videos-devuelve-html)
   - [No hay selector de formato (audio/video/gif)](#no-hay-selector-de-formato-audiovideogif)
   - [Tema oscuro/claro no cambia](#tema-oscuroclaro-no-cambia)
   - [Smart Mode onboarding se muestra cada vez](#smart-mode-onboarding-se-muestra-cada-vez)
6. [Capturas de Pantalla](#capturas-de-pantalla)
7. [Licencia](#licencia)

---

## Stack Tecnologico

| Componente | Tecnologia | Version |
|-----------|-----------|---------|
| Lenguaje | Kotlin | 2.1.10 |
| UI | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Arquitectura | Clean Architecture + MVVM | — |
| DI | Hilt | 2.54 |
| Red | OkHttp | 4.12.0 |
| Extraccion | yt-dlp (youtubedl-android fork) | 0.18.1 |
| Base de datos | Room | 2.7.1 |
| Preferencias | DataStore Preferences | 1.1.3 |
| Multimedia | Media3 Transformer | 1.5.1 |
| Torrents | (implementacion propia sobre libreria nativa) | — |
| Logging | Timber | 5.0.1 |
| Min SDK / Target SDK | 26 / 35 |

---

## Estructura del Proyecto

```
app/src/main/java/com/aetherdown/app/
├── AetherApp.kt                    # Application: init yt-dlp, Timber
├── MainActivity.kt                 # attachBaseContext (locale), onCreate (theme), intent handling
│
├── data/
│   ├── local/
│   │   ├── AetherDatabase.kt       # Room DB (downloads, history, torrents)
│   │   ├── dao/                    # DownloadDao, HistoryDao, TorrentDao
│   │   └── entity/                 # DownloadEntity, HistoryEntity, TorrentEntity, TorrentFileEntity
│   ├── repository/                 # Implementaciones de repositorios (SettingsRepositoryImpl, etc.)
│   └── transformer/
│       └── Media3TransformerImpl.kt
│
├── di/
│   ├── DatabaseModule.kt           # Provee Room DB y DAOs
│   ├── NetworkModule.kt            # Provee OkHttpClient con interceptors
│   └── RepositoryModule.kt         # Bindings de repositorios
│
├── domain/
│   ├── model/
│   │   ├── AppSettings.kt          # Language, ThemeMode, AppSettings
│   │   ├── DownloadInfo.kt         # DownloadStatus, DownloadPriority
│   │   ├── ExtractResult.kt        # ExtractResult, StreamInfo (con httpHeaders)
│   │   ├── ExtractionError.kt      # sealed class de errores
│   │   └── TorrentInfo.kt
│   ├── repository/                 # Interfaces de repositorios
│   └── usecase/                    # Casos de uso (ExtractUrlUseCase, StartDownloadUseCase, etc.)
│
├── download/
│   ├── DownloadEngine.kt           # Motor principal: cola, paralelismo, reanudacion
│   ├── DownloadTask.kt             # Descarga individual por chunks con Range
│   ├── Range.kt                    # data class para chunks
│   ├── SimpleDownloader.kt         # Descarga directa single-stream con OkHttp
│   └── SpeedLimiter.kt             # Limitador de velocidad sliding-window
│
├── extractor/
│   ├── Extractor.kt                # Interface generica de extraccion
│   ├── ExtractorManager.kt         # Orquestador de extractores
│   └── YtDlpExtractorWrapper.kt    # Wrapper de youtubedl-android
│
├── presentation/
│   ├── home/
│   │   ├── HomeScreen.kt           # Pantalla principal con input, resultados, descarga
│   │   ├── HomeViewModel.kt        # Estado: extraccion, descarga, clipboard
│   │   └── SmartModeOnboarding.kt  # Dialog de onboarding SweetAlert-style
│   ├── history/
│   │   ├── HistoryScreen.kt
│   │   └── HistoryViewModel.kt
│   ├── queue/
│   │   ├── QueueScreen.kt
│   │   └── QueueViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt       # Tema, idioma, descargas, red, notificaciones
│   │   └── SettingsViewModel.kt    # updateThemeMode(), updateLanguage(), recreate()
│   └── navigation/
│       └── AetherNavGraph.kt       # NavHost + Smart Mode dialog overlay
│
├── service/
│   ├── DownloadService.kt          # Foreground service para descarga directa
│   ├── DownloadJobService.kt       # JobScheduler (UIDT) para Android 15+
│   └── ForegroundNotification.kt   # Canal y notificaciones de progreso
│
├── ui/
│   └── theme/
│       ├── Color.kt                # LightColorScheme y DarkColorScheme personalizados
│       ├── Theme.kt                # AetherDownTheme con dynamicColor
│       └── Type.kt                 # Tipografia Material 3
│
├── util/
│   ├── ClipboardHelper.kt          # Lectura del portapapeles
│   ├── Constants.kt
│   ├── Extensions.kt
│   ├── FileUtils.kt               # saveToMediaStore(), getSafeFileName(), mime types
│   ├── FormatUtils.kt
│   ├── LocaleHelper.kt             # applyLanguage() para cambiar locale en runtime
│   └── NetworkUtils.kt
│
└── worker/
    └── DownloadWorker.kt           # WorkManager fallback
```

---

## Arquitectura y Flujo de Datos

```
[UI] Compose (HomeScreen)
  -> ViewModel (HomeViewModel)
    -> UseCase (ExtractUrlUseCase, StartDownloadUseCase)
      -> Repository (SettingsRepositoryImpl, DownloadRepositoryImpl)
        -> DataSource (Room, DataStore, OkHttp)
          -> Extractor (YtDlpExtractorWrapper via youtubedl-android)
          -> Downloader (SimpleDownloader o DownloadEngine)
```

### Flujo de descarga tipico

1. El usuario pega una URL (o se detecta del portapapeles)
2. `HomeViewModel.extractAndDownload()` llama a `ExtractUrlUseCase`
3. `ExtractUrlUseCase` delega en `ExtractorManager` que usa `YtDlpExtractorWrapper`
4. `YtDlpExtractorWrapper` llama a `YoutubeDL.getInstance().getInfo(url)` (yt-dlp)
5. El `VideoInfo` resultante se mapea a `ExtractResult` con lista de `StreamInfo`
6. La UI muestra los streams disponibles (calidad, formato, tamaño)
7. El usuario selecciona un stream
8. `HomeViewModel.startDownload(stream)` llama a `SimpleDownloader.download()` con:
   - `stream.url` (la URL directa del video)
   - `stream.httpHeaders` (cookies, tokens, user-agent especificos del stream)
   - `extractResult.url` como Referer
9. `SimpleDownloader` descarga el archivo via OkHttp, lo valida, y lo guarda en MediaStore

---

## Modulos Principales

### Extraccion con yt-dlp

**Archivo:** `extractor/YtDlpExtractorWrapper.kt`

```kotlin
class YtDlpExtractorWrapper @Inject constructor() : Extractor {

    override suspend fun extract(url: String): Result<ExtractResult> = withContext(Dispatchers.IO) {
        val info = YoutubeDL.getInstance().getInfo(url)
        val streams = buildStreams(info)
        Result.success(ExtractResult(
            title = info.title ?: info.fulltitle ?: "Unknown",
            url = info.webpageUrl ?: url,
            thumbnailUrl = info.thumbnail,
            duration = info.duration.toLong(),
            platform = detectPlatform(info.webpageUrl ?: url),
            streams = streams
        ))
    }
}
```

El metodo `buildStreams()` itera sobre `info.formats` y construye objetos `StreamInfo`. Filtra automaticamente URLs .m3u8 (HLS). Cada `StreamInfo` incluye el campo `httpHeaders` extraido de `VideoFormat.httpHeaders`, que contiene las cookies y cabeceras necesarias para descargar el stream — esto es clave para que la descarga no falle con HTML.

```kotlin
// buildStreams() extrae:
StreamInfo(
    url = streamUrl,
    quality = quality,       // "1080p", "720p", "128kbps", etc.
    format = ext,            // "mp4", "webm", "m4a"
    mimeType = mimeType,     // "video/mp4", "audio/mpeg"
    fileSize = fileSize,
    isAudio = isAudioOnly,
    isVideo = hasVideo || (!isAudioOnly && hasAudio),
    httpHeaders = fmt.httpHeaders ?: emptyMap()  // Cookies del stream
)
```

**Errores manejados:**
- `AgeRestricted` — contenido con restriccion de edad
- `RegionLocked` — bloqueo geografico
- `Content unavailable` — video privado, eliminado o no disponible
- `Unsupported URL` — URL no soportada por yt-dlp

### Motor de Descarga (DownloadEngine)

**Archivo:** `download/DownloadEngine.kt`

Es el motor completo para descargas avanzadas. Gestiona:

- **Cola de descargas** con prioridad (`LOW`, `NORMAL`, `HIGH`)
- **Hasta 3 descargas concurrentes**
- **Reanudacion automatica** desde el ultimo byte descargado
- **Descarga por chunks** (multihilo con cabecera HTTP `Range`)
- **Persistencia en Room** del progreso de cada chunk
- **Compatibilidad con JobScheduler** (UIDT) para Android 15+
- **Fallback a foreground service** si JobScheduler falla

```kotlin
// DownloadEngine usa Dispatchers.IO + SupervisorJob
private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

fun enqueue(entity: DownloadEntity) {
    scope.launch {
        enqueueUserInitiatedJob(context, entity)
            .onFailure { fallbackToForegroundService(entity) }
    }
}
```

**Limitacion:** El DownloadEngine es complejo y tiene dependencias con `DownloadJobService`, `DownloadService`, `DownloadTask` y `ForegroundNotification`. Por esta razon, el flujo de descarga actual usa `SimpleDownloader` como alternativa simplificada.

### Descarga Directa (SimpleDownloader)

**Archivo:** `download/SimpleDownloader.kt`

```kotlin
class SimpleDownloader @Inject constructor(
    private val client: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    suspend fun download(
        url: String,
        fileName: String,
        mimeType: String = "video/mp4",
        referer: String? = null,
        headers: Map<String, String> = emptyMap()
    ): Result<Uri>
}
```

SimpleDownloader es una alternativa directa al DownloadEngine. Realiza una unica peticion HTTP con OkHttp, escribe el contenido a un archivo temporal, valida que no sea HTML/JSON, y lo guarda en MediaStore mediante `FileUtils.saveToMediaStore()`.

**Validacion de contenido:**
```kotlin
// SimpleDownloader valida el archivo descargado:
val fileLen = tempFile.length()
if (fileLen < 100) { /* archivo muy pequeño = error */ }

val magic = tempFile.inputStream().use { it.readNBytes(512) }
val head = String(magic).trimStart().take(50)
if (head.startsWith("<!DOCTYPE") || head.startsWith("<html") || head.startsWith("{")) {
    /* El servidor devolvio HTML o JSON en lugar del video */
}
```

Si el servidor devuelve una pagina HTML de error (ej. por falta de cookies), el downloader rechaza el archivo con el mensaje *"Server returned error page"*.

**Uso de httpHeaders:** Las cabeceras extraidas por yt-dlp (`VideoFormat.httpHeaders`) se pasan como parametro `headers`. SimpleDownloader las aplica a la peticion OkHttp, sobrescribiendo solo las cabeceras que no esten ya definidas (User-Agent, Accept, Accept-Language, Referer).

### Media3 Transformer

**Archivo:** `data/transformer/Media3TransformerImpl.kt`

```kotlin
class Media3TransformerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun extractAudio(inputUri: Uri, outputFilePath: String): Flow<TransformationState> = callbackFlow {
        val transformer = Transformer.Builder(context)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()
        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setRemoveVideo(true)  // Solo extrae el audio
            .build()
        // ...
    }
}
```

Utiliza Jetpack Media3 Transformer (hardware-accelerated con `MediaCodec`) para extraer el audio de un video. Retorna un Flow con el progreso de transformacion.

### Tema y Colores Dinamicos

**Archivos:** `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`

```kotlin
// Theme.kt
@Composable
fun AetherDownTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
```

El tema se configura desde `MainActivity.onCreate()` leyendo `DataStore`:

```kotlin
// MainActivity.onCreate()
val themeMode = runBlocking {
    applicationContext.dataStore.data.first()[stringPreferencesKey("dark_theme")]
}
val isDark = when (themeMode) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
}
AetherDownTheme(darkTheme = isDark, dynamicColor = useDynamic) { ... }
```

Al cambiar el tema en `SettingsScreen`, `SettingsViewModel.updateThemeMode()` guarda en DataStore y emite `_recreateApp.emit(Unit)`. La UI escucha y llama a `Activity.recreate()`.

### Sistema de Idiomas (LocaleHelper)

**Archivo:** `util/LocaleHelper.kt`

```kotlin
object LocaleHelper {
    fun applyLanguage(context: Context, language: Language): Context {
        if (language == Language.SYSTEM) return context  // Deja que el sistema maneje el locale
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
```

El idioma se aplica en `MainActivity.attachBaseContext()`:

```kotlin
override fun attachBaseContext(newBase: Context) {
    val languageCode = runBlocking {
        newBase.dataStore.data.first()[stringPreferencesKey("language")] ?: "system"
    }
    val language = Language.fromCode(languageCode)
    val context = LocaleHelper.applyLanguage(newBase, language)
    super.attachBaseContext(context)
}
```

Los strings traducidos estan en `res/values-{es,fr,de,it,pt}/strings.xml`.

### Persistencia con Room + DataStore

**Archivos:** `data/local/AetherDatabase.kt`, `data/repository/SettingsRepositoryImpl.kt`

Room maneja 4 tablas:
- `downloads` — Descargas activas/completadas con progreso, velocidad, ETA
- `download_history` — Historial de descargas completadas con metadatos
- `torrents` — Torrents activos/completados con estado, velocidades, seeders/leechers
- `torrent_files` — Archivos individuales dentro de un torrent

DataStore almacena las preferencias de la aplicacion (~25 claves):
- Directorio de descarga, maximo de conexiones, limite de velocidad
- Modo solo WiFi, solo cargando, horario restringido
- Tema oscuro, colores dinamicos, idioma
- Organizacion por plataforma/tipo, copias de seguridad
- Preferencias de notificaciones

### DI con Hilt

**Archivos:** `di/DatabaseModule.kt`, `NetworkModule.kt`, `RepositoryModule.kt`

Hilt provee singletons de:
- `OkHttpClient` con interceptor de User-Agent y logging
- `AetherDatabase` y sus DAOs
- Repositorios (`DownloadRepository`, `HistoryRepository`, `TorrentRepository`, `SettingsRepository`)
- `SimpleDownloader`, `DownloadEngine`, `ExtractorManager`
- `ClipboardHelper`, `Media3TransformerImpl`, etc.

### Servicios en Background

**Archivos:** `service/DownloadService.kt`, `DownloadJobService.kt`, `ForegroundNotification.kt`

- **DownloadJobService** extiende `JobService` y usa JobScheduler (UIDT) para Android 15+. Recibe los parametros de descarga via `PersistableBundle`. En `onStartJob()` inicia la descarga; en `onStopJob()` persiste el progreso sincronamente con `runBlocking`.
- **DownloadService** extiende `Service` como foreground service de respaldo. Muestra una notificacion persistente con progreso, velocidad y ETA.
- **ForegroundNotification** crea el canal de notificacion (`CHANNEL_ID = "download_progress"`) y construye la notificacion con `NotificationCompat.Builder`.

---

## Problemas Conocidos y Codigo Relacionado

### Idiomas no aplican correctamente

**Archivos:** `util/LocaleHelper.kt:7-14`, `MainActivity.kt:27-35`

**Problema:** Al seleccionar un idioma en Ajustes y cerrar/reabrir la app, los textos no cambian al idioma seleccionado. El sistema siempre muestra ingles.

**Causa probable:** El metodo `applyLanguage()` en `LocaleHelper` funciona correctamente, pero `attachBaseContext()` en `MainActivity` lee el DataStore con `runBlocking`. Si DataStore aun no se ha inicializado (primera ejecucion), `data.first()` puede devolver valores por defecto. Ademas, `Activity.recreate()` debe llamarse explicitamente al cambiar el idioma para que `attachBaseContext()` se ejecute de nuevo.

```kotlin
// Codigo actual en MainActivity.kt (lineas 27-35):
override fun attachBaseContext(newBase: Context) {
    val languageCode = try {
        runBlocking {
            newBase.dataStore.data.first()[stringPreferencesKey("language")] ?: "system"
        }
    } catch (e: Exception) { "system" }
    val language = Language.fromCode(languageCode)
    val context = LocaleHelper.applyLanguage(newBase, language)
    super.attachBaseContext(context)
}
```

**Posible solucion:** Asegurar que `SettingsViewModel.updateLanguage()` llame a `Activity.recreate()` con un delay para que DataStore persista el cambio antes de recrear la Activity.

### Descarga de videos devuelve HTML

**Archivos:** `download/SimpleDownloader.kt:64-76`, `extractor/YtDlpExtractorWrapper.kt:59-101`, `domain/model/ExtractResult.kt:23`

**Problema:** Al descargar un video desde Twitter/X (y posiblemente otras plataformas), el servidor CDN devuelve una pagina HTML de error en lugar del archivo de video. SimpleDownloader detecta el HTML y muestra el error *"Server returned error page"*.

**Causa:** Las URLs de stream que extrae yt-dlp (ej. `https://video.twimg.com/...`) son URLs firmadas que requieren cookies y cabeceras especificas para ser descargadas. Aunque `VideoFormat.httpHeaders` contiene estas cabeceras, puede que falten cookies adicionales o que la URL haya expirado entre la extraccion y la descarga.

```kotlin
// Validacion en SimpleDownloader.kt (lineas 70-76):
val magic = tempFile.inputStream().use { it.readNBytes(512) }
val head = String(magic).trimStart().take(50)
if (head.startsWith("<!DOCTYPE") || head.startsWith("<html") || head.startsWith("{")) {
    val snippet = String(magic).take(200)
    tempFile.delete()
    return@withContext Result.failure(
        Exception("Server returned error page: ${snippet.take(100)}")
    )
}
```

```kotlin
// Las cabeceras extraidas por yt-dlp (YtDlpExtractorWrapper.kt lineas 88-101):
StreamInfo(
    url = streamUrl,
    quality = quality,
    format = ext,
    mimeType = mimeType,
    fileSize = fileSize,
    httpHeaders = fmt.httpHeaders ?: emptyMap()  // <-- estas cabeceras
)
```

**Posible solucion:** Usar `YoutubeDL.getInstance().execute()` (el metodo de descarga directa de yt-dlp) en lugar de OkHttp para los streams que vienen de yt-dlp. yt-dlp maneja internamente las cookies, la renovacion de URLs firmadas y los reintentos.

### No hay selector de formato (audio/video/gif)

**Archivo:** `presentation/home/HomeScreen.kt`

**Problema:** La UI actual solo muestra la lista de streams disponibles (diferentes calidades y formatos) y permite seleccionar uno para descargar. No hay una opcion explicita para descargar solo el audio, solo el video, o convertir a GIF.

**Codigo actual:** `HomeScreen.kt` muestra los streams en un `LazyColumn` con boton de descarga. No hay filtros ni modo de seleccion de formato.

**Posible solucion:** Agregar filtros en `HomeViewModel` para categorizar streams por tipo:
- "Video + Audio" (streams con video y audio)
- "Solo Video" (streams sin audio, `isAudio = false && hasVideo = true`)
- "Solo Audio" (streams solo audio, `isAudio = true`)
- Usar Media3 Transformer o FFmpeg para convertir a GIF

### Tema oscuro/claro no cambia

**Archivos:** `presentation/settings/SettingsViewModel.kt`, `MainActivity.kt:38-72`

**Problema:** Al cambiar el tema en Ajustes (Claro/Oscuro/Sistema), el cambio no se refleja hasta que se reinicia la app manualmente.

**Causa:** El metodo `updateThemeMode()` en `SettingsViewModel` guarda el valor en DataStore pero no emite correctamente `_recreateApp`. La UI necesita recibir el evento para llamar a `Activity.recreate()`.

```kotlin
// Codigo en SettingsViewModel.kt:
fun updateThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
        updateSettingsUseCase(settings.copy(darkTheme = mode))
        delay(100)  // Espera a que DataStore persista
        _recreateApp.emit(Unit)  // Evento para recrear la Activity
    }
}
```

**Codigo receptor en SettingsScreen.kt:**
```kotlin
LaunchedEffect(Unit) {
    viewModel.recreateApp.collect {
        var activity = LocalContext.current as? Activity
        while (activity is ContextWrapper) {
            activity = (activity as? ContextWrapper)?.baseContext as? Activity
        }
        activity?.recreate()
    }
}
```

**Status actual:** La implementacion existe pero puede fallar si `LocalContext.current` no es una `Activity` (ej. en ciertos contextos de Compose).

### Smart Mode onboarding se muestra cada vez

**Archivo:** `presentation/home/SmartModeOnboarding.kt`

**Problema:** El dialog de onboarding (Smart Mode) se muestra en cada inicio de la app en lugar de solo la primera vez.

**Causa:** `SmartModeOnboarding` usa `SharedPreferences` para recordar si ya se mostro, pero la clave puede no estar persistiendose correctamente o el estado de la preferencia se pierde.

---

## Capturas de Pantalla

*(pendiente)*

---

## Licencia

```
MIT License

Copyright (c) 2025 FGuz20

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
