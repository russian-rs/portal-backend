package rs.russian.portal.maps.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import rs.russian.portal.config.NominatimProperties
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Service
class GeocodingService(
    private val objectMapper: ObjectMapper,
    private val restTemplate: RestTemplate,
    private val nominatimProperties: NominatimProperties
) {

    fun getCoordinates(address: String): Pair<Double, Double>? {
        val cleanedAddress = cleanAddress(address)
        val encodedAddress = URLEncoder.encode(cleanedAddress, StandardCharsets.UTF_8.toString())
        val url = "${nominatimProperties.baseUrl}/search?q=$encodedAddress&format=json&limit=1"

        return try {
            val headers = HttpHeaders()
            headers["User-Agent"] = "KotlinGeocoder"
            val entity = HttpEntity<String>(headers)
            val response = restTemplate.exchange(url, HttpMethod.GET, entity, String::class.java)
            val rootNode: JsonNode = objectMapper.readTree(response.body)
            if (rootNode.isArray && rootNode.size() > 0) {
                val lat = rootNode[0]["lat"].asDouble()
                val lon = rootNode[0]["lon"].asDouble()
                Pair(lat, lon)
            } else {
                log.error("Не удалось найти координаты для: $cleanedAddress")
                null
            }
        } catch (e: Exception) {
            log.error("Ошибка геокодирования: $cleanedAddress - ${e.message}")
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

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
