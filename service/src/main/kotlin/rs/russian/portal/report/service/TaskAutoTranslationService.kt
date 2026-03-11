package rs.russian.portal.report.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import rs.russian.portal.report.domain.Task
import rs.russian.portal.shared.ai.LlmClient
import rs.russian.portal.shared.ai.LlmGenerationRequest
import rs.russian.portal.shared.ai.domain.AiProfileCode
import rs.russian.portal.shared.ai.service.AiProfileService

@Service
class TaskAutoTranslationService(
    private val aiProfileService: AiProfileService,
    private val llmClient: LlmClient
) {

    fun localizeTask(task: Task) {
        if (task.nameSr.isNullOrBlank()) {
            task.nameSr = localizeField(task.name, "name")
        }
        if (task.descriptionSr.isNullOrBlank()) {
            task.descriptionSr = localizeField(task.description, "description")
        }
    }

    private fun localizeField(text: String, field: String): String {
        if (text.isBlank() || isLikelySerbianLatin(text)) {
            return text
        }
        return translateWithFallback(text, field)
    }

    private fun translateWithFallback(text: String, field: String): String {
        return try {
            val profile = aiProfileService.getActiveProfile(AiProfileCode.SERBIAN_TRANSLATOR)
            llmClient.generateText(
                LlmGenerationRequest(
                    systemPrompt = profile.systemPrompt,
                    userPrompt = text,
                    model = profile.model,
                    temperature = profile.temperature
                )
            )
        } catch (ex: Exception) {
            log.warn("Task {} auto-translation failed, keeping original text", field, ex)
            text
        }
    }

    private fun isLikelySerbianLatin(text: String): Boolean {
        if (text.isBlank()) {
            return true
        }
        if (CYRILLIC_REGEX.containsMatchIn(text)) {
            return false
        }
        if (SERBIAN_LATIN_CHARS_REGEX.containsMatchIn(text)) {
            return true
        }

        val words = LETTERS_AND_DIGITS_REGEX.split(text.lowercase())
            .filter { it.length > 1 }
        if (words.isEmpty()) {
            return true
        }

        val hintMatches = words.count { it in SERBIAN_HINT_WORDS }
        return hintMatches >= 2
    }

    companion object {
        private val log = LoggerFactory.getLogger(TaskAutoTranslationService::class.java)
        private val CYRILLIC_REGEX = Regex("[\\u0400-\\u04FF]")
        private val SERBIAN_LATIN_CHARS_REGEX = Regex("[\\u0161\\u0111\\u010D\\u0107\\u017E]", RegexOption.IGNORE_CASE)
        private val LETTERS_AND_DIGITS_REGEX = Regex("[^\\p{L}\\p{N}]+")
        private val SERBIAN_HINT_WORDS = setOf(
            "nije", "jesam", "jeste", "samo", "kada", "zbog", "koji", "koja", "koje",
            "ovo", "ovaj", "ovde", "danas", "sutra", "izmedju", "posle", "pre", "dok"
        )
    }
}
