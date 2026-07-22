package com.localai.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.localai.mobile.ui.AppViewModel
import com.localai.mobile.ui.Screen
import com.localai.mobile.ui.screens.*
import com.localai.mobile.ui.theme.LocalAITheme
import com.localai.mobile.ui.theme.Themes
import com.localai.mobile.voice.VoiceManager
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()
    private lateinit var voice: VoiceManager

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Wake the screen when launched via ASSIST (Google-Assistant style).
        turnScreenOn()

        voice = VoiceManager(this)
        voice.initTts(Locale.getDefault())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            val ui by vm.ui.collectAsState()
            val theme = Themes.byId(ui.themeId)
            var listening by remember { mutableStateOf(false) }
            var partial by remember { mutableStateOf("") }

            val startVoice: () -> Unit = {
                if (listening) { voice.stopListening(); listening = false }
                else {
                    partial = ""
                    listening = true
                    voice.startListening(
                        locale = Locale.forLanguageTag(ui.lang.code),
                        onPartial = { partial = it },
                        onResult = { listening = false; partial = it },
                        onError = { listening = false }
                    )
                }
            }
            val speak: (String) -> Unit = { voice.speak(it) }

            LocalAITheme(theme) {
                Surface(Modifier.fillMaxSize().background(theme.bg)) {
                    when (ui.screen) {
                        Screen.SETUP -> SetupScreen(vm, ui.lang)
                        Screen.UNLOCK -> UnlockScreen(vm, ui.lang)
                        Screen.HOME -> HomeScreen(vm, ui.lang)
                        Screen.CHAT -> ChatScreen(vm, ui.lang, startVoice, listening, partial, speak)
                        Screen.CREATE -> CreateCharacterScreen(vm, ui.lang)
                        Screen.SETTINGS -> SettingsScreen(vm, ui.lang)
                    }
                }
            }
        }
    }

    private fun turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        @Suppress("DEPRECATION")
        pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "localai:wake"
        ).apply { acquire(3000); release() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::voice.isInitialized) voice.release()
    }
}
