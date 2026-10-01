# Saarthi

One-tap phone voice controller with offline transcription. Tap **Speak** while parked and say “open Google Maps”, “navigate to Connaught Place”, or “play Arijit Singh”. Saarthi requests the corresponding Android action and shows what it heard. The phone app also retains **Log Fuel** and **History**; existing local fuel entries are unchanged.

## Scope and limits

- Recording lasts six seconds after tapping Speak. Audio is held in memory and is not uploaded.
- Voice commands support opening Google Maps, navigating to a named destination, and requesting music by unstructured search. “Maps kholo”, “CP chalo”, and “Arijit Singh bajao” are also recognized. Unknown or incomplete speech launches nothing.
- Music uses Android's standard play-from-search intent, with no music vendor hard-coded. A compatible app must be installed; it may show a chooser or decline the request. “Requested” does not mean playback or navigation actually started.
- Maps and music apps may need internet access. Saarthi's speech recognition does not.
- Fuel logging remains under Log Fuel. Say a short command such as “Petrol 1500 rupees, 14 litres, meter 45200”, or enter amount, litres and odometer manually. If any field is unclear, nothing is saved.
- The first run copies the bundled 148 MB model to phone storage; transcription may take several seconds, especially on older phones.
- No mileage figure is shown: one fill cannot establish km/L. A later full-tank-to-full-tank workflow would be needed.
- The Android Auto service uses a POI category **only in the debug build** so it can be tried as a sideloaded personal prototype. Fuel logging is not a supported Play Store Android Auto category. Do not publish this build as a POI app.
- Actual car microphone behavior and Hinglish accuracy are unverified until tested in a compatible car.

## Build

Requirements: Android SDK 36, NDK 28.2.13676358, JDK 17, and Android Auto on the phone. The Gradle wrapper and whisper.cpp v1.8.3 submodule are pinned in Git.

```sh
git submodule update --init
curl -fL -o app/src/main/assets/ggml-base.bin https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin
shasum -a 256 app/src/main/assets/ggml-base.bin
ANDROID_HOME=/path/to/Android/sdk ./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Expected model SHA-256: `60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe`. The model is ignored by Git because it is 148 MB. The build fails if it is absent or the wrong size; check its hash before building. The installable APK is at `app/build/outputs/apk/debug/app-debug.apk`.

## Phone test and existing car prototype

1. Install the debug APK on the phone connected to Android Auto: `adb install -r app/build/outputs/apk/debug/app-debug.apk`. This preserves existing app data if the same signing key is used; do not uninstall the app.
2. Open Saarthi on the phone and grant microphone permission. While parked, try a Maps command, a navigation command, and a music command. Check the displayed transcript and the receiving app. Fuel logging remains in its own tab.
3. The car screen is still fuel-only. To try it, enable Android Auto developer mode and “Unknown sources” on the phone, connect to the car, and open Saarthi on the car screen. If the app is hidden, the car host may reject its unsupported category.
4. While parked, use the car's fuel screen to say the fuel sample command. Check the saved entry on the phone. Record the transcript, elapsed time and any error for each failed attempt.

Do the first tests while parked. The car UI uses a six-second capture and a brief spoken result; it has not had a driving distraction review.

Do not run `connectedDebugAndroidTest` on a phone containing fuel logs you want to keep. The test runner may uninstall the app during cleanup, deleting its local database. Use an emulator for instrumented tests.

## Architecture

- `AudioCapture`: records 16 kHz mono PCM from the phone or Android Auto microphone.
- `WhisperEngine` and `voice_jni.cpp`: local transcription through the pinned whisper.cpp source.
- `FuelCommandParser`: converts mixed Hindi/English text and numerals into three validated fields.
- `FuelStore`: SQLite persistence on the phone.
- `VoiceCommandParser` and `VoiceCommandWorkflow`: convert offline speech into supported phone commands.
- `ExternalAction` and `CommandDispatcher`: map commands to documented Android intents and report whether a receiving app accepted the request.
- `FuelWorkflow`: transcribe, parse, save, or reject fuel entries.
- `MainActivity` and `SaarthiCarService`: phone and car interfaces; the car interface remains fuel-only.

No server, account, network permission, or remote sync is used.
