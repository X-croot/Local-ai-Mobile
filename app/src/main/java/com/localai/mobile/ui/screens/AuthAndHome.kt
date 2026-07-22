package com.localai.mobile.ui.screens

import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localai.mobile.core.Character
import com.localai.mobile.core.ChatMessage
import com.localai.mobile.i18n.Lang
import com.localai.mobile.i18n.Strings
import com.localai.mobile.ui.AppViewModel
import com.localai.mobile.ui.Screen
import com.localai.mobile.ui.theme.Themes


@Composable
fun UnlockScreen(vm: AppViewModel, lang: Lang) {
    val ui by vm.ui.collectAsState()
    var pw by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AiLogo(72)
        Spacer(Modifier.height(20.dp))
        Text("LocalAI", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(Strings.t(lang, "setup_desc"), textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = pw, onValueChange = { pw = it },
            label = { Text(Strings.t(lang, "password")) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true, modifier = Modifier.fillMaxWidth().testTag("unlock-password")
        )
        if (ui.error != null) { Text(Strings.t(lang, ui.error!!), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.unlock(pw) }, modifier = Modifier.fillMaxWidth().height(52.dp).testTag("unlock-button")) {
            Text(Strings.t(lang, "unlock"))
        }
    }
}

@Composable
fun SetupScreen(vm: AppViewModel, lang: Lang) {
    var pw by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AiLogo(64)
        Spacer(Modifier.height(16.dp))
        Text(Strings.t(lang, "setup_title"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(Strings.t(lang, "setup_desc"), textAlign = TextAlign.Center, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = token, onValueChange = { token = it },
            label = { Text(Strings.t(lang, "openrouter_key")) }, singleLine = true,
            supportingText = { Text(Strings.t(lang, "get_key_hint"), fontSize = 11.sp) },
            modifier = Modifier.fillMaxWidth().testTag("setup-token"))
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = pw, onValueChange = { pw = it },
            label = { Text(Strings.t(lang, "create_password")) },
            visualTransformation = PasswordVisualTransformation(), singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("setup-password"))
        if (err) Text(Strings.t(lang, "wrong_password"), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        Spacer(Modifier.height(20.dp))
        Button(onClick = { vm.setup(pw, token) { ok -> err = !ok } },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("setup-start")) {
            Text(Strings.t(lang, "start"))
        }
    }
}

@Composable
fun HomeScreen(vm: AppViewModel, lang: Lang) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarRow(title = "LocalAI", right = {
            IconButton(onClick = { vm.goto(Screen.SETTINGS) }, modifier = Modifier.testTag("open-settings")) {
                Icon(Icons.Default.Settings, "settings", tint = MaterialTheme.colorScheme.onBackground)
            }
        })
        if (ui.keyProblem != null) { KeyBanner(vm, lang) }
        Text(Strings.t(lang, "characters"), fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(20.dp, 8.dp))
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(ui.characters) { c ->
                CharacterCard(c) { vm.openCharacter(c) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        Button(onClick = { vm.goto(Screen.CREATE) },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("new-character")) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(Strings.t(lang, "new_character"))
        }
    }
}

@Composable
private fun CharacterCard(c: Character, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("char-${c.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center) { Text(c.avatarEmoji, fontSize = 24.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(c.description, fontSize = 12.sp, maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun KeyBanner(vm: AppViewModel, lang: Lang) {
    var show by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(Strings.t(lang, "key_expired"), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp)
            if (!show) TextButton(onClick = { show = true }, modifier = Modifier.testTag("update-key-btn")) {
                Text(Strings.t(lang, "update_key"))
            } else {
                OutlinedTextField(value = key, onValueChange = { key = it },
                    label = { Text(Strings.t(lang, "openrouter_key")) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("update-key-field"))
                Button(onClick = { vm.updateToken(key); show = false }, modifier = Modifier.padding(top = 8.dp)) {
                    Text(Strings.t(lang, "save"))
                }
            }
        }
    }
}
