package rs.russian.portal.report.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.report.domain.Task
import rs.russian.portal.shared.translation.TranslationClient
import rs.russian.portal.user.scheduler.ContractExpirationScheduler

@Service
class TaskTranslationService(
    private val translationClient: TranslationClient
) {
    private val sourceLang = "Russian"
    private val targetLang = "Serbian-Lat"

    fun translateTask(task: Task) {
        val nameNeeds = task.nameSr.isNullOrBlank()
        val descNeeds = task.descriptionSr.isNullOrBlank()

        if (nameNeeds) {
            task.nameSr = translateOrFallback(task.name, "name")
        }
        if (descNeeds) {
            task.descriptionSr = translateOrFallback(task.description, "description")
        }
    }

    private fun translateOrFallback(text: String, field: String): String {
        return try {
            translationClient.translate(sourceLang, targetLang, text)
        } catch (ex: Exception) {
            log.warn("Task {} translation failed, keeping original text", field, ex)
            text
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ContractExpirationScheduler::class.java)
    }
}
