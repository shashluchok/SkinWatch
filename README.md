SkinWatch — Kotlin Multiplatform-приложение с таргетами Android, iOS, Web, Desktop (JVM).

* [/iosApp](./iosApp/iosApp) — iOS-приложение. Даже если UI шарится через Compose Multiplatform,
  эта точка входа всё равно нужна для iOS-приложения. Сюда же добавляется SwiftUI-код, если он понадобится.

* [/shared](./shared/src) — код, общий для всех Compose Multiplatform приложений.
  Содержит несколько подпапок:
  - [commonMain](./shared/src/commonMain/kotlin) — код, общий для всех таргетов.
  - Остальные папки — код, который компилируется только под платформу, указанную в названии папки.
    Например, если нужно использовать Apple CoreCrypto в iOS-части — [iosMain](./shared/src/iosMain/kotlin)
    подходящее место для такого кода. Аналогично для Desktop (JVM)-специфичного кода — папка
    [jvmMain](./shared/src/jvmMain/kotlin).

### Git-хуки

`.githooks/pre-commit` гоняется при каждом `git commit` и проверяет только застейдженные файлы:

- **Формат ключей строковых ресурсов.** Если среди застейдженных файлов есть `strings.xml`
  (Android / Compose Multiplatform resources) или `.strings` (iOS), их ключи прогоняются через
  [config/string-keys/validate_string_keys.py](./config/string-keys/validate_string_keys.py) на
  соответствие формату `[dev__](screen_X|dialog_X)__component[__type][__property]` (сама конвенция
  и когда нужны `__type`/`__property` — в скилле `string-resource-keys`). Проверка механическая
  (регистр, разделители, форма), не семантическая — коммит с кривым ключом просто не пройдёт.
  - **Allowlist** — [config/string-keys/allowlist.txt](./config/string-keys/allowlist.txt): ключи,
    которые не подчиняются конвенции и не должны считаться нарушением (например, `app_name` —
    имя, навязанное платформой, а не фичей). Одна запись на строку, `#` — комментарии.
- **ktlint + detekt.** Если среди застейдженных файлов есть `.kt`/`.kts`, оба гоняются, но только по
  застейдженным файлам, а не по всему проекту — быстрее и не блокирует коммит из-за несвязанных
  файлов, которые вы не трогали.

Хук подключается автоматически: при первом запуске Gradle (`./gradlew ...`) корневой
`build.gradle.kts` сам выставляет `core.hooksPath=.githooks` в локальном `.git/config` — руками
`git config` делать не нужно.

### Запуск приложений

Используйте конфигурации запуска из тулбара IDE. Либо эти команды:

- Android-приложение: `./gradlew :androidApp:assembleDebug`
- Desktop-приложение:
  - С hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Обычный запуск: `./gradlew :desktopApp:run`
- Web-приложение:
  - Wasm-таргет (быстрее, современные браузеры): `./gradlew :webApp:wasmJsBrowserDevelopmentRun`
  - JS-таргет (медленнее, поддерживает старые браузеры): `./gradlew :webApp:jsBrowserDevelopmentRun`
- iOS-приложение: открыть директорию [/iosApp](./iosApp) в Xcode и запустить оттуда.

### Запуск тестов

Через кнопку запуска в IDE, либо Gradle-таски:

- Android-тесты: `./gradlew :shared:testAndroidHostTest`
- Desktop-тесты: `./gradlew :shared:jvmTest`
- Web-тесты:
  - Wasm-таргет: `./gradlew :shared:wasmJsTest`
  - JS-таргет: `./gradlew :shared:jsTest`
- iOS-тесты: `./gradlew :shared:iosSimulatorArm64Test`
