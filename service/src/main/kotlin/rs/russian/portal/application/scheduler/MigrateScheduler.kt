package rs.russian.portal.application.scheduler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.ApplicationStatus
import rs.russian.portal.application.domain.ApplicationType
import rs.russian.portal.application.service.ApplicationService
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.user.service.AccountService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class MigrateScheduler(
    private val s3Service: S3Service,
    private val objectMapper: ObjectMapper,
    private val applicationService: ApplicationService,
    private val accountService: AccountService
) {

    @Scheduled(cron = "-")
    fun run(): Unit = runBlocking {
        val text = s3Service.file("/users/applications.json")
        val applications = objectMapper.readValue(text, object : TypeReference<List<ApplicationObj>>() {})
        applications.forEach { application ->
            try {
                migrate(application.data)
            } catch (e: Exception) {
                log.error("Failed to migrate application ${application.id}", e)
            }
        }
    }

    private fun migrate(data: Map<String, String>) {
        val status = data["application_status"] ?: "Новый"
        if (status == "Акцепт" || status == "Удален" || status == "Истек срок" || status == "Отказ" || status == "Вышел" || status == "Приостановка" || status == "Продление") {
            return
        }
        val email = data["your-email"]!!
        val name = data["name-surname"]!!
        val application = Application(email = email, name = name)
        val existUser = accountService.findAccountByEmail(email)
        if (existUser != null) {
            application.type = ApplicationType.PROLONGATION
        }
        if (data["submit_time"] != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                application.created = LocalDateTime.parse(data["submit_time"], formatter)
            } catch (e: Exception) {
                log.error("Failed to parse created ${data["submit_time"]} for $email")
            }
        }
        if (status == "Проверка") {
            application.status = ApplicationStatus.IN_PROGRESS
        }
        if (status == "Уточнение") {
            application.status = ApplicationStatus.CLARIFICATION
        }
        if (status == "Подготовка") {
            application.status = ApplicationStatus.IN_PROGRESS
        }
        if (status == "К отправке") {
            application.status = ApplicationStatus.READY_TO_SEND
        }
        if (status == "Отправлено") {
            application.status = ApplicationStatus.DOCS_SENT
        }
        if (status == "Получено") {
            application.status = ApplicationStatus.DOCS_RECEIVED
        }
        if (data["patronymic"] != null) {
            application.patronymic = data["patronymic"]!!
        }
        if (data["date-of-birth"] != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                application.birthDate = LocalDate.parse(data["date-of-birth"], formatter)
            } catch (e: Exception) {
                log.error("Failed to parse birthDate ${data["date-of-birth"]} for $email")
            }
        }
        application.passport = data["passport-number"]
        application.citizenship = data["citizenship"]
        if (!data["telegram-another"].isNullOrBlank()) {
            application.telegram = data["telegram-another"]!!
                .replace("@", "")
                .replace("https://t.me/", "")
                .replace("http://t.me/", "")
                .lowercase()
        }
        application.inSerbia = data["in-serbia"] == "Да"
        if (data["date-of-entrance"] != null) {
            try {
                val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                application.enterDate = LocalDate.parse(data["date-of-entrance"], formatter)
            } catch (e: Exception) {
                log.error("Failed to parse enterDate ${data["date-of-entrance"]} for $email")
            }
        }
        application.address = data["postal-address"]
        if (data["contact-phone"] != null) {
            if (data["contact-phone"]!!.startsWith("381")) {
                application.phone = "+" + data["contact-phone"]!!
            } else {
                application.phone = data["contact-phone"]
            }
        }
        application.residenceRequired = data["vnj-volunteer"]?.contains("Да")
        application.occupation = data["work"]
        application.hasExperience = data["volunteer"] == "Да"
        application.experience = data["volunteer-comment"]

        val languages = mutableListOf<String>()
        if (data["checkbox-english"] != null) {
            languages.add("Английский - ${data["checkbox-english"]}")
        }
        if (data["checkbox-serbian"] != null) {
            languages.add("Сербский - ${data["checkbox-serbian"]}")
        }
        if (data["another-lang"] != null) {
            languages.add(data["another-lang"]!!)
        }
        application.languages = languages.joinToString(", ")

        val skills = mutableListOf<String>()
        if (data["checkbox-skills"] != null) {
            skills.add(data["checkbox-skills"]!!)
        }
        if (data["another-skills"] != null) {
            skills.add(data["another-skills"]!!)
        }
        application.skills = skills.joinToString(", ")

        applicationService.save(application)
    }

    data class ApplicationObj(
        var id: String,
        var data: Map<String, String>
    )

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
