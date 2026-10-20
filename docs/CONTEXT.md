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
