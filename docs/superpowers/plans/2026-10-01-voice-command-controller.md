# Voice Command Controller Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a one-tap phone voice controller that requests Google Maps or compatible music actions without coupling recognition to any music vendor.

**Architecture:** Keep Whisper transcription, pure command parsing, and Android intent launching as separate units. Make Voice the phone home screen; retain the existing fuel log and history screens and their database. The phone activity owns the command flow and never claims that an external app completed an action merely because an intent launched.

**Tech Stack:** Kotlin, Android SDK 36 / minSdk 28, existing whisper.cpp JNI and 16 kHz audio capture, JUnit 4, Android instrumentation on the `Pixel_9a` emulator.

**Spec:** `docs/superpowers/specs/2026-10-01-voice-command-controller-design.md`

## Global Constraints

- One tap on the phone starts a six-second recording; no wake word or background listening.
- Initial commands: open Google Maps, navigate to a destination, and request music playback.
- Recognition stays offline; receiving Maps/media apps may need network access.
- No app-specific media adapters, accessibility automation, notification-listener access, or privileged permissions.
- Fuel storage and the existing Android Auto fuel screen remain unchanged.
- Test while parked. Never run `connectedDebugAndroidTest` on the user's phone; it has removed the app and may erase local logs. Use `Pixel_9a` for instrumentation.
- Commit each independently tested task locally; do not push or install on the user's phone without asking.

## Review Focus

- A negated command such as “don't play music” must not launch anything (Task 1 parser test).
- A place or music query with punctuation or extra whitespace must retain the intended words (Task 1 and Task 2 tests).
- Empty audio or unrecognized speech must stop before intent launch (Task 3 workflow test).
- An absent or refusing Maps/media handler must yield an error rather than crash (Task 2 emulator test).
- A second tap while recording/transcribing must not start another capture (Task 3 emulator UI test).

---

### Task 1: Pure command model and parser

**Files:** Create `app/src/main/java/dev/saarthi/app/VoiceCommand.kt`, `VoiceCommandParser.kt`; test in `app/src/test/java/dev/saarthi/app/VoiceCommandParserTest.kt`.

**Interfaces:** Produce `sealed interface VoiceCommand` with `OpenMaps`, `Navigate(val destination: String)`, `PlayMusic(val query: String)`, and `Unsupported`; produce `VoiceCommandParser.parse(raw: String): VoiceCommand` for Tasks 2–3. No Android dependency in either file.

- [ ] **Step 1: Write failing parser tests.** Assert literal results for “open Google Maps”, “Maps kholo”, “navigate to Connaught Place”, “CP chalo”, “play Arijit Singh”, and “Arijit Singh bajao”. Assert `Unsupported` for blank text, “play”, “navigate to”, “don't play music”, and unrelated speech. Assert destination/query trimming and punctuation preservation.
- [ ] **Step 2: Verify red.** Run `ANDROID_HOME=/Users/sourabh/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests dev.saarthi.app.VoiceCommandParserTest`; expect the new behavior test to fail.
- [ ] **Step 3: Implement `VoiceCommandParser.parse(raw: String): VoiceCommand`.** Normalize whitespace and case only for matching; preserve destination/query words. Recognize explicit command forms, never infer a destination or song from unrelated speech.
- [ ] **Step 4: Verify green.** Run the same targeted test, then `:app:testDebugUnitTest`; expect zero failures.
- [ ] **Step 5: Commit.** Stage only Task 1 files and commit `Add pure voice command parser`.

### Task 2: Vendor-neutral Android action boundary

**Files:** Create `app/src/main/java/dev/saarthi/app/ExternalAction.kt`, `CommandDispatcher.kt`; test in `app/src/test/java/dev/saarthi/app/ExternalActionTest.kt` and `app/src/androidTest/java/dev/saarthi/app/CommandDispatcherTest.kt`.

**Interfaces:** Consume `VoiceCommand`. Produce `ExternalAction.forCommand(command: VoiceCommand): ExternalAction?` with action, optional URI, optional package, and string extras. Produce `CommandDispatcher(context: Context).dispatch(action: ExternalAction): DispatchResult`, where result is `Requested` or `Unavailable`.

- [ ] **Step 1: Write failing request tests.** Assert `OpenMaps` maps to `ACTION_VIEW`, `geo:0,0`, and Maps package `com.google.android.apps.maps`; `Navigate("Connaught Place")` maps to `google.navigation:q=Connaught+Place`; `PlayMusic("Arijit Singh")` maps to Android's `MEDIA_PLAY_FROM_SEARCH` action with focus and query extras and no package; `Unsupported` returns null. Include whitespace/punctuation in a destination fixture.
- [ ] **Step 2: Verify red.** Run targeted `:app:testDebugUnitTest --tests dev.saarthi.app.ExternalActionTest`; expect a behavior failure.
- [ ] **Step 3: Implement `ExternalAction.forCommand`.** URI-encode navigation query text and use the documented Android media-search fields.
- [ ] **Step 4: Verify request tests green.** Run the targeted `ExternalActionTest`; expect zero failures.
- [ ] **Step 5: Write the failing no-handler emulator test.** `CommandDispatcherTest` uses a `ContextWrapper` whose `startActivity` throws `ActivityNotFoundException`; assert `Unavailable` without a crash.
- [ ] **Step 6: Verify red on `Pixel_9a`.** Run only `CommandDispatcherTest`; expect the new behavior test to fail.
- [ ] **Step 7: Implement `CommandDispatcher.dispatch(action: ExternalAction): DispatchResult`.** Build an `Intent`, call `startActivity`, and convert `ActivityNotFoundException` or `SecurityException` into `Unavailable`.
- [ ] **Step 8: Verify green.** Run Task 2 unit tests and `CommandDispatcherTest` on `Pixel_9a`; expect zero failures.
- [ ] **Step 9: Commit.** Stage only Task 2 files and commit `Add Android action dispatcher`.

### Task 3: Phone voice flow and retained fuel screens

**Files:** Create `app/src/main/java/dev/saarthi/app/VoiceCommandWorkflow.kt`; modify `MainActivity.kt`, `FuelWorkflow.kt`, `app/src/main/res/layout/screen_home.xml`, `activity_main.xml`, `README.md`; extend `app/src/androidTest/java/dev/saarthi/app/PhoneNavigationTest.kt`; test in `app/src/test/java/dev/saarthi/app/VoiceCommandWorkflowTest.kt`.

**Interfaces:** Consume `VoiceCommandParser`, `WhisperEngine.transcribe(samples: FloatArray): String`, and `CommandDispatcher.dispatch`. `VoiceCommandWorkflow(transcribe: suspend (FloatArray) -> String, parser: VoiceCommandParser)` produces `interpret(samples: FloatArray): VoiceInterpretation(transcript: String, command: VoiceCommand)`; empty samples return `Unsupported` without transcribing. `MainActivity` dispatches only supported commands after interpretation.

- [ ] **Step 1: Write failing workflow tests.** With an injected suspend transcribe function, assert a valid transcript yields the parsed command, empty samples do not call transcription, and unsupported speech yields no launchable command.
- [ ] **Step 2: Verify red.** Run targeted `:app:testDebugUnitTest --tests dev.saarthi.app.VoiceCommandWorkflowTest`; expect a behavior failure.
- [ ] **Step 3: Implement `VoiceCommandWorkflow.interpret`.** Inject the suspend transcriber; return `Unsupported` for empty samples without calling it.
- [ ] **Step 4: Verify workflow green.** Run `VoiceCommandWorkflowTest` and full `:app:testDebugUnitTest`; expect zero failures.
- [ ] **Step 5: Write failing phone UI tests.** Extend `PhoneNavigationTest` to assert the Voice home has a Speak control, Log Fuel and History remain reachable, and Speak becomes disabled after recording starts.
- [ ] **Step 6: Verify UI red on `Pixel_9a`.** Run only `PhoneNavigationTest`; expect the new behavior test to fail.
- [ ] **Step 7: Implement phone UI.** Replace Home's fuel-first content with one prominent Speak control, transcript, busy state, and requested/unavailable result. Keep Log Fuel and History navigation. `MainActivity` owns a shared `WhisperEngine`, passes it into `FuelWorkflow` and voice workflow, and closes it on destruction; the car service keeps its current default `FuelWorkflow` constructor. Guard against a second capture while busy, preserve state across navigation, and leave the fuel database untouched. Update `README.md` for the new commands and limits.
- [ ] **Step 8: Verify UI green.** Run full `:app:testDebugUnitTest`, `PhoneNavigationTest` on `Pixel_9a`, and `:app:assembleDebug`; expect zero failures. Inspect emulator screenshots for safe areas and scroll/accessibility at the default font size.
- [ ] **Step 9: Commit.** Stage only Task 3 files and commit `Add one-tap phone voice controller`.

## Final verification and handoff

- [ ] Run `git diff --check`, full `:app:testDebugUnitTest`, `:app:assembleDebug`, and emulator-only `:app:connectedDebugAndroidTest`; report each actual result.
- [ ] Manually try representative speech while parked on an emulator or a specifically approved test phone. Record transcript, requested intent, handler outcome, and latency; do not claim that external playback or car projection worked without observation.
- [ ] Confirm Git worktree is clean, commits are local, and the final APK path. Ask before installing the APK on the user's phone.
