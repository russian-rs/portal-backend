package rs.russian.portal.security.utils

import org.mindrot.jbcrypt.BCrypt
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

/**
 * Данный класс реализует механизм проверки паролей захэшированных с помощью PHPass,
 * который используется в WordPress. Необходимо для бесшовной миграции пользователей на портал.
 */
class PHPasswordEncoder: PasswordEncoder {

    override fun matches(password: CharSequence?, storedHash: String?): Boolean {
        if (storedHash == null || password == null) {
            return false
        }
        return check(password.toString(), storedHash)
    }

    override fun encode(rawPassword: CharSequence?): String {
        throw NotImplementedError()
    }

    /**
     * Checks if the given password matches the stored hash.
     *
     * @param password the password string to check
     * @param storedHash the stored hash string to compare against
     * @return true if the password matches the stored hash, false otherwise
     */
    private fun check(password: String, storedHash: String): Boolean {
        var hash = cryptPrivate(password, storedHash)
        var md: MessageDigest? = null
        if (hash.startsWith("*")) {
            if (storedHash.startsWith("$6$")) {
                md = try {
                    MessageDigest.getInstance("SHA-512")
                } catch (e: NoSuchAlgorithmException) {
                    null
                }
            }
            if (md == null && storedHash.startsWith("$5$")) {
                md = try {
                    MessageDigest.getInstance("SHA-256")
                } catch (e: NoSuchAlgorithmException) {
                    null
                }
            }
            if (md == null && storedHash.startsWith("$2")) {
                return BCrypt.checkpw(password, storedHash)
            }
            if (md == null && storedHash.startsWith("$1$")) {
                md = try {
                    MessageDigest.getInstance("MD5")
                } catch (e: NoSuchAlgorithmException) {
                    null
                }
            }
            if (md != null) {
                hash = String(md.digest(password.toByteArray()))
            }
        }
        return hash == storedHash
    }

    private fun encode64(source: ByteArray): String {
        val count = 16
        var src = source
        var value: Int
        var output = ""
        var i = 0

        if (src.size < count) {
            val t = ByteArray(count)
            System.arraycopy(src, 0, t, 0, src.size)
            Arrays.fill(t, src.size, count - 1, 0.toByte())
            src = t
        }

        do {
            value = src[i] + (if (src[i] < 0) 256 else 0)
            ++i
            output += ITOA64[value and 63]
            if (i < count) {
                value = value or ((src[i] + (if (src[i] < 0) 256 else 0)) shl 8)
            }
            output += ITOA64[value shr 6 and 63]
            if (i++ >= count) {
                break
            }
            if (i < count) {
                value = value or ((src[i] + (if (src[i] < 0) 256 else 0)) shl 16)
            }
            output += ITOA64[value shr 12 and 63]
            if (i++ >= count) {
                break
            }
            output += ITOA64[value shr 18 and 63]
        } while (i < count)
        return output
    }

    private fun cryptPrivate(password: String, setting: String): String {
        var output = "*0"
        if ((if ((setting.length < 2)) setting else setting.substring(0, 2)).equals(output, ignoreCase = true)) {
            output = "*1"
        }
        val id = if ((setting.length < 3)) setting else setting.substring(0, 3)
        if (!(id == "\$P$" || id == "\$H$")) {
            return output
        }
        val countLog2 = ITOA64.indexOf(setting[3])
        if (countLog2 < 7 || countLog2 > 30) {
            return output
        }
        var count = 1 shl countLog2
        val salt = setting.substring(4, 4 + 8)
        if (salt.length != 8) {
            return output
        }
        val md: MessageDigest
        try {
            md = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            return output
        }
        val pass = password.toByteArray()
        var hash = md.digest((salt + password).toByteArray())
        do {
            val t = ByteArray(hash.size + pass.size)
            System.arraycopy(hash, 0, t, 0, hash.size)
            System.arraycopy(pass, 0, t, hash.size, pass.size)
            hash = md.digest(t)
        } while (--count > 0)
        output = setting.substring(0, 12)
        output += encode64(hash)
        return output
    }

    companion object {
        private const val ITOA64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
    }
}
