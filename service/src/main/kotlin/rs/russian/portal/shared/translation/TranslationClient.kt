package rs.russian.portal.shared.translation

interface TranslationClient {
    fun translate(sourceLang: String, targetLang: String, text: String): String
}