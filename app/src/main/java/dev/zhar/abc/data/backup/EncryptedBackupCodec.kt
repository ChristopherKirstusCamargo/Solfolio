package dev.zhar.abc.data.backup

import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Password-protected backup envelope. Portfolio data never leaves the selected file. */
object EncryptedBackupCodec {
    private val magic = byteArrayOf(0x53, 0x4F, 0x4C, 0x36) // SOL6
    private const val iterations = 210_000
    private const val saltSize = 16
    private const val ivSize = 12
    private const val maxPlaintextBytes = 25 * 1024 * 1024

    fun encrypt(plaintext: ByteArray, password: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        require(plaintext.size <= maxPlaintextBytes) { "O backup excede o limite de 25 MB." }
        require(password.size >= 8) { "Use uma senha com pelo menos 8 caracteres." }
        val salt = ByteArray(saltSize).also(random::nextBytes)
        val iv = ByteArray(ivSize).also(random::nextBytes)
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(magic)
        val encrypted = cipher.doFinal(plaintext)
        return ByteBuffer.allocate(magic.size + salt.size + iv.size + encrypted.size)
            .put(magic).put(salt).put(iv).put(encrypted).array()
    }

    fun decrypt(envelope: ByteArray, password: CharArray): ByteArray {
        require(envelope.size in (magic.size + saltSize + ivSize + 16)..(maxPlaintextBytes + 1024)) { "Arquivo de backup inválido." }
        require(password.size >= 8) { "Use a senha criada com o backup." }
        val buffer = ByteBuffer.wrap(envelope)
        val actualMagic = ByteArray(magic.size).also(buffer::get)
        require(actualMagic.contentEquals(magic)) { "Este arquivo não é um backup Solfolio V6." }
        val salt = ByteArray(saltSize).also(buffer::get)
        val iv = ByteArray(ivSize).also(buffer::get)
        val encrypted = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, derive(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD(magic)
        return cipher.doFinal(encrypted)
    }

    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
