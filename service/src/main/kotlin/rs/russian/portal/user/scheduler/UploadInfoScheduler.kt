package rs.russian.portal.user.scheduler

import kotlinx.coroutines.runBlocking
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.Resource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.file.service.FileService
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.user.service.AccountService
import java.io.ByteArrayInputStream
import java.io.InputStream

@Component
class UploadInfoScheduler(
    private val s3Service: S3Service,
    private val fileService: FileService,
    private val accountService: AccountService,
) {

    @Scheduled(cron = "-")
    @SchedulerLock(name = "uploadUserInfo")
    fun upload(): Unit = runBlocking {
        val users = s3Service.csv("/users/info.csv", UserUploadInfo::class)
        users.forEach { user ->
            try {
                val account = accountService.getAccountByLogin(user.login)
                if (!user.avatar.isNullOrBlank()) {
                    val avatarResource = downloadAvatar(user.avatar)
                    if (avatarResource != null) {
                        val file = fileService.createFile(avatarResource, account)
                        accountService.setAvatar(account, file.id)
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to upload user (${user.login})", e)
            }
        }
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
        val avatar: String?
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
