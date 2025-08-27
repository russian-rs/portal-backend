package rs.russian.portal.city.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import rs.russian.portal.city.domain.City

@Repository
interface CityRepository : JpaRepository<City, String> {

    @Query("SELECT c FROM City c WHERE c.active = true ORDER BY c.name")
    fun findAllActive(): List<City>

    fun findByNameIgnoreCase(name: String): City?
    
    @Query("SELECT c FROM City c WHERE LOWER(c.name) = LOWER(:searchTerm) OR LOWER(c.nameCyrillic) = LOWER(:searchTerm)")
    fun findByNameOrNameCyrillic(searchTerm: String): City?
}
