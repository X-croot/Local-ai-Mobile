package com.localai.mobile.core

import android.content.Context
import java.io.File
import javax.crypto.SecretKey

/**
 * Encrypted on-device vault. Everything (OpenRouter token, chats, characters,
 * settings) is stored inside a single AES-256-GCM sealed file. Nothing is ever
 * synced. Wiping the file wipes every trace.
 *
 *   vault.bin  -> sealed JSON payload (chats, characters, settings)
 *   token.bin  -> sealed OpenRouter API token
 *   salt.bin   -> PBKDF2 salt + a verifier used to validate the password
 */
class Vault(private val ctx: Context) {

    private val dir: File get() = File(ctx.filesDir, "localai").apply { mkdirs() }
    private val saltFile get() = File(dir, "salt.bin")
    private val verifierFile get() = File(dir, "verify.bin")
    private val tokenFile get() = File(dir, "token.bin")
    private val dataFile get() = File(dir, "vault.bin")

    var sessionKey: SecretKey? = null
        private set
    private var salt: ByteArray? = null

    val isInitialised: Boolean get() = saltFile.exists() && verifierFile.exists()

    /** First launch: create salt, seal a verifier and the OpenRouter token. */
    fun setup(password: CharArray, openRouterToken: String) {
        val s = Crypto.newSalt()
        val key = Crypto.deriveKey(password, s)
        saltFile.writeBytes(s)
        salt = s
        sessionKey = key
        // verifier: a known marker sealed with the key -> proves the password
        verifierFile.writeBytes(Crypto.seal(key, MARKER.toByteArray(), s))
        setToken(openRouterToken)
        writeData("{\"chats\":[],\"characters\":[],\"settings\":{}}")
    }

    /** Later launches: unlock with password. Returns true on success. */
    fun unlock(password: CharArray): Boolean {
        val s = saltFile.readBytes()
        val key = Crypto.deriveKey(password, s)
        return try {
            val opened = Crypto.openWithKey(key, verifierFile.readBytes())
            if (String(opened) == MARKER) {
                salt = s; sessionKey = key; true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    fun lock() { sessionKey = null }

    fun setToken(token: String) {
        val key = sessionKey ?: return
        tokenFile.writeBytes(Crypto.seal(key, token.toByteArray(), salt!!))
    }

    fun getToken(): String? {
        val key = sessionKey ?: return null
        if (!tokenFile.exists()) return null
        return String(Crypto.openWithKey(key, tokenFile.readBytes()))
    }

    fun readData(): String {
        val key = sessionKey ?: return "{}"
        if (!dataFile.exists()) return "{}"
        return try { String(Crypto.openWithKey(key, dataFile.readBytes())) }
        catch (e: Exception) { "{}" }
    }

    fun writeData(json: String) {
        val key = sessionKey ?: return
        dataFile.writeBytes(Crypto.seal(key, json.toByteArray(), salt!!))
    }

    /** Panic wipe — removes every trace from the device. */
    fun wipeEverything() {
        listOf(saltFile, verifierFile, tokenFile, dataFile).forEach { if (it.exists()) it.delete() }
        sessionKey = null; salt = null
    }

    companion object { private const val MARKER = "LOCALAI_OK" }
}
