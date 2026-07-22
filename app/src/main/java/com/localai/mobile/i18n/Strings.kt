package com.localai.mobile.i18n

/**
 * Simple in-memory localization. Follows the system language by default and can
 * be switched at runtime. Supports English, Turkish and Russian.
 */
enum class Lang(val code: String, val label: String) {
    EN("en", "English"), TR("tr", "Türkçe"), RU("ru", "Русский");
    companion object {
        fun fromCode(c: String?): Lang = entries.firstOrNull { it.code == c?.take(2) } ?: EN
    }
}

object Strings {
    private val en = mapOf(
        "unlock" to "Unlock", "password" to "Password", "setup_title" to "Welcome to LocalAI",
        "setup_desc" to "Everything stays on this device, sealed with AES-256. Nothing is ever synced.",
        "openrouter_key" to "OpenRouter API key", "create_password" to "Create master password",
        "get_key_hint" to "Get a free key at openrouter.ai/keys",
        "start" to "Start", "wrong_password" to "Wrong password",
        "characters" to "Characters", "new_character" to "New character", "settings" to "Settings",
        "type_message" to "Type a message…", "send" to "Send", "listening" to "Listening…",
        "web_search" to "Web search", "generate_image" to "Generate image",
        "theme" to "Theme", "language" to "Language", "model" to "Model",
        "update_key" to "Update API key", "key_expired" to "Your API key is out of credit or invalid. Please update it.",
        "wipe" to "Wipe everything", "wipe_confirm" to "Delete ALL data permanently? This cannot be undone.",
        "commands" to "System commands", "root" to "Root", "termux" to "Termux",
        "name" to "Name", "system_prompt" to "System prompt", "greeting" to "Greeting", "save" to "Save",
        "lock" to "Lock", "voice" to "Voice", "cancel" to "Cancel"
    )
    private val tr = mapOf(
        "unlock" to "Kilidi Aç", "password" to "Parola", "setup_title" to "LocalAI'ye Hoş Geldin",
        "setup_desc" to "Her şey bu cihazda kalır, AES-256 ile şifrelenir. Asla senkronize edilmez.",
        "openrouter_key" to "OpenRouter API anahtarı", "create_password" to "Ana parola oluştur",
        "get_key_hint" to "Ücretsiz anahtar: openrouter.ai/keys",
        "start" to "Başla", "wrong_password" to "Yanlış parola",
        "characters" to "Karakterler", "new_character" to "Yeni karakter", "settings" to "Ayarlar",
        "type_message" to "Bir mesaj yaz…", "send" to "Gönder", "listening" to "Dinleniyor…",
        "web_search" to "Web arama", "generate_image" to "Görsel oluştur",
        "theme" to "Tema", "language" to "Dil", "model" to "Model",
        "update_key" to "API anahtarını güncelle", "key_expired" to "API anahtarınız bitmiş veya geçersiz. Lütfen güncelleyin.",
        "wipe" to "Her şeyi sil", "wipe_confirm" to "TÜM veriler kalıcı silinsin mi? Geri alınamaz.",
        "commands" to "Sistem komutları", "root" to "Root", "termux" to "Termux",
        "name" to "İsim", "system_prompt" to "Sistem promptu", "greeting" to "Karşılama", "save" to "Kaydet",
        "lock" to "Kilitle", "voice" to "Ses", "cancel" to "İptal"
    )
    private val ru = mapOf(
        "unlock" to "Разблокировать", "password" to "Пароль", "setup_title" to "Добро пожаловать в LocalAI",
        "setup_desc" to "Всё хранится на устройстве и шифруется AES-256. Синхронизации нет.",
        "openrouter_key" to "Ключ OpenRouter API", "create_password" to "Создайте мастер-пароль",
        "get_key_hint" to "Бесплатный ключ: openrouter.ai/keys",
        "start" to "Начать", "wrong_password" to "Неверный пароль",
        "characters" to "Персонажи", "new_character" to "Новый персонаж", "settings" to "Настройки",
        "type_message" to "Введите сообщение…", "send" to "Отправить", "listening" to "Слушаю…",
        "web_search" to "Веб-поиск", "generate_image" to "Создать изображение",
        "theme" to "Тема", "language" to "Язык", "model" to "Модель",
        "update_key" to "Обновить ключ API", "key_expired" to "Ключ API исчерпан или недействителен. Обновите его.",
        "wipe" to "Стереть всё", "wipe_confirm" to "Удалить ВСЕ данные навсегда? Это необратимо.",
        "commands" to "Системные команды", "root" to "Root", "termux" to "Termux",
        "name" to "Имя", "system_prompt" to "Системный промпт", "greeting" to "Приветствие", "save" to "Сохранить",
        "lock" to "Заблокировать", "voice" to "Голос", "cancel" to "Отмена"
    )

    fun t(lang: Lang, key: String): String = when (lang) {
        Lang.EN -> en; Lang.TR -> tr; Lang.RU -> ru
    }[key] ?: en[key] ?: key
}
