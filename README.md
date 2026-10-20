# Cobalt TV

Reproductor IPTV para Android TV, escrito en Kotlin con Jetpack Compose for TV y
Media3. Pensado para uso personal y un grupo pequeño de amigos: cada usuario
configura sus propias fuentes y reproduce contenido que tiene derecho a usar.

> **Estado: Fase 1 (base navegable).** La app arranca, navega con mando y muestra
> pantallas completas con **datos de demostración**. Todavía no descarga listas
> M3U, no conecta con Xtream Codes y no reproduce vídeo: eso llega en las
> siguientes fases.

![Banner](app/src/main/res/drawable/tv_banner.png)

## Qué hay ahora mismo

- Proyecto Android TV con `LEANBACK_LAUNCHER`, banner de TV e iconos propios.
- Tema oscuro cinematográfico (grafito + azul cobalto) y tipografía para sofá.
- Navegación lateral compacta con foco visible y orden predecible.
- Pantalla de inicio con hero, filas de "Continuar viendo", TV, películas y series.
- TV en directo con categorías y rejilla de canales (demo).
- Películas y series con catálogo de pósteres (demo).
- Búsqueda funcional sobre el catálogo demo, con teclado en pantalla.
- Favoritos vacío, fuentes vacío y ajustes con estado real de la app.
- Avisos de "función en una fase posterior" al pulsar contenido que aún no se
  puede reproducir.

Todo el contenido de ejemplo está marcado con la etiqueta **DATOS DE DEMO** y no
apunta a ningún stream real.

## Stack

| Pieza | Versión |
| --- | --- |
| Kotlin | 2.4.21 |
| Android Gradle Plugin | 9.4.1 (Kotlin integrado) |
| Gradle | 9.6.0 |
| Compose BOM | 2026.09.00 |
| Compose for TV (`androidx.tv:tv-material`) | 1.1.0 |
| Navigation Compose | 2.10.2 |
| compileSdk / targetSdk | 37 |
| minSdk | 24 |

Las versiones se consultaron en Google Maven y en la documentación oficial de
AGP antes de fijarlas. Están centralizadas en
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

> AGP 9 compila Kotlin por sí mismo (built-in Kotlin). El proyecto declara
> `kotlin-gradle-plugin` en el `buildscript` raíz para que el compilador de
> Kotlin y el plugin del compilador de Compose queden en la misma versión.

## Compilar

Requisitos: JDK 17 y el SDK de Android (platform 37 y build-tools 36.0.0).

```bash
./gradlew :app:assembleDebug
```

En Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

`local.properties` con la ruta del SDK no se versiona.

## Instalar en la Mi Box

1. Activa *Depuración USB/ADB* en la Mi Box (Ajustes → Información → compilar
   varias veces, luego Opciones de desarrollador).
2. PC y Mi Box en la misma red.
3. Conecta y autoriza:

```bash
adb connect <IP_DE_LA_MIBOX>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

También puedes copiar el APK y abrirlo con un gestor de archivos o Downloader.

## Emulador de Android TV

```bash
sdkmanager "system-images;android-36;android-tv;x86_64"
avdmanager create avd -n CobaltTV -k "system-images;android-36;android-tv;x86_64" -d tv_1080p
emulator -avd CobaltTV
```

El emulador sirve para UI, foco, navegación y parser. La decodificación real, el
rendimiento y el mando se validan en la Mi Box.

## Estructura

```text
app/src/main/java/io/github/gonzsanz/cobalttv/
  MainActivity.kt
  core/designsystem/     tema, tipografía, foco, componentes, iconos
  data/demo/             catálogo de demostración (solo UI)
  feature/
    home/ live/ movies/ series/ favorites/ search/ settings/ sources/
  ui/
    CobaltTvApp.kt       shell, navegación lateral, NavHost
    components/          tarjetas de contenido
    navigation/          destinos
```

La UI no conoce M3U ni Xtream Codes: cuando se implementen los importadores,
producirán modelos normalizados en `domain` y las pantallas no cambiarán.

## Fases

| Fase | Contenido | Estado |
| --- | --- | --- |
| 1 | Proyecto, launcher TV, navegación, inicio, datos demo | ✅ |
| 2 | Media3: reproducción, controles, ciclo de vida, errores | ⏳ |
| 3 | Importación M3U por URL y archivo + parser tolerante | ⏳ |
| 4 | TV en directo real, categorías, favoritos, EPG | ⏳ |
| 5 | Xtream Codes (login, categorías, canales, VOD) | ⏳ |
| 6 | Películas y series: fichas, temporadas, episodios | ⏳ |
| 7 | Room, DataStore, caché de imágenes, pulido | ⏳ |
| 8 | Pruebas en Mi Box, APK firmado, distribución privada | ⏳ |

No se avanza de fase hasta que la anterior compila y se ha probado.

## Legal y privacidad

- El repositorio no incluye listas, credenciales ni streams.
- Usa solo fuentes que tengas derecho a reproducir.
- Las credenciales de Xtream Codes se guardarán en el dispositivo, nunca en el
  repositorio ni en logs.
- La app no envía datos a servidores propios ni incluye analítica.

El contexto completo del proyecto y las decisiones tomadas están en
[`docs/CONTEXT.md`](docs/CONTEXT.md).
