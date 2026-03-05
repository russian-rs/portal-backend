package rs.russian.portal.shared.translation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OpenRouterTranslationClient(
    @param:Value("\${app.openrouter-api-key}")
    private val token: String
) : TranslationClient {

    private val client = OkHttpClient()
    private val objectMapper = jacksonObjectMapper()

    override fun translate(sourceLang: String, targetLang: String, text: String): String {
        val apiToken = token.takeIf { it.isNotBlank() }
            ?: return text

        val requestBody = objectMapper.writeValueAsString(
            mapOf(
                "model" to "google/gemini-2.5-flash",
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "You are a helpful assistant that translates text. " +
                            "Translate the following $sourceLang text into $targetLang. " +
                            "Only provide the translated text, without any additional explanations or formatting."
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to text
                    )
                ),
                "temperature" to 0.1
            )
        )

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("OpenRouter request failed with HTTP ${response.code}: $responseText")
            }

            val root = objectMapper.readTree(responseText)
            val translatedText = root
                .path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText()
                .trim()

            if (translatedText.isBlank()) {
                throw IllegalStateException("OpenRouter response does not contain choices[0].message.content")
            }

            return translatedText
        }
    }
}
