package com.localai.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localai.mobile.core.Character
import com.localai.mobile.i18n.Lang
import com.localai.mobile.i18n.Strings
import com.localai.mobile.ui.AppViewModel
import com.localai.mobile.ui.Screen
import com.localai.mobile.ui.theme.Themes

@Composable
fun CreateCharacterScreen(vm: AppViewModel, lang: Lang) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var greeting by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🤖") }
    var allowWeb by remember { mutableStateOf(true) }
    var allowImg by remember { mutableStateOf(true) }
    var allowCmd by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarRow(title = Strings.t(lang, "new_character"), left = {
            IconButton(onClick = { vm.goto(Screen.HOME) }, modifier = Modifier.testTag("back-home")) {
                Icon(Icons.Default.ArrowBack, "back", tint = MaterialTheme.colorScheme.onBackground)
            }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(Strings.t(lang, "name")) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("cc-name"))
            OutlinedTextField(emoji, { emoji = it.take(2) }, label = { Text("Avatar (emoji)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(prompt, { prompt = it }, label = { Text(Strings.t(lang, "system_prompt")) },
                minLines = 3, modifier = Modifier.fillMaxWidth().testTag("cc-prompt"))
            OutlinedTextField(greeting, { greeting = it }, label = { Text(Strings.t(lang, "greeting")) },
                modifier = Modifier.fillMaxWidth())
            ToggleRow(Strings.t(lang, "web_search"), allowWeb) { allowWeb = it }
            ToggleRow(Strings.t(lang, "generate_image"), allowImg) { allowImg = it }
            ToggleRow(Strings.t(lang, "commands"), allowCmd) { allowCmd = it }
        }
        Button(onClick = {
            if (name.isBlank() || prompt.isBlank()) return@Button
            vm.saveCharacter(Character(
                id = vm.newCharacterId(), name = name, description = prompt.take(60),
                systemPrompt = prompt, greeting = greeting.ifBlank { "Hi, I'm $name." },
                theme = "mono", builtIn = false, allowCommands = allowCmd,
                allowWebSearch = allowWeb, allowImages = allowImg, avatarEmoji = emoji
            ))
        }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp).testTag("cc-save")) {
            Text(Strings.t(lang, "save"))
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, lang: Lang) {
    val ui by vm.ui.collectAsState()
    var showWipe by remember { mutableStateOf(false) }
    var newKey by remember { mutableStateOf("") }
    var imgEndpoint by remember { mutableStateOf(ui.imageEndpoint) }
    var imgModel by remember { mutableStateOf(ui.imageModel) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarRow(title = Strings.t(lang, "settings"), left = {
            IconButton(onClick = { vm.goto(Screen.HOME) }, modifier = Modifier.testTag("back-home")) {
                Icon(Icons.Default.ArrowBack, "back", tint = MaterialTheme.colorScheme.onBackground)
            }
        })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            Section(Strings.t(lang, "language"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Lang.entries.forEach { l ->
                    FilterChip(selected = ui.lang == l, onClick = { vm.setLang(l) },
                        label = { Text(l.label) }, modifier = Modifier.testTag("lang-${l.code}"))
                }
            }

            Section(Strings.t(lang, "theme"))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Themes.all.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { th ->
                            FilterChip(selected = ui.themeId == th.id, onClick = { vm.setTheme(th.id) },
                                label = { Text(th.label) }, modifier = Modifier.weight(1f).testTag("theme-${th.id}"))
                        }
                    }
                }
            }

            Section(Strings.t(lang, "model"))
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag("model-picker")) {
                    Text(ui.model.ifBlank { "—" }, maxLines = 1)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ui.models.take(60).forEach { m ->
                        DropdownMenuItem(text = {
                            Text((if (m.free) "🆓 " else "") + (if (m.uncensored) "🔞 " else "") + m.name, fontSize = 13.sp)
                        }, onClick = { vm.setModel(m.id); expanded = false })
                    }
                }
            }

            Section(Strings.t(lang, "update_key"))
            OutlinedTextField(newKey, { newKey = it }, label = { Text(Strings.t(lang, "openrouter_key")) },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("settings-key"))
            Button(onClick = { if (newKey.isNotBlank()) { vm.updateToken(newKey); newKey = "" } },
                modifier = Modifier.testTag("settings-key-save")) { Text(Strings.t(lang, "save")) }

            Section(Strings.t(lang, "generate_image"))
            OutlinedTextField(imgEndpoint, { imgEndpoint = it }, label = { Text("Image endpoint") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(imgModel, { imgModel = it }, label = { Text("Image model") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.setImageConfig(imgEndpoint, imgModel) }) { Text(Strings.t(lang, "save")) }

            Divider()
            Button(onClick = { vm.lock() },
                modifier = Modifier.fillMaxWidth().testTag("lock-button")) { Text(Strings.t(lang, "lock")) }
            OutlinedButton(onClick = { showWipe = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().testTag("wipe-button")) { Text(Strings.t(lang, "wipe")) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showWipe) AlertDialog(
        onDismissRequest = { showWipe = false },
        confirmButton = { TextButton(onClick = { showWipe = false; vm.wipe() }, modifier = Modifier.testTag("wipe-confirm")) { Text(Strings.t(lang, "wipe")) } },
        dismissButton = { TextButton(onClick = { showWipe = false }) { Text(Strings.t(lang, "cancel")) } },
        text = { Text(Strings.t(lang, "wipe_confirm")) }
    )
}

@Composable
private fun Section(title: String) {
    Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onBackground)
}
