# STONIC AI — Phase 1

Native Android app foundation for Android 10+ (minSdk 29). Phase 1 includes Gemini text chat, speech-to-text through Android's installed recognition provider, Android TextToSpeech replies, local SQLite chat history, encrypted API-key/settings storage, assistant name/language/personality/voice settings, and the four user-provided avatar images.

## Requirements
- Android Studio / Android SDK with API 35 installed to build locally; Java 17.
- Internet connection for Gemini requests.
- A Gemini API key. Create/manage it in Google AI Studio. Usage is subject to Google's current quotas and terms; free usage is not unlimited.
- Speech recognition availability depends on the phone and its installed speech service. Voice input may require internet and language packs.

## Build on GitHub from a phone
1. Create a new GitHub repository.
2. Upload the **contents** of this folder (the `app`, `.github`, `gradle` folders and root Gradle files), not an extra enclosing folder.
3. Open the repository's **Actions** tab and run **Build STONIC AI APK** (or push to `main`/`master`).
4. Open the completed workflow run, scroll to **Artifacts**, and download `StonicAI-debug-apk`.
5. Extract the artifact ZIP and install the APK. Android may ask you to allow installs from your browser/file manager.

The workflow uses a hosted Gradle installation and creates a wrapper if the wrapper JAR is absent. The wrapper JAR is not checked into this ZIP.

## First launch
1. Open Settings in STONIC.
2. Paste your Gemini API key and save it. It is stored with Android Keystore-backed encrypted preferences on the device.
3. Choose response language, personality and voice settings.
4. Return to Chat and send a message. Tap the microphone to dictate; grant microphone permission if prompted.

## Current scope / honest limitations
- This is **Phase 1**, not the full always-on phone-control assistant.
- No Hey Stonic wake word, always-listening background service, accessibility automation, WhatsApp actions, alarms or notification reading are implemented yet.
- The microphone button opens Android's speech-recognition UI; it is not a hotword listener.
- Female voice selection depends on the TTS voices installed on the device. Android's TTS engine/voice may be changed in phone settings; this app controls speech rate and on/off.
- Gemini model/network availability, API quotas and billing depend on the user's Google account and region.
- Chat history is stored locally in SQLite. Clearing chat removes the local conversation history.
- API calls send conversation context to Google's Gemini API. Do not send secrets or sensitive personal information.

## Project
- Kotlin + Jetpack Compose + Material 3
- Android minSdk 29 / targetSdk 35
- Gemini REST API via HTTPS
- SQLite local conversation history
- Android SpeechRecognizer + TextToSpeech
- EncryptedSharedPreferences backed by Android Keystore
