package com.customapplock.studio.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing. Stored form: base64(salt):base64(hash). */
object SecretCodec {
    private val rnd = SecureRandom()

    fun hash(secret: String): String {
        val salt = ByteArray(16).also { rnd.nextBytes(it) }
        return Base64.encodeToString(salt, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(derive(salt, secret), Base64.NO_WRAP)
    }

    fun verify(secret: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        return runCatching {
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            MessageDigest.isEqual(derive(salt, secret), expected)
        }.getOrDefault(false)
    }

    private fun derive(salt: ByteArray, s: String): ByteArray {
        val spec = PBEKeySpec(s.trim().lowercase().toCharArray(), salt, 4096, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
    }
}
