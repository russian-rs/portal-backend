package rs.russian.portal.city.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import rs.russian.portal.city.domain.City

@Repository
interface CityRepository : JpaRepository<City, String> {

    @Query("SELECT c FROM City c WHERE c.active = true ORDER BY c.name")
    fun findAllActive(): List<City>
    
    @Query("SELECT c FROM City c WHERE c.active = true AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(c.nameCyrillic) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) ORDER BY c.name")
    fun findByNameOrNameCyrillic(searchTerm: String): List<City>
}
