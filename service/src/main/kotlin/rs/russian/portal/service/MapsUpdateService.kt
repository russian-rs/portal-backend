package rs.russian.portal.service

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.Query
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val objectMapper = jacksonObjectMapper()

@Service
class MapsUpdateService(private val geocodingService: GeocodingService) {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Scheduled(fixedRate = 300000) // Запуск каждые 5 минут
    @Transactional
    fun updateMapsUsersTable() {
        try {
            val query: Query = entityManager.createNativeQuery("""
                SELECT ui.city, ui.address, a.full_name, a.email, ui.telegram, a.groups
                FROM public.account a
                JOIN user_info ui ON a.username = ui.username
                WHERE a.active = true
            """)

            val users = query.resultList as List<Array<Any>>

            for (user in users) {
                val city = user[0] as? String ?: "Неизвестно"
                val address = user[1] as? String ?: "Неизвестно"
                val fullName = user[2] as? String ?: "Неизвестно"
                val email = user[3] as? String ?: "unknown@example.com"
                val telegram = user[4] as? String ?: "Нет данных"
                val groups = user[5] as? String ?: "[]"
                val jsonGroups = objectMapper.readValue<List<String>>(groups)
                val fullAddress = "$city, $address"
                val coordinates = geocodingService.getCoordinates(fullAddress)
                val latitude = coordinates?.first ?: 0.0
                val longitude = coordinates?.second ?: 0.0

                entityManager.createNativeQuery("""
        INSERT INTO maps_users (city, address, full_name, email, telegram, groups, latitude, longitude, last_updated)
        VALUES (:city, :address, :full_name, :email, :telegram, CAST(:groups AS jsonb), :latitude, :longitude, NOW())
        ON CONFLICT (email) DO UPDATE 
        SET city = EXCLUDED.city, 
            address = EXCLUDED.address,
            full_name = EXCLUDED.full_name,
            telegram = EXCLUDED.telegram,
            groups = EXCLUDED.groups, -- ✅ Теперь `groups` обновляется правильно
            latitude = EXCLUDED.latitude,
            longitude = EXCLUDED.longitude,
            last_updated = NOW();
    """)
                    .setParameter("city", city)
                    .setParameter("address", address)
                    .setParameter("full_name", fullName)
                    .setParameter("email", email)
                    .setParameter("telegram", telegram)
                    .setParameter("groups", objectMapper.writeValueAsString(jsonGroups)) // ✅ Передаём JSON, а не пустой список
                    .setParameter("latitude", latitude)
                    .setParameter("longitude", longitude)
                    .executeUpdate()
            }

            println("✅ Таблица maps_users обновлена.")
        } catch (e: Exception) {
            println("❌ Ошибка в updateMapsUsersTable: ${e.message}")
        }
    }
}