# Saarthi voice command controller — design

Date: 2026-10-01

## Purpose and boundary

Make Saarthi a phone-based, one-tap voice controller for common driving tasks. The user taps **Speak** on the phone while parked, says a short command, and Saarthi converts the offline transcript into a supported Android action. The initial actions are open Google Maps, navigate to a destination, and request music playback. Fuel logging remains available as a secondary feature, with its existing local data untouched.

This release does not add a wake word, replace Gemini or Google Assistant, control the Android Auto connection, or promise that actions will appear on the car display. The Android Auto fuel screen remains unchanged. Voice-command testing is phone-first and done while parked.

## Approach

Use Android's published intents rather than hard-code individual music apps. Google Maps supports Android intents for opening maps and requesting navigation. Android defines a media play-from-search intent that compatible music apps may handle. This keeps recognition and command interpretation independent of the receiving app. It does **not** guarantee support for every installed media app; an app may ignore or only partly implement the standard request. Saarthi must report that outcome honestly instead of claiming playback started.

App-specific adapters are out of scope for this release. They can be considered later only for a demonstrated gap in a target app. Accessibility-based UI automation and privileged system permissions are not used.

## Components and data flow

1. **Phone voice screen:** A prominent Speak button starts a single six-second recording using the existing microphone permission and `AudioCapture.phone()`. It shows listening, transcribing, recognized words, and the action or error. Existing fuel screens move behind a secondary navigation item; their storage and voice workflow are preserved.
2. **Offline transcription:** Reuse the bundled Whisper engine. The audio and transcript stay on the phone. Transcription is separate from command parsing and action dispatch.
3. **`VoiceCommandParser`:** A pure, deterministic component converts a transcript into one of `OpenMaps`, `Navigate(destination)`, `PlayMusic(query)`, or `Unsupported`. Initial phrasings include “open Maps” / “Maps kholo,” “navigate to X” / “X chalo,” and “play X” / “X bajao.” It does not guess missing destinations or queries.
4. **`CommandDispatcher`:** The Android boundary maps a recognized command to a documented Maps or media-search intent. Maps requests target the Google Maps package and report an error if it is absent; navigation destinations are URI-encoded. Music requests use the standard media-search action without naming a vendor, so Android can select a capable installed app. It returns a clear handled/not-handled result; an unhandled intent never crashes the app.
5. **UI result:** Saarthi shows what it heard and which action it requested. A successful intent launch means the request was handed to another app, **not** that navigation or playback actually began. If Android needs an app chooser, the user completes that choice while parked.

The path is: tap → record → transcribe → parse → dispatch → show result. Unknown or incomplete speech stops before dispatch.

## Errors and safety

- Permission denied, no audio, model failure, and transcription failure show a recoverable error and send no external action.
- Unrecognized, ambiguous, or incomplete commands send no external action.
- Missing Maps or music handlers show an explicit unsupported-action message.
- No arbitrary app-opening command, background listening, notification-listener access, or silent retries are included.
- External Maps and media apps may need network access even though Saarthi's recognition is offline.

## Validation

- Unit-test the parser with literal English and Hinglish commands, missing fields, unrelated speech, and destination/query extraction.
- Test intent construction and the no-handler path without a real car.
- Build the APK and run Android UI tests on an emulator. Do not run `connectedDebugAndroidTest` on the user's phone; that runner previously uninstalled the app and can erase local fuel data.
- Manually test the built app while parked: open Maps, navigate to a named place, request music, and observe a no-handler or unsupported command. Record transcript, chosen action, and elapsed time. Do not claim Android Auto behavior until separately tested in a car.

## Acceptance criteria

On the phone, one tap starts a voice command. Supported commands result in the corresponding Android request; unsupported commands cause no action and explain why. The visible transcript and result do not falsely claim that a receiving app completed the task. Existing fuel entries and fuel logging remain available. No Android Auto connection, wake-word, or universal cross-app-control claim is added.
