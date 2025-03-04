package rs.russian.portal.service

import org.springframework.stereotype.Service
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class GeocodingService {

    private val nominatimUrl = "https://nominatim.openstreetmap.org/search"

    fun getCoordinates(address: String): Pair<Double, Double>? {
        val cleanedAddress = cleanAddress(address)
        val encodedAddress = URLEncoder.encode(cleanedAddress, StandardCharsets.UTF_8.toString())

        return try {
            val url = URL("$nominatimUrl?q=$encodedAddress&format=json&limit=1")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "KotlinGeocoder")

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val objectMapper = jacksonObjectMapper()
            val rootNode: JsonNode = objectMapper.readTree(response)

            if (rootNode.isArray && rootNode.size() > 0) {
                val lat = rootNode[0]["lat"].asDouble()
                val lon = rootNode[0]["lon"].asDouble()
                Pair(lat, lon)
            } else {
                println("Не удалось найти координаты для: $cleanedAddress")
                null
            }
        } catch (e: Exception) {
            println("Ошибка геокодирования: $cleanedAddress - ${e.message}")
            null
        }
    }

    private fun cleanAddress(address: String): String {
        val parts = address.split(",").map { it.trim() }
        return if (parts.size > 2) {
            parts.take(3).joinToString(", ")
                .replace(Regex("(stan|flat|апартамент)\\s*\\d*", RegexOption.IGNORE_CASE), "").trim()
        } else {
            address
        }
    }
}