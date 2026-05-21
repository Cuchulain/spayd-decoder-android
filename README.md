# SPAYD dekodér — Android

Nativní Android aplikace (Kotlin + Jetpack Compose) pro dekódování českých platebních **SPAYD** QR kódů. Sourozenec [webové verze](https://github.com/Cuchulain/spayd-decoder).

## Funkce

- **Skenování kamerou** přes CameraX + zxing-cpp (offline, bez sítě, FOSS)
- **Sdílení do aplikace** — appka se zaregistruje do systémového Share Sheetu:
  - sdílený **obrázek** (galerie, screenshot, web prohlížeč) se prožene zxing-cpp
  - sdílený **text** (např. už vyextrahovaný SPAYD řetězec) se rovnou parsuje
  - URI se schématem `spayd:` se otevře přímo
- **Výběr obrázku** z úložiště
- **Parser SPAYD** s odvozením českého čísla účtu z IBAN (`CZ58…99` → `1234567899/0800`)
- **Material 3** s dynamickými barvami (Android 12+), tmavý režim podle systému
- **Tlačítka Kopírovat** u každého pole

## Sestavení

Otevři adresář v **Android Studio** (Hedgehog / 2024.x nebo novější). Studio si dogeneruje Gradle wrapper a stáhne závislosti.

Pro CLI build (vyžaduje nainstalovaný Android SDK a Gradle ≥ 8.10):
```bash
cd spayd-decoder-android
gradle wrapper --gradle-version 8.10
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Klíčové soubory

- `app/src/main/AndroidManifest.xml` — intent-filtery pro `ACTION_SEND` (image/*, text/plain) a `ACTION_VIEW` (`spayd:` scheme)
- `app/src/main/kotlin/.../MainActivity.kt` — zpracování příchozích intentů
- `app/src/main/kotlin/.../data/SpaydParser.kt` — parser (port z JS verze)
- `app/src/main/kotlin/.../ui/CameraScanner.kt` — CameraX preview + zxing-cpp analyzer
- `app/src/main/kotlin/.../ui/ResultView.kt` — karty polí + kopírování

## Verze

- minSdk 24 (Android 7.0), targetSdk 35 (Android 15)
- Kotlin 2.1, AGP 8.7, Compose BOM 2024.12
- CameraX 1.4.1, zxing-cpp 2.3 (Apache 2.0, FOSS — žádné Google závislosti)

## Autor

**Jan Čejka** (alias *Merguian*) — <https://jancejka.cz>
GitHub: [@Cuchulain](https://github.com/Cuchulain)
