package com.localai.mobile.core

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM sealing with a key derived from the master password via
 * PBKDF2-HMAC-SHA256 (200,000 iterations). Mirrors the security model of the
 * original LocalAI project. The derived key lives only in memory (session key)
 * and is never written to disk.
 */
object Crypto {
    private const val ITERATIONS = 200_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val IV_LEN = 12
    private const val SALT_LEN = 16

    fun randomBytes(n: Int): ByteArray = ByteArray(n).also { SecureRandom().nextBytes(it) }

    fun deriveKey(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /** Returns salt||iv||ciphertext (all raw bytes). */
    fun seal(key: SecretKey, plaintext: ByteArray, salt: ByteArray): ByteArray {
        val iv = randomBytes(IV_LEN)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext)
        return salt + iv + ct
    }

    fun open(password: CharArray, blob: ByteArray): ByteArray {
        val salt = blob.copyOfRange(0, SALT_LEN)
        val iv = blob.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = blob.copyOfRange(SALT_LEN + IV_LEN, blob.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    fun openWithKey(key: SecretKey, blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = blob.copyOfRange(SALT_LEN + IV_LEN, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    fun newSalt(): ByteArray = randomBytes(SALT_LEN)
    fun b64(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    fun unb64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
}
