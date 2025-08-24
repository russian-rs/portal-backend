package rs.russian.portal.user.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account.Companion.GRAPH_FULL
import rs.russian.portal.user.repository.projections.AgeSliceCountProjection
import rs.russian.portal.user.repository.projections.GenderCountProjection
import rs.russian.portal.user.repository.projections.UsersStatisticGroupCountProjection
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, Int> {

    @EntityGraph(value = GRAPH_FULL)
    override fun findById(id: Int): Optional<Account>

    @EntityGraph(value = GRAPH_FULL)
    fun findByUsername(username: String): Optional<Account>

    @EntityGraph(value = GRAPH_FULL)
    fun findByEmail(email: String): Optional<Account>

    @EntityGraph(value = GRAPH_FULL)
    fun findAllByUsernameIn(usernames: List<String>): List<Account>

    fun findAll(specification: Specification<Account>, pageable: Pageable): Page<Account>

    @EntityGraph(value = GRAPH_FULL)
    fun findAllByIdIn(accountIds: Collection<Int>, sort: Sort): List<Account>

    @EntityGraph(value = GRAPH_FULL)
    fun findAll(specification: Specification<Account>): List<Account>

    @Query(
        """
    SELECT ui.gender AS gender, COUNT(ui) AS count
    FROM UserInfo ui
    GROUP BY ui.gender
"""
    )
    fun countByGender(): List<GenderCountProjection>

    @Query(
        """
    SELECT
        COUNT(CASE WHEN age BETWEEN 15 AND 18 THEN 1 END) AS age15to18Count,
        COUNT(CASE WHEN age > 18 AND age <= 30 THEN 1 END) AS age18to30Count,
        COUNT(CASE WHEN age > 30 AND age <= 40 THEN 1 END) AS age30to40Count,
        COUNT(CASE WHEN age > 40 AND age <= 65 THEN 1 END) AS age40to65Count,
        COUNT(CASE WHEN age > 65 THEN 1 END) AS age65AndAboveCount
    FROM (
        SELECT EXTRACT(YEAR FROM AGE(CURRENT_DATE, ui.birth_date)) AS age
        FROM user_info ui
        WHERE ui.birth_date IS NOT NULL
    ) AS derived
    """,
        nativeQuery = true
    )
    fun countByAgeSlices(): AgeSliceCountProjection

    @Query(
        value = """
            SELECT
                s.code AS groupCode,
                COUNT(DISTINCT ui.username) AS userCount
            FROM user_info ui
            JOIN project_statistic_group psg ON ui.project_code = psg.project_code
            JOIN statistic_group s ON s.code = psg.statistic_group_code
            GROUP BY s.code
        """,
        nativeQuery = true
    )
    fun countByStatisticGroup(): List<UsersStatisticGroupCountProjection>
}
