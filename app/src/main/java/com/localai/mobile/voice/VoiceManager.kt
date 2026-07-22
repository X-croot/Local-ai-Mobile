package com.localai.mobile.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * On-device voice. STT prefers the OFFLINE recognizer so audio is processed
 * locally where the device supports it (no traces). TTS gives the assistant a
 * voice, Google-Assistant style.
 */
class VoiceManager(private val ctx: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun initTts(locale: Locale = Locale.getDefault()) {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = locale
                ttsReady = true
            }
        }
    }

    fun speak(text: String) {
        if (ttsReady) tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "localai-utt")
    }

    fun stopSpeaking() { tts?.stop() }

    val isAvailable: Boolean get() = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun startListening(
        locale: Locale = Locale.getDefault(),
        onPartial: (String) -> Unit = {},
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable) { onError("Speech recognition unavailable"); return }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // local-first, privacy
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onError("STT error $error") }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (text != null) onResult(text) else onError("No speech")
            }
            override fun onPartialResults(partial: Bundle?) {
                partial?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(onPartial)
            }
            override fun onEvent(e: Int, b: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    fun stopListening() { recognizer?.stopListening() }

    fun release() {
        recognizer?.destroy(); recognizer = null
        tts?.stop(); tts?.shutdown(); tts = null; ttsReady = false
    }
}
