package com.localai.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import com.localai.mobile.core.ChatMessage
import com.localai.mobile.i18n.Lang
import com.localai.mobile.i18n.Strings
import com.localai.mobile.ui.AppViewModel
import com.localai.mobile.ui.Screen

@Composable
fun ChatScreen(
    vm: AppViewModel, lang: Lang,
    onStartVoice: () -> Unit, isListening: Boolean, partial: String,
    speak: (String) -> Unit
) {
    val ui by vm.ui.collectAsState()
    val conv = ui.activeConv
    val ch = ui.activeCharacter
    var input by remember { mutableStateOf("") }
    var useWeb by remember { mutableStateOf(false) }
    var imageMode by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(conv?.messages?.size, partial) {
        val n = conv?.messages?.size ?: 0
        if (n > 0) listState.animateScrollToItem(n - 1)
    }
    LaunchedEffect(partial) { if (partial.isNotBlank()) input = partial }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        TopBarRow(title = ch?.name ?: "Chat",
            left = {
                IconButton(onClick = { vm.goto(Screen.HOME) }, modifier = Modifier.testTag("back-home")) {
                    Icon(Icons.Default.ArrowBack, "back", tint = MaterialTheme.colorScheme.onBackground)
                }
            },
            right = {
                IconButton(onClick = { vm.goto(Screen.SETTINGS) }) {
                    Icon(Icons.Default.Settings, "settings", tint = MaterialTheme.colorScheme.onBackground)
                }
            })
        if (ui.keyProblem != null) {
            Text(Strings.t(lang, "key_expired"), color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp, modifier = Modifier.padding(16.dp, 4.dp))
        }

        LazyColumn(state = listState, modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Spacer(Modifier.height(8.dp)) }
            items(conv?.messages ?: emptyList()) { m -> MessageBubble(m) { speak(m.content) } }
            if (ui.busy) item {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("…", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // Composer
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ch?.allowWebSearch == true) FilterChip(selected = useWeb, onClick = { useWeb = !useWeb },
                        label = { Text(Strings.t(lang, "web_search"), fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("web-toggle"))
                    if (ch?.allowImages == true) {
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = imageMode, onClick = { imageMode = !imageMode },
                            label = { Text(Strings.t(lang, "generate_image"), fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Default.Image, null, Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("image-toggle"))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onStartVoice, modifier = Modifier.testTag("voice-button")) {
                        Icon(if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            "voice", tint = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                    OutlinedTextField(
                        value = input, onValueChange = { input = it },
                        placeholder = { Text(if (isListening) Strings.t(lang, "listening") else Strings.t(lang, "type_message")) },
                        modifier = Modifier.weight(1f).testTag("chat-input"), maxLines = 4
                    )
                    IconButton(
                        onClick = {
                            val text = input.trim(); if (text.isEmpty()) return@IconButton
                            input = ""
                            if (imageMode) vm.generateImage(text) else vm.send(text, useWeb) { speak(it) }
                        },
                        enabled = !ui.busy, modifier = Modifier.testTag("send-button")
                    ) { Icon(Icons.Default.Send, "send", tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, onSpeak: () -> Unit) {
    val isUser = m.role == "user"
    val bg = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp)).background(bg).padding(12.dp)
        ) {
            val bmp = m.imageB64?.let { decode(it) }
            if (bmp != null) {
                Image(bmp.asImageBitmap(), "image",
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).padding(bottom = 6.dp),
                    contentScale = ContentScale.FillWidth)
            }
            if (m.content.isNotBlank()) Text(m.content, color = fg, fontSize = 14.sp)
            if (!isUser && m.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Default.VolumeUp, "speak",
                    tint = fg.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp))
                        .padding(2.dp))
            }
        }
    }
}
