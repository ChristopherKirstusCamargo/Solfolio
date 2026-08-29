package dev.zhar.abc.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test fun `encrypted backup round trips without exposing plaintext`() {
        val source = "{\"portfolio\":\"Principal\",\"balance\":123.45}".encodeToByteArray()
        val encrypted = EncryptedBackupCodec.encrypt(source, "senha-forte-v6".toCharArray())
        assertFalse(encrypted.toString(Charsets.UTF_8).contains("Principal"))
        assertArrayEquals(source, EncryptedBackupCodec.decrypt(encrypted, "senha-forte-v6".toCharArray()))
    }

    @Test(expected = Exception::class)
    fun `wrong password cannot authenticate backup`() {
        val encrypted = EncryptedBackupCodec.encrypt("dados".encodeToByteArray(), "senha-correta".toCharArray())
        EncryptedBackupCodec.decrypt(encrypted, "senha-errada".toCharArray())
    }
}
