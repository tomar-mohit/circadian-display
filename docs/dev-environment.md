# Circadian Display — Development Environment

Last verified: 2026-07-15

## Paths

| Variable | Value |
|---|---|
| **Project root** | `D:\VS\circadian-display` |
| **JDK (JetBrains Runtime)** | `C:\Program Files\Android\Android Studio\jbr` |
| **Android SDK** | `D:\Android\SDK` |
| **ADB** | `D:\Android\SDK\platform-tools\adb.exe` |
| **Emulator** | `D:\Android\SDK\emulator\emulator.exe` |
| **Gradle wrapper** | `D:\VS\circadian-display\gradlew.bat` |

## AVD (Android Virtual Device)

```
Name:     Pixel_7
Config:   %USERPROFILE%\.android\avd\Pixel_7.ini
```

## Build Command (PowerShell)

```powershell
# Set JDK and build
cd D:\VS\circadian-display
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
```

```powershell
# Quick build + install + launch
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug
& "D:\Android\SDK\platform-tools\adb.exe" install -r "app\build\outputs\apk\debug\app-debug.apk"
& "D:\Android\SDK\platform-tools\adb.exe" shell am start -n com.circadiandisplay.app/.MainActivity
```

## Emulator Control

```powershell
# Start emulator (background)
Start-Process -FilePath "D:\Android\SDK\emulator\emulator.exe" -ArgumentList "-avd Pixel_7 -no-boot-anim" -WindowStyle Hidden

# Wait for boot + check
& "D:\Android\SDK\platform-tools\adb.exe" wait-for-device
& "D:\Android\SDK\platform-tools\adb.exe" shell getprop sys.boot_completed

# Screenshot
& "D:\Android\SDK\platform-tools\adb.exe" exec-out screencap -p > screenshot.png

# Stop emulator
& "D:\Android\SDK\platform-tools\adb.exe" emu kill
```

## Important Notes

- **JDK is the JetBrains Runtime bundled with Android Studio** — it's JDK 21 (`jbr/` directory). Do NOT use `JAVA_HOME` from system PATH; always set it explicitly.
- **`gradle.properties`** has `org.gradle.java.home` set, but `gradlew.bat` also checks `JAVA_HOME`. Set BOTH for reliability.
- **Room schema exports** go to `app/schemas/` — KSP arg set in `app/build.gradle.kts`.
- **`local.properties`** has `sdk.dir=D\:\\Android\\SDK`.
- **versionCode** defaults to `1` for local dev. CI can override via `-PversionCode=$(git rev-list --count HEAD)`.
- **Configuration cache** is enabled (`gradle.properties`). If you get spurious errors, add `--no-configuration-cache`.

## Module Structure

```
circadian-display/
├── app/                          # Main Android app (wiring + UI)
│   └── src/main/java/com/circadiandisplay/app/
│       ├── data/entity/           # Room entities
│       ├── data/dao/              # Room DAOs
│       ├── data/repository/       # Repositories
│       ├── data/settings/         # DataStore (AppSettings)
│       ├── data/seed/             # Default seed data
│       ├── di/                    # Hilt modules
│       └── ui/dashboard/          # Dashboard screen
├── core/
│   └── curve/                    # Pure Kotlin: CurveEngine, domain models
└── gradle/
    └── libs.versions.toml        # Version catalog
```
