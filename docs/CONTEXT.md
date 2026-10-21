> **Decisiones de implementación (2026-10-20)**
>
> - Nombre provisional: **Cobalt TV**. Repositorio `gonzsanz/cobalt-tv`,
>   `applicationId` `io.github.gonzsanz.cobalttv` (paquete propio, no `com.example`).
> - Stack fijado: Kotlin 2.4.21, AGP 9.4.1 con Kotlin integrado, Gradle 9.6.0,
>   Compose BOM 2026.09.00, Compose for TV 1.1.0, Navigation Compose 2.10.2,
>   compileSdk/targetSdk 37, minSdk 24.
> - Un solo módulo Gradle (`app`) al principio, con paquetes por capa para poder
>   extraer módulos si el proyecto crece.
> - Navegación principal con Navigation Compose y rutas simples. `sources` no
>   aparece en el rail: se abre desde Inicio y Ajustes.
> - Foco inicial explícito por pantalla (`FocusRequester` con reintento corto)
>   para que el mando siempre tenga un destino predecible.
> - Datos de demostración centralizados en `data/demo/DemoCatalog.kt` y marcados
>   en la UI con la etiqueta **DATOS DE DEMO**.
> - Estado: **Fase 1 completada** (proyecto, launcher de TV, navegación, inicio y
>   pantallas con datos demo; compila y genera APK debug).
> - Verificación en emulador Android TV (API 36, 1080p/320 dpi): foco inicial en
>   todas las pantallas, navegación con D-pad, filtros de TV en directo, búsqueda
>   con teclado y avisos de fase. Rail ajustado a 540 dp de alto (54 dp por
>   elemento) para que Ajustes siempre sea visible; al recuperar el foco, el hero
>   vuelve a la parte superior para no cortar el título.
> - La búsqueda pide foco al entrar y abre el teclado en pantalla. El primer
>   BACK oculta el teclado; el siguiente vuelve a Inicio.
> - CI de GitHub Actions en verde: `assembleDebug` + tests unitarios; el APK
>   release con R8 se comprueba también en local.
>
> **Fase 2 completada (2026-10-21)**
>
> - Media3/ExoPlayer 1.11.1 con `media3-exoplayer`, `media3-exoplayer-hls` y
>   `media3-ui`. HLS y MP4 progresivo por el mismo reproductor.
> - El reproductor es una ruta (`player`) fuera del rail: al abrirse ocupa toda
>   la pantalla y el rail desaparece; BACK vuelve a la pantalla de origen con el
>   foco restaurado.
> - `PlaybackSession` (en memoria) transporta título, subtítulo, URL y si es
>   directo. Evita meter URLs largas o credenciales en la ruta/back stack.
> - Controles propios de TV: play/pausa, −10 s y +10 s (solo VOD), barra de
>   progreso y tiempos. Los controles se ocultan a los 5 s si hay reproducción y
>   reaparecen con cualquier tecla; con los controles ocultos, OK alterna
>   play/pausa y las flechas solo muestran los controles. BACK sale del
>   reproductor (no hay doble BACK: el comportamiento real del NavHost es salir).
> - Estados: spinner de buffering, badge EN DIRECTO sin seek, panel de error con
>   mensaje en español + Reintentar/Volver, y fin de reproducción que vuelve a
>   mostrar controles y enfoca play para repetir.
> - Ciclo de vida: pausa en `ON_STOP`, reanuda en `ON_START` si estaba sonando;
>   audio focus y noisy handling de ExoPlayer; `keepScreenOn` en el PlayerView.
> - Streams de prueba: HLS `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`
>   y MP4 `https://media.w3.org/2010/05/sintel/trailer.mp4`. El bucket antiguo de
>   Google (`commondatastorage.googleapis.com/.../BigBuckBunny.mp4`) devuelve 403
>   y se descartó; sirvió para verificar el panel de error.
> - `InitialFocusEffect` amplía los reintentos a 10×100 ms: en arranque en frío el
>   primer foco del hero podía perderse y el rail se quedaba con el foco.
> - Verificado en emulador API 36 (1080p): reproducción HLS de un canal demo,
>   pausa/reanudación, autoocultado, reaparición con D-pad, VOD MP4 con progreso
>   00:02/00:52, seek ±10 s, fin de reproducción, panel de error 403 y BACK a la
>   pantalla anterior. `assembleDebug`, tests y `assembleRelease` con R8 en verde.
> - Pendiente de la Fase 2: prueba en la Mi Box real (no se toca sin permiso).
>
> **Fase 3 completada (2026-10-21)**
>
> - Importación M3U por URL con OkHttp 5.5.0 y por archivo con el selector SAF
>   (`OpenDocument`). Límite de 16 MB; UTF-8 con fallback Latin-1; errores HTTP y
>   de parseo en español.
> - Parser Android-free y tolerante: BOM/CRLF, listas sin `#EXTM3U`, directivas
>   desconocidas, comas entre comillas, `#EXTGRP`, `tvg-name` como fallback,
>   URLs sueltas y deduplicación por nombre+URL. IDs SHA-256 estables.
> - Modelos `Channel`, `PlaylistSource` y `CatalogState`; `CatalogRepository`
>   expone un `StateFlow` y se provee a la UI con `LocalCatalog`.
> - Una sola fuente activa: importar sustituye la anterior. La lista se cachea en
>   `playlist_cache.m3u` (filesDir) y los metadatos en `SharedPreferences`
>   `cobalt_catalog`; se restaura al arrancar.
> - TV en directo muestra los canales reales con categorías; Inicio y Búsqueda
>   los incluyen. Sin fuente se mantiene el catálogo demo con su badge.
> - Fuentes: tarjeta de estado (nombre, tipo, canales, fecha), Actualizar,
>   Eliminar con confirmación (Cancelar por defecto), formulario para sustituir
>   la lista, spinner compartido y estilo Danger. DOWN desde el campo URL lleva
>   a Importar (o a Elegir archivo si el campo está vacío).
> - Verificación en emulador API 36: importación de 8 canales desde la URL raw
>   de GitHub de la lista de ejemplo, filtros por categoría, reproducción HLS de
>   un canal importado, persistencia tras force-stop y tras reiniciar el
>   emulador completo, actualización correcta y fallo 404 sin perder la lista
>   activa; borrar la fuente vuelve a los datos demo. La búsqueda devuelve los
>   canales reales. `assembleDebug`, 10 tests del parser y `assembleRelease` con
>   R8 en verde; CI de GitHub Actions en verde.
> - El selector SAF del emulador resuelve a un stub sin interfaz; el formulario
>   muestra una nota para usar la URL. La importación por archivo queda pendiente
>   de probar en la Mi Box (Fase 8).
> - No se registran URLs ni credenciales en los logs.
>
> **Fase 4 completada (2026-10-21)**
>
> - Favoritos de canales persistentes en `SharedPreferences` `cobalt_favorites`
>   (clave `channel_ids`), guardados por el ID estable del canal. Mantener OK en
>   Inicio, TV en directo o Favoritos alterna el favorito; los corazones y los
>   avisos confirman el cambio. Los IDs huérfanos se conservan para que al
>   reimportar la misma lista vuelvan los favoritos.
> - Pantalla Favoritos real: mezcla canales reales y demo, etiqueta DATOS DE DEMO
>   solo si hay algún favorito demo, estados de carga, vacío y "Favoritos no
>   disponibles" cuando la fuente activa ya no contiene los canales guardados.
> - Zapping en el reproductor en directo con Arriba/Abajo o CH+/CH−. El salto se
>   hace dentro de la lista desde la que se abrió el canal (Todos, una categoría,
>   Inicio o Favoritos), con vuelta circular; el reproductor no se recrea. El
>   contador "n / total" y una pista en los controles muestran el contexto. En VOD
>   las flechas no cambian de contenido.
> - EPG/XMLTV opcional. `M3uParser` extrae `url-tvg` o `x-tvg-url` del encabezado
>   y lo persiste con la fuente; `EpgRepository` descarga la guía al importar,
>   actualizar o arrancar, con límite de 32 MB y sin registrar su URL.
>   `EpgParser` usa SAX, descarta DTD/entidades externas, filtra solo los
>   `tvg-id` activos y una ventana de 24 h, y limita el título y el número de
>   programas. El estado vive en memoria y se expone por `LocalEpg`.
> - UI de la guía: programa actual en las tarjetas de Inicio, TV y Favoritos;
>   estado de la guía en la cabecera de TV y en la tarjeta de Fuentes; línea
>   Ahora/Después en el reproductor en directo, actualizada al cambiar de canal
>   con zapping. Si no hay guía se usa "En directo"; si falta un programa se
>   muestra "Sin programación"; si la descarga falla, "Guía EPG no disponible".
>   En VOD no se muestra información de EPG.
> - Verificación en emulador API 36 con un servidor XMLTV local y una lista con
>   `url-tvg`: 8 canales con ahora/siguiente en TV, Inicio, Favoritos y
>   reproductor; zapping 1/8 → 2/8 y 2/2 → 1/2 con actualización de la guía;
>   canal ausente en la guía → "Sin programación"; HTTP 500 de la guía → estado
>   de error y tarjetas en "En directo"; lista sin `url-tvg` → "Sin guía EPG";
>   recuperación tras reiniciar la app; VOD sin línea de EPG. 19 tests unitarios
>   (M3U y XMLTV), `assembleDebug` y `assembleRelease` con R8 en verde.
> - Versión visible: **0.4.0 / Fase 4**. Siguiente fase: Xtream Codes.
>
> **Fase 5 completada (2026-10-21)**
>
> - Cliente Xtream Codes con `player_api.php` sobre OkHttp: login, categorías y
>   listas de canales, VOD y series. Las secciones VOD/series son opcionales: un
>   servidor que no las implemente no impide importar la TV en directo. Los
>   arrays ausentes, los IDs numéricos o de texto y los metadatos `null` se
>   normalizan sin romper la importación.
> - Modelos normalizados compartidos con M3U: `Channel` y nuevos `VodItem` y
>   `SeriesItem` (con `sourceId` para pedir temporadas/episodios en la Fase 6).
>   IDs SHA-256 estables por servidor, usuario y contenido; los favoritos siguen
>   funcionando con los canales Xtream.
> - URLs de reproducción: `direct_source` cuando es HTTP(S); si no,
>   `/live/{user}/{pass}/{stream_id}.m3u8` y
>   `/movie/{user}/{pass}/{stream_id}.{ext}`. Se conserva un subdirectorio del
>   servidor (`/iptv`, etc.) y se eliminan endpoints pegados por el usuario
>   (`player_api.php`, `xmltv.php`, `get.php`).
> - Una sola fuente activa también para Xtream. El catálogo normalizado se
>   cachea en `xtream_cache.json` y la fuente en `cobalt_catalog`; al importar
>   M3U se borran la caché y las credenciales Xtream, y viceversa. Actualizar
>   vuelve a consultar la API; un fallo conserva la fuente y el catálogo activos.
> - Credenciales Xtream cifradas con AES/GCM y una clave de Android Keystore
>   (`cobalt_xtream_credentials`). No se guardan en texto plano; si la clave ya
>   no puede descifrarlas, se limpian y se pide reimportar la cuenta. Nunca se
>   registran URLs, credenciales ni respuestas de la API.
> - Formulario de Fuentes con servidor, usuario y contraseña, foco de TV,
>   validación, spinner y errores en español (credenciales, HTTP 401/403/404/5xx,
>   cuenta caducada, red, respuestas inválidas). La tarjeta activa muestra
>   canales, películas, series, servidor y estado de la guía. El campo de
>   contraseña se limpia al completar la importación.
> - EPG Xtream mediante `xmltv.php` derivado del servidor; se reutilizan el
>   parser XMLTV, la ventana de 24 h y la UI de ahora/siguiente. Si el servidor
>   no ofrece guía, los canales siguen en "En directo".
> - Verificación en emulador API 36 contra un servidor Xtream local: login,
>   categorías, 6 canales, 2 películas y 2 series normalizados; EPG ahora/
>   siguiente en TV y reproductor; zapping 2/6 → 3/6; reproducción HLS de un
>   canal con `direct_source`; favorito persistente; actualización; error de
>   contraseña sin perder la fuente; restauración tras force-stop con
>   credenciales descifradas; caché y prefs inspeccionadas para confirmar que no
>   hay credenciales en claro. 31 tests unitarios (M3U, XMLTV y Xtream),
>   `assembleDebug` y `assembleRelease` con R8 en verde.
> - Versión visible: **0.5.0 / Fase 5**. Pendiente: fichas y reproducción de
>   películas/series en la Fase 6; prueba en la Mi Box sin tocar el dispositivo
>   hasta tener permiso.

# CONTEXT.md --- Proyecto de app IPTV para Android TV

## 1. Resumen del proyecto

Quiero desarrollar una aplicación IPTV propia para televisores con
Android TV, empezando por mi Xiaomi Mi Box y, si funciona bien,
compartirla de forma privada con algunos amigos.

La aplicación debe permitir configurar fuentes IPTV mediante: - Listas
M3U por URL. - Archivos M3U locales, si es viable. - Xtream Codes (URL
del servidor, usuario y contraseña).

Tipos de contenido: - Canales de televisión en directo. - Películas
(VOD). - Series, temporadas y episodios.

Objetivo inicial: uso personal y un pequeño grupo de amigos. No se
necesita backend, cuentas propias, sincronización en la nube ni
monetización para el MVP. Cada usuario podrá configurar su propia fuente
y debe utilizar contenido que tenga autorización para reproducir.

La app debe tener una interfaz cuidada, moderna y coherente, inspirada
en las aplicaciones comerciales de streaming, pero diseñada
específicamente para televisión y navegación con mando a distancia.

## 2. Perfil de desarrollo y principios

El desarrollador tiene experiencia programando, especialmente en Java.
Se puede usar Kotlin para aprovechar el ecosistema moderno de Android;
conviene explicar las decisiones técnicas de forma práctica y no asumir
experiencia avanzada en Android TV.

Principios: - Avanzar por fases pequeñas y funcionales. - Probar pronto
en el emulador y después en la Xiaomi Mi Box real. - No implementar todo
de golpe. - Mantener separadas la interfaz, la lógica de negocio, las
fuentes de datos y el reproductor. - Evitar dependencias de
infraestructura innecesarias. - Priorizar estabilidad de reproducción y
navegación con mando por encima de funciones decorativas. - No inventar
APIs de proveedores ni asumir que todas las listas contienen EPG o
VOD. - La compatibilidad real depende de los streams, sus protocolos,
códecs y del hardware.

## 3. Stack técnico recomendado

-   **Kotlin**: lenguaje principal.
-   **Android Studio + Gradle**: entorno de desarrollo.
-   **Jetpack Compose for TV**: interfaz nativa de Android TV; verificar
    la versión y las APIs estables disponibles al crear el proyecto.
-   **ViewModel + StateFlow**: estado de pantallas y lógica de
    presentación.
-   **Kotlin Coroutines**: operaciones asíncronas.
-   **AndroidX Media3 / ExoPlayer**: reproducción de vídeo.
-   **Room**: almacenamiento estructurado local, si el tamaño del
    catálogo y las necesidades lo justifican.
-   **DataStore**: preferencias y configuración no relacional.
-   **OkHttp** u otro cliente HTTP compatible: solicitudes de red cuando
    sea necesario.
-   Caché de imágenes apropiada para Android, seleccionada según
    compatibilidad del proyecto.

No utilizar Spring Boot, Supabase, Firebase ni servidor propio en la
primera versión. Añadir backend solo si aparece una necesidad real (por
ejemplo, sincronización entre dispositivos o distribución centralizada
de datos).

## 4. Arquitectura lógica

Separar la aplicación en capas lógicas:

1.  **UI / presentación**
    -   Pantallas Compose for TV.
    -   ViewModels.
    -   Estados de carga, error, vacío y contenido.
    -   Gestión de foco y navegación con mando.
2.  **Dominio**
    -   Modelos de contenido comunes.
    -   Casos de uso para cargar catálogo, buscar, filtrar, guardar
        favoritos y reproducir.
    -   Interfaces de repositorio.
3.  **Datos**
    -   Importador/parser M3U.
    -   Cliente Xtream Codes.
    -   Importación EPG/XMLTV cuando la fuente lo permita.
    -   Repositorios.
    -   Persistencia local.
4.  **Reproductor**
    -   Gestor Media3/ExoPlayer.
    -   Estado de reproducción.
    -   Controles, selección de pistas cuando estén disponibles, errores
        y reintentos.
    -   Ciclo de vida correctamente gestionado.

Estructura inicial sugerida (adaptable, no es obligatorio crear muchos
módulos Gradle al principio):

``` text
app/src/main/java/com/example/iptv/
  core/
    model/
    network/
    database/
    util/
  data/
    playlist/
    xtream/
    epg/
    repository/
  domain/
    model/
    repository/
    usecase/
  player/
    PlayerManager.kt
    PlayerScreen.kt
  feature/
    home/
    live/
    movies/
    series/
    favorites/
    search/
    settings/
    sources/
  navigation/
  MainActivity.kt
```

Usar un paquete propio en lugar de `com.example.iptv` antes de publicar
una release.

## 5. Modelo de contenido

Crear modelos internos normalizados para que las pantallas no dependan
de si los datos vienen de M3U o Xtream Codes.

Modelo ilustrativo de canal:

``` kotlin
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String? = null,
    val epgId: String? = null
)
```

Crear modelos separados para `Movie`, `Series`, `Season` y `Episode`; no
forzar todo a ser un canal. Considerar identificadores estables,
proveedor/fuente de origen y metadatos opcionales.

M3U no garantiza por sí solo metadatos completos de películas, series,
temporadas o episodios. Xtream Codes suele ofrecer una estructura VOD
más organizada, pero las capacidades varían según el servidor. EPG puede
necesitar una fuente aparte, como XMLTV. Todas las funciones deben
degradarse correctamente cuando falten metadatos.

## 6. Diseño visual y experiencia de usuario

### Dirección de arte

-   Tema oscuro premium, cinematográfico y moderno.
-   Fondo base azul-negro/grafito, no negro absoluto en todas las
    superficies.
-   Acento azul eléctrico/cobalto para selección, acciones principales y
    elementos activos.
-   Tarjetas con esquinas redondeadas moderadas y separación generosa.
-   Tipografía limpia, legible desde el sofá y con jerarquía clara.
-   Imágenes y carátulas grandes, de buena proporción y recorte
    consistente.
-   Iconografía sencilla y coherente.
-   Evitar exceso de neones, gradientes fuertes, demasiados bordes o
    elementos pequeños.
-   Mantener contraste accesible y tamaños de texto adecuados a
    distancia de televisión.
-   La interfaz debe parecer un producto de streaming cuidado, no un
    panel web adaptado sin más.

Paleta de partida orientativa: - Fondo principal: azul-negro/grafito. -
Superficies: gris carbón ligeramente más claro. - Texto principal:
blanco roto. - Texto secundario: gris claro. - Acento: azul
eléctrico/cobalto. - Errores: rojo moderado; estados positivos: verde
discreto. La paleta exacta se puede ajustar durante los primeros
prototipos.

### Reglas de interacción Android TV

La navegación se hace principalmente con mando: flechas, OK/Enter, Atrás
y, según dispositivo, botones multimedia. - El foco siempre debe ser
visible: borde, escala ligera o cambio de superficie. - No depender de
hover, ratón o gestos táctiles. - El orden de foco debe ser predecible
entre menú, filas y tarjetas. - Al entrar y salir de una pantalla,
restaurar el foco de forma sensata. - Los objetivos interactivos deben
ser grandes y separados. - Atrás debe comportarse de forma
consistente. - Evitar formularios largos y escritura frecuente con el
teclado en pantalla. - Mostrar feedback inmediato al cargar una lista,
cambiar de canal o guardar favoritos. - Proporcionar estados de carga,
error, vacío y desconexión bien diseñados. - Verificar que la app puede
iniciarse desde el launcher de Android TV y que está configurada
correctamente como aplicación de televisión.

### Pantallas previstas

1.  **Inicio**
    -   Navegación lateral compacta.
    -   Acceso a TV en directo, Películas, Series, Favoritos y Ajustes.
    -   Continuar viendo, canales recientes/favoritos y filas de
        contenido destacado cuando haya datos.
    -   Banner destacado opcional; no mostrar grandes zonas vacías si la
        lista no aporta imágenes/metadatos.
    -   Evitar contenido ficticio en la versión real.
2.  **TV en directo**
    -   Diseño de dos zonas: lista/categorías de canales y reproductor;
        alternativa de reproducción completa con guía superpuesta.
    -   Logos, nombre, grupo/categoría y estado de programa si hay EPG.
    -   Zapping con flechas o controles claros.
    -   Búsqueda y favoritos.
    -   La guía EPG debe indicar cuándo no hay programación disponible,
        sin inventarla.
3.  **Reproductor**
    -   Vista de vídeo prioritaria, con controles que aparezcan y
        desaparezcan de forma apropiada.
    -   Play/pausa, cambio de canal, información del contenido y
        selector de audio/subtítulos cuando el stream lo permita.
    -   Mostrar errores recuperables con acciones útiles.
    -   Opcionalmente, panel de depuración oculto para pruebas:
        resolución, formato, bitrate si se conoce, estado de búfer y
        tipo de stream.
4.  **Películas**
    -   Catálogo en cuadrícula o filas horizontales con pósteres.
    -   Categorías, búsqueda, ficha de película y botón de reproducir.
    -   Año, duración, descripción y demás datos solo si la fuente los
        proporciona.
5.  **Series**
    -   Catálogo de series con pósteres.
    -   Ficha, selección de temporada, lista de episodios y
        reproducción.
    -   Historial/progreso de episodios como función posterior.
6.  **Favoritos**
    -   Favoritos de canales y, si se decide, películas/series.
    -   Estados vacíos claros y accesos directos.
7.  **Buscar**
    -   Búsqueda adaptada al teclado en pantalla de TV.
    -   Buscar en categorías de contenido según lo permita el catálogo.
8.  **Fuentes / Configuración IPTV**
    -   Añadir fuente M3U por URL.
    -   Importar archivo M3U si se implementa.
    -   Añadir Xtream Codes con URL base, usuario y contraseña.
    -   Probar conexión y mostrar errores comprensibles.
    -   Actualizar catálogo y eliminar/editar fuentes.
    -   Evitar exponer contraseñas en logs o pantallas.
    -   No guardar secretos en texto plano si existe una alternativa de
        almacenamiento más segura y compatible.
9.  **Ajustes generales**
    -   Preferencias de reproducción disponibles.
    -   Gestión de fuentes.
    -   Limpieza de caché, información de la app y versión.
    -   Opciones de calidad solo cuando el stream lo permita; no
        prometer cambio de calidad para una URL fija.

## 7. Reproducción y calidad

La calidad final no la determina solo la aplicación. Depende de la
fuente, bitrate, compresión, estabilidad del servidor, red, protocolo,
códecs y compatibilidad del dispositivo.

Con Media3/ExoPlayer: - Empezar con streams de prueba autorizados y
formatos compatibles. - Probar HLS/DASH adaptativo si se dispone de
fuentes que realmente ofrezcan varias representaciones. - Para una URL
fija M3U no prometer selección dinámica de calidad. - Gestionar
buffering, errores de red, reintentos limitados y liberación correcta
del reproductor. - Probar H.264 y, posteriormente, otros códecs/formatos
según soporte real. - Verificar audio, subtítulos, relación de aspecto,
cambio de canal y sesiones largas. - No afirmar que 4K está garantizado:
depende del modelo de Mi Box, el stream, el códec, el bitrate y la
salida de vídeo. - Evitar reintentos infinitos y no bloquear la UI
durante peticiones o parseo.

## 8. Plan de desarrollo por fases

Las estimaciones son aproximadas para una persona y pueden variar. Es
preferible avanzar por entregables verificables.

### Fase 1 --- Proyecto y navegación base

-   Instalar Android Studio y SDK.
-   Crear proyecto Kotlin con Compose for TV.
-   Configurar el manifiesto, launcher y compatibilidad Android TV.
-   Implementar navegación lateral, inicio y foco con mando.
-   Ejecutar en el emulador. **Entregable:** app navegable con datos de
    muestra claramente identificados como mock.

### Fase 2 --- Reproductor mínimo

-   Integrar Media3/ExoPlayer.
-   Reproducir una URL de vídeo de prueba legal y compatible.
-   Implementar controles básicos y gestión del ciclo de vida.
-   Probar estados de carga y error. **Entregable:** reproducción
    funcional en emulador y, si es posible, en Mi Box.

### Fase 3 --- Importación M3U

-   Formulario para URL y, opcionalmente, archivo local.
-   Parser que tolere líneas malformadas y metadatos opcionales.
-   Extraer nombres, URLs, logos, grupos y EPG IDs si existen.
-   Informar de errores de descarga/parseo.
-   Guardar fuente y catálogo de forma apropiada. **Entregable:**
    catálogo de canales procedente de una M3U real autorizada.

### Fase 4 --- TV en directo

-   Categorías, lista de canales, búsqueda y favoritos.
-   Reproducción desde selección de canal.
-   Zapping y estados de error.
-   EPG/XMLTV si hay fuente disponible. **Entregable:** experiencia
    funcional de televisión en directo.

### Fase 5 --- Xtream Codes

-   Formulario de servidor, usuario y contraseña.
-   Cliente de API según la interfaz que soporte el servidor.
-   Normalizar categorías, canales, películas y series.
-   Construir URLs de reproducción de forma compatible con la fuente.
-   Tratar errores de autenticación, desconexión y respuestas
    incompletas. **Entregable:** segunda fuente integrada a través de
    los mismos modelos internos.

### Fase 6 --- Películas y series

-   Catálogo y fichas.
-   Reproducción de películas.
-   Temporadas y episodios.
-   Progreso y continuar viendo si hay tiempo. **Entregable:** VOD
    funcional.

### Fase 7 --- Persistencia y pulido

-   Room para datos que necesiten consultas estructuradas.
-   DataStore para preferencias.
-   Caché de imágenes y actualización del catálogo.
-   Estados vacíos, errores, navegación y foco.
-   Revisión de privacidad y credenciales. **Entregable:** experiencia
    estable para uso diario.

### Fase 8 --- Pruebas y distribución privada

-   Probar en la Xiaomi Mi Box real.
-   Medir inicio, cambio de canal, buffering y estabilidad.
-   Corregir problemas de mando y compatibilidad.
-   Crear APK release firmado y conservar de forma segura el keystore.
-   Compartir el APK con amigos mediante enlace de descarga.
    **Entregable:** versión privada instalable.

Plan orientativo: un MVP básico puede llevar unas semanas de trabajo
parcial; una app pulida con canales, EPG, películas, series y manejo
robusto de errores puede llevar bastante más. No tratar los plazos como
garantías.

## 9. Desarrollo y pruebas desde el PC

-   Usar Android Studio y un emulador Android TV para UI, navegación,
    parser, datos y gran parte de las pruebas.
-   Usar la Mi Box real para comprobar decodificación, calidad, red,
    mando y rendimiento.
-   Conectar por ADB si la versión del dispositivo permite depuración;
    normalmente PC y Mi Box deben estar en la misma red y hay que
    autorizar la conexión.
-   Probar primero una URL de vídeo de prueba autorizada antes de
    integrar las fuentes IPTV.
-   Crear datos mock para diseñar las pantallas sin depender de que una
    lista IPTV esté disponible.
-   No asumir que el emulador reproduce con la misma fluidez que el
    hardware físico.
-   En este PC el AVD `CobaltTV` (Android TV API 36, 1920×1080) usa el
    puerto 5556 y se inicia con `-gpu host`. Con
    `-gpu swiftshader_indirect` aparecieron artefactos de framebuffer
    (texto duplicado) tras sesiones largas de vídeo; reiniciar el
    emulador completo y usar el renderizador del host los elimina.

## 10. Distribución a amigos

Estrategia inicial: 1. Generar un APK firmado de release desde Android
Studio. 2. Conservar la misma clave de firma para todas las
actualizaciones. 3. Alojar el APK en un lugar controlado, por ejemplo
GitHub Releases o una web estática. 4. Compartir una URL o código de
descarga que pueda abrirse desde Downloader en Android TV. 5. El usuario
instala el APK y permite la instalación desde esa fuente si el sistema
lo solicita. 6. Para actualizar, publicar una nueva versión con el mismo
applicationId y certificado de firma. Al principio, la instalación de la
actualización puede ser manual.

No se necesita backend para distribuir un APK privado. Una app normal no
puede instalar actualizaciones silenciosamente sin los
permisos/mecanismos correspondientes. Si se publica en Google Play en el
futuro, hay que revisar las políticas y requisitos vigentes.

## 11. Seguridad, legalidad y privacidad

-   Usar únicamente listas y contenidos que el usuario tenga derecho a
    reproducir.
-   No distribuir listas de canales, credenciales o contenido protegido
    de terceros.
-   No incluir credenciales reales en el repositorio, código fuente ni
    logs.
-   No mostrar contraseñas al depurar.
-   Tratar URLs de fuentes IPTV como datos no confiables y validar
    entradas.
-   Limitar reintentos y evitar peticiones que bloqueen el hilo
    principal.
-   No introducir analítica, seguimiento ni servidor externo
    innecesario.
-   Compartir el APK de forma privada y avisar de que solo deben
    instalar versiones obtenidas de la fuente de confianza del
    desarrollador.

## 12. Criterios de aceptación del MVP

El MVP puede considerarse listo cuando: - Se instala y arranca en
Android TV. - Toda la navegación principal funciona con mando. - Se
puede configurar una fuente M3U. - Los canales se listan con los
metadatos disponibles. - Se puede reproducir un stream compatible. - Hay
búsqueda y favoritos básicos. - Se puede configurar una fuente Xtream
Codes. - Películas y series aparecen cuando la fuente las ofrece. - Los
fallos de red, credenciales y streams se muestran de forma entendible. -
La aplicación no se bloquea al cargar un catálogo. - Los datos locales
persisten al cerrar y abrir la app. - Se ha probado en una Xiaomi Mi Box
real. - Se puede generar e instalar un APK firmado.

## 13. Cómo debe ayudar la IA durante el desarrollo

Trabajar de manera incremental: 1. Revisar el estado real del
repositorio antes de proponer cambios. 2. Acordar una fase y sus
criterios de aceptación. 3. Implementar una unidad funcional cada vez.
4. Proporcionar archivos completos o diffs precisos y rutas concretas.
5. Explicar comandos de Gradle y pasos para ejecutar pruebas. 6. No
inventar dependencias, APIs ni nombres de métodos; comprobar versiones y
documentación oficial cuando haga falta. 7. Añadir pruebas unitarias
para parser M3U, normalización y lógica que se pueda probar sin
dispositivo. 8. No pasar a la siguiente fase hasta que la actual compile
y se haya probado. 9. Mantener este archivo actualizado cuando se tomen
decisiones de arquitectura o diseño.

## 14. Primer siguiente paso

Empezar por la **Fase 1**: definir el nombre provisional de la app y el
`applicationId`, crear el proyecto Kotlin/Compose for TV, configurar el
launcher de Android TV y diseñar una pantalla de inicio con navegación
lateral y tarjetas de muestra. Primero hacer que compile y navegue bien
en el emulador; todavía no integrar M3U, Xtream Codes ni el reproductor.
