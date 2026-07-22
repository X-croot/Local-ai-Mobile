package com.localai.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localai.mobile.core.*
import com.localai.mobile.i18n.Lang
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

enum class Screen { UNLOCK, SETUP, HOME, CHAT, CREATE, SETTINGS }

data class UiState(
    val screen: Screen = Screen.UNLOCK,
    val lang: Lang = Lang.EN,
    val themeId: String = "mono",
    val model: String = "",
    val models: List<OpenRouterClient.Model> = emptyList(),
    val characters: List<Character> = emptyList(),
    val activeCharacter: Character? = null,
    val activeConv: Conversation? = null,
    val busy: Boolean = false,
    val error: String? = null,
    val keyProblem: String? = null,          // non-null => prompt to update key
    val imageEndpoint: String = "https://openrouter.ai/api/v1/images/generations",
    val imageModel: String = "openai/dall-e-3"
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val vault = Vault(app)
    private val client = OpenRouterClient { vault.getToken() }

    private val _ui = MutableStateFlow(
        UiState(
            screen = if (vault.isInitialised) Screen.UNLOCK else Screen.SETUP,
            lang = Lang.fromCode(java.util.Locale.getDefault().language)
        )
    )
    val ui: StateFlow<UiState> = _ui

    private val conversations = mutableListOf<Conversation>()

    // ---- Auth ----
    fun setup(password: String, token: String, onDone: (Boolean) -> Unit) {
        if (password.length < 4 || token.isBlank()) { onDone(false); return }
        vault.setup(password.toCharArray(), token.trim())
        seedCharacters()
        persist()
        _ui.value = _ui.value.copy(screen = Screen.HOME)
        loadModels(); onDone(true)
    }

    fun unlock(password: String) {
        if (vault.unlock(password.toCharArray())) {
            loadData()
            _ui.value = _ui.value.copy(screen = Screen.HOME, error = null)
            loadModels()
        } else {
            _ui.value = _ui.value.copy(error = "wrong_password")
        }
    }

    fun lock() { vault.lock(); conversations.clear(); _ui.value = UiState(screen = Screen.UNLOCK, lang = _ui.value.lang) }

    fun wipe() { vault.wipeEverything(); conversations.clear(); _ui.value = UiState(screen = Screen.SETUP, lang = _ui.value.lang) }

    // ---- Persistence ----
    private fun seedCharacters() {
        _ui.value = _ui.value.copy(characters = DefaultCharacters.all.toList())
    }

    private fun loadData() {
        val root = JSONObject(vault.readData())
        val chars = ArrayList<Character>()
        root.optJSONArray("characters")?.let { for (i in 0 until it.length()) chars.add(Serial.charFromJson(it.getJSONObject(i))) }
        if (chars.none { it.builtIn }) chars.addAll(0, DefaultCharacters.all)
        conversations.clear()
        root.optJSONArray("chats")?.let { for (i in 0 until it.length()) conversations.add(Serial.convFromJson(it.getJSONObject(i))) }
        val s = root.optJSONObject("settings") ?: JSONObject()
        _ui.value = _ui.value.copy(
            characters = chars,
            themeId = s.optString("theme", "mono"),
            model = s.optString("model", ""),
            lang = Lang.fromCode(s.optString("lang", _ui.value.lang.code)),
            imageEndpoint = s.optString("imageEndpoint", _ui.value.imageEndpoint),
            imageModel = s.optString("imageModel", _ui.value.imageModel)
        )
    }

    private fun persist() {
        val root = JSONObject()
        val chats = org.json.JSONArray(); conversations.forEach { chats.put(Serial.convToJson(it)) }
        val chars = org.json.JSONArray(); _ui.value.characters.forEach { chars.put(Serial.charToJson(it)) }
        val s = JSONObject()
            .put("theme", _ui.value.themeId).put("model", _ui.value.model)
            .put("lang", _ui.value.lang.code)
            .put("imageEndpoint", _ui.value.imageEndpoint).put("imageModel", _ui.value.imageModel)
        root.put("chats", chats).put("characters", chars).put("settings", s)
        vault.writeData(root.toString())
    }

    // ---- Settings ----
    fun setTheme(id: String) { _ui.value = _ui.value.copy(themeId = id); persist() }
    fun setLang(l: Lang) { _ui.value = _ui.value.copy(lang = l); persist() }
    fun setModel(m: String) { _ui.value = _ui.value.copy(model = m); persist() }
    fun setImageConfig(endpoint: String, model: String) {
        _ui.value = _ui.value.copy(imageEndpoint = endpoint, imageModel = model); persist()
    }
    fun updateToken(token: String) { vault.setToken(token.trim()); _ui.value = _ui.value.copy(keyProblem = null); loadModels() }
    fun goto(s: Screen) { _ui.value = _ui.value.copy(screen = s, error = null) }

    // ---- Models ----
    fun loadModels() = viewModelScope.launch {
        try {
            val models = withContext(Dispatchers.IO) { client.listModels() }
            val chosen = if (_ui.value.model.isNotBlank()) _ui.value.model else models.firstOrNull()?.id ?: ""
            _ui.value = _ui.value.copy(models = models, model = chosen, keyProblem = null)
        } catch (e: OpenRouterClient.KeyError) {
            _ui.value = _ui.value.copy(keyProblem = e.kind)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(error = e.message)
        }
    }

    // ---- Characters ----
    fun openCharacter(c: Character) {
        val existing = conversations.lastOrNull { it.characterId == c.id }
        val conv = existing ?: Conversation(UUID.randomUUID().toString(), c.id, c.name).also {
            it.messages.add(ChatMessage("assistant", c.greeting))
            conversations.add(it)
        }
        _ui.value = _ui.value.copy(activeCharacter = c, activeConv = conv, screen = Screen.CHAT)
    }

    fun saveCharacter(c: Character) {
        val list = _ui.value.characters.toMutableList()
        val idx = list.indexOfFirst { it.id == c.id }
        if (idx >= 0) list[idx] = c else list.add(c)
        _ui.value = _ui.value.copy(characters = list, screen = Screen.HOME); persist()
    }

    fun newCharacterId() = UUID.randomUUID().toString()

    // ---- Chat ----
    fun send(text: String, useWeb: Boolean, onSpeak: (String) -> Unit) = viewModelScope.launch {
        val conv = _ui.value.activeConv ?: return@launch
        val ch = _ui.value.activeCharacter ?: return@launch
        conv.messages.add(ChatMessage("user", text))
        _ui.value = _ui.value.copy(busy = true, error = null, activeConv = conv)
        try {
            val history = ArrayList<ChatMessage>()
            history.add(ChatMessage("system", ch.systemPrompt))
            if (useWeb && ch.allowWebSearch) {
                val ctxText = withContext(Dispatchers.IO) { WebSearch.asContext(WebSearch.search(text)) }
                if (ctxText.isNotBlank()) history.add(ChatMessage("system", "Web results:\n$ctxText"))
            }
            history.addAll(conv.messages.filter { it.role != "system" })
            val reply = withContext(Dispatchers.IO) { client.chat(_ui.value.model, history) }
            conv.messages.add(ChatMessage("assistant", reply))
            if (conv.title == ch.name && conv.messages.size <= 3) conv.title = text.take(40)
            _ui.value = _ui.value.copy(busy = false, activeConv = conv); persist()
            onSpeak(reply)
        } catch (e: OpenRouterClient.KeyError) {
            conv.messages.removeAll { it.content == text && it.role == "user" }
            _ui.value = _ui.value.copy(busy = false, keyProblem = e.kind, activeConv = conv)
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(busy = false, error = e.message)
        }
    }

    fun generateImage(prompt: String) = viewModelScope.launch {
        val conv = _ui.value.activeConv ?: return@launch
        val token = vault.getToken() ?: return@launch
        conv.messages.add(ChatMessage("user", "🎨 $prompt"))
        _ui.value = _ui.value.copy(busy = true, activeConv = conv)
        try {
            val b64 = withContext(Dispatchers.IO) {
                ImageGen(_ui.value.imageEndpoint, token, _ui.value.imageModel).generate(prompt)
            }
            conv.messages.add(ChatMessage("assistant", "Here is your image:", imageB64 = b64))
            _ui.value = _ui.value.copy(busy = false, activeConv = conv); persist()
        } catch (e: Exception) {
            _ui.value = _ui.value.copy(busy = false, error = e.message)
        }
    }
}
