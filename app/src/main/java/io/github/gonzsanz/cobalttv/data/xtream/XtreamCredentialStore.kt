package io.github.gonzsanz.cobalttv.data.xtream

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the Xtream username and password encrypted with an Android Keystore
 * AES key. The encrypted values live in private SharedPreferences; the key
 * never leaves the keystore.
 */
class XtreamCredentialStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(username: String, password: String) {
        try {
            val user = encrypt(username)
            val pass = encrypt(password)
            val saved = prefs.edit()
                .putString(KEY_USERNAME, user)
                .putString(KEY_PASSWORD, pass)
                .commit()
            if (!saved) {
                throw XtreamException(
                    "No se pudieron guardar las credenciales de Xtream en el dispositivo.",
                )
            }
        } catch (error: XtreamException) {
            throw error
        } catch (error: Exception) {
            throw XtreamException(
                "No se pudieron guardar las credenciales de Xtream de forma segura.",
            )
        }
    }

    /** Returns null when no credentials are stored or they can no longer be read. */
    fun load(): StoredXtreamCredentials? {
        val encryptedUser = prefs.getString(KEY_USERNAME, null) ?: return null
        val encryptedPass = prefs.getString(KEY_PASSWORD, null) ?: return null
        return try {
            StoredXtreamCredentials(
                username = decrypt(encryptedUser),
                password = decrypt(encryptedPass),
            )
        } catch (error: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().clear().commit()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(cipher.iv.size + encrypted.size)
        cipher.iv.copyInto(combined, destinationOffset = 0)
        encrypted.copyInto(combined, destinationOffset = cipher.iv.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        if (combined.size <= IV_SIZE) throw IllegalStateException("Credencial incompleta")
        val iv = combined.copyOfRange(0, IV_SIZE)
        val encrypted = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_BITS, iv),
        )
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "cobalt_xtream_credentials"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "cobalt_xtream_credentials_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}

data class StoredXtreamCredentials(
    val username: String,
    val password: String,
) {
    override fun toString(): String = "StoredXtreamCredentials(redacted)"
}
