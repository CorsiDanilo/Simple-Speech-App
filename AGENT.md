# AGENT.md

## 1. Executable Commands

- Setup / submodules: `git submodule update --init --recursive`
- Dependency installation: Not configured; the Gradle Wrapper resolves dependencies on demand.
- Local development run: Not configured; launch and exercise the app from Android Studio or a connected device.
- Debug build (Windows): `./gradlew.bat :app:assembleDebug`
- Debug build (Linux/CI): `./gradlew :app:assembleDebug`
- Release build (Windows): `./gradlew.bat :app:assembleRelease`
- Release build (Linux/CI): `./gradlew :app:assembleRelease`
- Lint: `./gradlew.bat :app:lintDebug` (Windows) or `./gradlew :app:lintDebug` (Linux/CI)
- Formatting / autofix: Not configured.
- Full automated test suite: `./gradlew.bat :app:testDebugUnitTest` (Windows) or `./gradlew :app:testDebugUnitTest` (Linux/CI)
- Targeted test class: `./gradlew.bat :app:testDebugUnitTest --tests "com.anomalyzed.simplespeechkeyboard.engine.TextPostProcessorTest"`
- Targeted test method: `./gradlew.bat :app:testDebugUnitTest --tests "com.anomalyzed.simplespeechkeyboard.engine.TextPostProcessorTest.testSpokenPunctuationReplacement"`
- Single-file validation: Not configured; Gradle validates Kotlin sources at module/task scope.
- Type checking: `./gradlew.bat :app:compileDebugKotlin` (Windows) or `./gradlew :app:compileDebugKotlin` (Linux/CI)
- Native-only validation: Not configured; CMake is invoked through the Android Gradle build.
- Packaging (Windows): `./gradlew.bat :app:assembleRelease` produces the unsigned release APK; CI signs and renames release artifacts.
- Packaging (Linux/CI): `./gradlew :app:assembleRelease` produces the unsigned release APK; CI signs and renames release artifacts.

## 2. Operational Boundaries

### Always Do

- Run commands from the repository root and inspect `git status` before editing.
- Inspect neighboring Kotlin, C++, CMake, and resource code before introducing a new pattern.
- Initialize/update the recursive `whisper.cpp` submodule before native builds.
- For bugs, reproduce the issue with the smallest relevant unit or device check before fixing it.
- Run the narrowest relevant Gradle validation after changes and keep the diff minimal.

### Ask First

- Add, remove, or upgrade dependencies, Gradle plugins, SDK/NDK versions, or submodule revisions.
- Change the Android manifest, accessibility behavior, public JNI/Kotlin interfaces, or persisted data formats.
- Change release signing, CI release automation, model download behavior, or protected/generated paths.
- Rename or delete shared components, native symbols, or user-facing configuration.

### Never Do

- Never commit secrets, credentials, tokens, `local.properties`, keystores, or signing material.
- Never delete, disable, weaken, or bypass existing tests or validation to make a build pass.
- Never replace the pinned native submodule with an untracked/system copy without approval.
- Never mass-format or refactor unrelated Kotlin, C++, resource, or vendored files.

## 3. Repository-Specific Constraints

- Use the Gradle Wrapper (`gradle-9.0.0`) and the repository version catalog; CI builds with JDK 17.
- The Android module targets/compiles SDK 35, supports API 26+, targets Java/Kotlin 11, and packages only `arm64-v8a` and `armeabi-v7a`.
- `app/src/main/cpp/whisper/CMakeLists.txt` consumes the pinned `third_party/whisper.cpp` checkout and builds the CPU C++17 JNI library with bundled ggml.
- Release signing is CI-only through GitHub secrets; the local `release.jks` is ignored and must not be used or committed.
- Treat `build/`, `app/build/`, `.cxx/`, `app/.cxx/`, `.gradle/`, `.kotlin/`, APKs, and local signing/config files as generated or protected.
- Automated coverage is JVM unit tests under `app/src/test`; no `app/src/androidTest` suite or repository formatter is configured.
