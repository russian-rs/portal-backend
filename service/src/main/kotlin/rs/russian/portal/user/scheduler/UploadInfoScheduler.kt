package rs.russian.portal.user.scheduler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.Resource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.file.service.FileService
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.shared.enums.Gender
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.service.AccountService
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class UploadInfoScheduler(
    private val s3Service: S3Service,
    private val fileService: FileService,
    private val objectMapper: ObjectMapper,
    private val accountService: AccountService,
) {

    @Scheduled(cron = "-")
    fun upload(): Unit = runBlocking {
        val users = s3Service.csv("/users/info.csv", UserUploadInfo::class)
        users.forEach { user ->
            try {
                var account = accountService.findAccountByLogin(user.login) ?: return@forEach
                if (!user.avatar.isNullOrBlank()) {
                    val avatarResource = downloadAvatar(user.avatar)
                    if (avatarResource != null) {
                        val file = fileService.createFile(avatarResource, account)
                        account = accountService.setAvatar(account, file.id)
                    }
                }
                val userInfo = account.info ?: UserInfo.default(account)
                if (!user.city.isNullOrBlank()) {
                    userInfo.city = user.city
                }
                if (user.birthDate != null) {
                    userInfo.birthDate = user.birthDate
                }
                if (!user.gender.isNullOrBlank()) {
                    if (user.gender == "Мужчина") {
                        userInfo.gender = Gender.MALE
                    }
                    if (user.gender == "Женщина") {
                        userInfo.gender = Gender.FEMALE
                    }
                }
                if (!user.telegram.isNullOrBlank()) {
                    val paths = user.telegram.split("/")
                    if (paths.size > 1) {
                        userInfo.telegram = paths.last().replace("@", "")
                    }
                }
                account.info = userInfo
                accountService.save(account)
            } catch (e: Exception) {
                log.error("Failed to upload user (${user.login})", e)
            }
        }
    }

    @Scheduled(cron = "-")
    fun enrich(): Unit = runBlocking {
        val text = s3Service.file("/users/applications.json")
        val applications = objectMapper.readValue(text, object : TypeReference<List<Map<String, String>>>() {})
        applications.forEach { application ->
            try {
                enrichUser(application)
            } catch (e: Exception) {
                log.error("Failed to enrich user ${application["Эл.почта"]}", e)
            }
        }
    }

    private fun enrichUser(data: Map<String, String>) = runBlocking {
        val email = data["E-mail"] ?: return@runBlocking
        val account = accountService.findAccountByEmail(email) ?: return@runBlocking
        val userInfo = account.info
        if (!data["Дата рождения"].isNullOrBlank()) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                userInfo?.birthDate = LocalDate.parse(data["Дата рождения"]!!.substringBefore('T'), formatter)
            } catch (e: Exception) {
            }
        }
        if (!data["Почтовый адрес"].isNullOrBlank()) {
            userInfo?.address = data["Почтовый адрес"]
        }
        if (!data["Telegram"].isNullOrBlank() && data["Telegram"] != "#ERROR!") {
            userInfo?.telegram = data["Telegram"]!!
                .replace("@", "")
                .replace("https://t.me/", "")
                .lowercase()
        }
        if (!data["Телефон"].isNullOrBlank() && data["Телефон"] != "#ERROR!") {
            val str = data["Телефон"]!!
            if (str.length < 6) {
                return@runBlocking
            }
            if (str.startsWith("381")) {
                userInfo?.phone = "+$str"
            }
        }
        account.info = userInfo
        accountService.save(account)
    }

    private fun downloadAvatar(imageUrl: String): Resource? {
        try {
            val name = imageUrl.split("/").last().split("?").first().ifBlank { "avatar.jpg" }
            if (name == "default_avatar.jpg") {
                return null
            }

            // Open connection to the URL
            val url = imageUrl.toHttpUrl().toUrl()
            val connection = url.openConnection()
            connection.connect()

            // Read data from the URL
            val inputStream = connection.getInputStream().readAllBytes()
            return NamedByteArrayResource(inputStream, name)
        } catch (e: Exception) {
            log.error("Failed to download image from URL: $imageUrl", e)
            return null
        }
    }

    data class UserUploadInfo(
        val login: String,
        val avatar: String?,
        val city: String?,
        val birthDate: LocalDate?,
        val gender: String?,
        val telegram: String?
    )

    class NamedByteArrayResource(
        private val byteArray: ByteArray,
        private val resourceName: String
    ) : AbstractResource() {

        override fun getInputStream(): InputStream = ByteArrayInputStream(byteArray)

        override fun getDescription(): String = "Named resource: $resourceName"

        override fun getFilename(): String = resourceName
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
