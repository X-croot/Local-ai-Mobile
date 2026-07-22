# LocalAI — Mobile (Android, Kotlin + Jetpack Compose)

Free • Local • Private. A ChatGPT/Google-Assistant style AI app that runs on YOUR
phone. Inspired by and ported from the desktop project
**[X-croot/Local-ai](https://github.com/X-croot/Local-ai)**.

Everything stays on the device, sealed with **AES-256-GCM** (key derived from your
master password via PBKDF2-HMAC-SHA256, 200,000 iterations). Nothing is ever synced.

---

## ✅ Ported from the original (web → mobile)

| Original feature | Mobile status |
|---|---|
| OpenRouter free models (bring-your-own key) | ✅ `core/OpenRouterClient.kt` |
| Master password + AES-256 sealed token/chats | ✅ `core/Crypto.kt`, `core/Vault.kt` |
| Encrypted local chat history, no sync | ✅ single encrypted `vault.bin` |
| AI characters (Assistant default + Coder + Companion) | ✅ `core/DefaultCharacters.kt` |
| Create your own character (prompt, greeting, avatar, permissions) | ✅ Create screen |
| Granular permissions (web / images / commands) | ✅ per character |
| Model picker (free-first, uncensored filter) | ✅ Settings |
| Multiple themes (Mono, Dark, Light, Midnight, Terminal, Sakura, Rose) | ✅ `ui/theme/Theme.kt` |
| DuckDuckGo web search (no API key) | ✅ `core/WebSearch.kt` |
| Sandboxed command execution | ✅ via Termux / root — see below |

## ➕ New mobile-only features you asked for

- **Image generation** — configurable OpenAI-compatible endpoint (`core/ImageGen.kt`). Result stored as base64 inside the encrypted vault (no traces on disk).
- **Voice-to-text (speech)** — `voice/VoiceManager.kt`, prefers the **offline** recognizer for privacy.
- **AI talks back (TTS)** — Google-Assistant-style spoken replies.
- **Multi-language UI** — follows system language, switchable between **English / Türkçe / Русский**.
- **Screen wake on launch** — opens over the lock screen when triggered as Assistant (`android.intent.action.ASSIST`).
- **Panic wipe** — one tap deletes every trace permanently.
- **API-key runs out?** — the app never crashes: it detects `401 / 402 / 429` and shows an inline "Update your API key" banner instead of asking mid-chat.

## 🔐 System commands (root / Termux) — graceful by design
`system/SystemTools.kt`
- **Root** via **libsu** (`com.github.topjohnwu.libsu`). If the device is not rooted, `hasRoot()` returns false and nothing crashes.
- **Termux**: commands are dispatched via the `com.termux.RUN_COMMAND` intent. If Termux is not installed, the app returns a friendly message — **no crash**.
- Requires the user to set `allow-external-apps=true` in Termux's config for command dispatch.

> Note on "unlock the phone by voice": Android's secure keyguard **cannot** be bypassed programmatically by any app (OS security). This app therefore **wakes the screen** and offers **voice-driven in-app unlock** with your master password — the safe, allowed equivalent.

---

## 🛠️ Build & run

Requires **Android Studio (Koala or newer)** + JDK 17.

```bash
# 1. Open the folder in Android Studio (it auto-creates the Gradle wrapper jar),
#    OR from a terminal with Gradle 8.9 installed:
gradle wrapper
./gradlew assembleDebug        # builds app/build/outputs/apk/debug/app-debug.apk
```

Install the APK on your phone, open the app:
1. Paste your **free OpenRouter key** (get one at https://openrouter.ai/keys).
2. Create a **master password** — this encrypts everything.
3. Pick a character and start chatting / talking / generating images.

### Language / stack
- **Kotlin** + **Jetpack Compose** (Material 3) — the modern native Android language.
- Networking: OkHttp. HTML parsing: Jsoup. Root: libsu.
- Min SDK 26, Target SDK 34.

## Project layout
```
app/src/main/java/com/localai/mobile/
  MainActivity.kt              single-activity Compose host, screen wake, mic permission
  core/
    Crypto.kt                  PBKDF2 -> AES-256-GCM
    Vault.kt                   encrypted on-device store (+ panic wipe)
    Models.kt / DefaultCharacters.kt
    OpenRouterClient.kt        models + chat, key-expiry detection
    WebSearch.kt               DuckDuckGo, no API key
    ImageGen.kt                OpenAI-compatible image endpoint
  voice/VoiceManager.kt        offline-first STT + TTS
  system/SystemTools.kt        root (libsu) + Termux, graceful
  i18n/Strings.kt              EN / TR / RU
  ui/
    theme/Theme.kt             7 themes
    AppViewModel.kt            state + persistence
    screens/                   Unlock, Setup, Home, Chat, Create, Settings
```

## Security notes
- The derived AES key lives only in memory and is never written to disk.
- Chats, token, characters and settings are all inside one sealed file.
- Forget the password → data is unrecoverable (by design), just like the original.

MIT — a mobile port in the spirit of X-croot/Local-ai.
