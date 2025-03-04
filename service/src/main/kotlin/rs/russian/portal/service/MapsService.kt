package rs.russian.portal.service

import org.springframework.stereotype.Service
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/maps")
@CrossOrigin(origins = ["http://localhost:3000"])  //я хз чо тут указывать
class MapsApi(private val mapsService: MapsService) {

    @GetMapping("/volunteers")
    fun getVolunteers(): List<VolunteerDTO> {
        return mapsService.getVolunteers()
    }

    @GetMapping("/playgrounds")
    fun getReports(): List<ReportDTO> {
        return mapsService.getReports()
    }
}

@Service
class MapsService {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    private val objectMapper = jacksonObjectMapper()

    fun getVolunteers(): List<VolunteerDTO> {
        val result = entityManager.createNativeQuery("""
            SELECT full_name, email, telegram, city, address, latitude, longitude, groups
            FROM maps_users
        """).resultList as List<Array<Any>>

        return result.map { row ->
            VolunteerDTO(
                full_name = row[0] as String,
                email = row[1] as String,
                telegram = row[2] as? String ?: "Нет данных",
                city = row[3] as String,
                address = row[4] as String,
                latitude = (row[5] as Number).toDouble(),
                longitude = (row[6] as Number).toDouble(),
                groups = try {
                    objectMapper.readValue(row[7].toString())
                } catch (e: Exception) {
                    listOf()
                }
            )
        }
    }
    fun getReports(): List<ReportDTO> {
        val result = entityManager.createNativeQuery("""
                SELECT data_id, url_adress, date, photo, pokritie, drenaj, ograjdenie, security, light, lat, lng
                FROM playgrounds
            """).resultList as List<Array<Any>>

        return result.map { row ->
            ReportDTO(
                data_id = row[0] as Int,
                url_adress = row[1] as? String ?: "Нет данных",
                date = row[2] as? String ?: "Неизвестно",
                photo = row[3] as? String ?: "",
                pokritie = row[4] as? String ?: "Нет данных",
                drenaj = row[5] as? String ?: "Нет данных",
                ograjdenie = row[6] as? String ?: "Нет данных",
                security = row[7] as? String ?: "Нет данных",
                light = row[8] as? String ?: "Нет данных",
                latitude = (row[9] as? String)?.toDoubleOrNull() ?: 0.0,
                longitude = (row[10] as? String)?.toDoubleOrNull() ?: 0.0
            )
        }
    }
}




@JsonInclude(JsonInclude.Include.NON_NULL)
data class VolunteerDTO(
    val full_name: String,
    val email: String,
    val telegram: String?,
    val city: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val groups: List<String>
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ReportDTO(
    val data_id: Int,
    val url_adress: String,
    val date: String,
    val photo: String,
    val pokritie: String,
    val drenaj: String,
    val ograjdenie: String,
    val security: String,
    val light: String,
    val latitude: Double,
    val longitude: Double
)