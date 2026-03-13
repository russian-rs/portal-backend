package rs.russian.portal.user.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account.Companion.GRAPH_FULL
import rs.russian.portal.user.repository.projections.AgeSliceCountProjection
import rs.russian.portal.user.repository.projections.GenderCountProjection
import rs.russian.portal.user.repository.projections.UsersStatisticGroupCountProjection
import java.time.LocalDate
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
    JOIN ui.account a
    WHERE EXISTS (
        SELECT c.id
        FROM Contract c
        WHERE c.account = a
          AND c.startDate <= :yearEnd
          AND c.endDate >= :yearStart
    )
    GROUP BY ui.gender
"""
    )
    fun countByGender(
        @Param("yearStart") yearStart: LocalDate,
        @Param("yearEnd") yearEnd: LocalDate,
    ): List<GenderCountProjection>

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
        JOIN account a ON a.username = ui.username
        WHERE ui.birth_date IS NOT NULL
          AND EXISTS (
              SELECT 1
              FROM contract c
              WHERE c.username = a.username
                AND c.start_date <= :yearEnd
                AND c.end_date >= :yearStart
          )
    ) AS derived
    """,
        nativeQuery = true
    )
    fun countByAgeSlices(
        @Param("yearStart") yearStart: LocalDate,
        @Param("yearEnd") yearEnd: LocalDate,
    ): AgeSliceCountProjection

    @Query(
        value = """
            SELECT
                s.code AS groupCode,
                COUNT(DISTINCT ui.username) AS userCount
            FROM user_info ui
            JOIN account a ON a.username = ui.username
            JOIN project_statistic_group psg ON ui.project_code = psg.project_code
            JOIN statistic_group s ON s.code = psg.statistic_group_code
            WHERE EXISTS (
                SELECT 1
                FROM contract c
                WHERE c.username = a.username
                  AND c.start_date <= :yearEnd
                  AND c.end_date >= :yearStart
            )
            GROUP BY s.code
        """,
        nativeQuery = true
    )
    fun countByStatisticGroup(
        @Param("yearStart") yearStart: LocalDate,
        @Param("yearEnd") yearEnd: LocalDate,
    ): List<UsersStatisticGroupCountProjection>

    @Query(
        """
        SELECT *
        FROM account a
        WHERE a.active = true
          AND a.groups @> jsonb_build_array(:group)
        """,
        nativeQuery = true
    )
    fun findAllActiveByGroup(@Param("group") group: String): List<Account>

    @EntityGraph(value = GRAPH_FULL)
    @Query(
        """
        SELECT a
        FROM Account a
        WHERE a.active = true
          AND (
              SELECT MAX(c.endDate)
              FROM Contract c
              WHERE c.account = a
          ) <= :date
          AND (
              :strict = false
              OR (
                  SELECT MAX(c.endDate)
                  FROM Contract c
                  WHERE c.account = a
              ) = :date
          )
        """
    )
    fun findByLatestContractDate(
        @Param("date") date: LocalDate,
        @Param("strict") strict: Boolean,
    ): List<Account>

    @Query(
        """
        SELECT COUNT(DISTINCT a.username)
        FROM account a
        WHERE EXISTS (
            SELECT 1
            FROM contract c
            WHERE c.username = a.username
              AND c.start_date <= :yearEnd
              AND c.end_date >= :yearStart
        )
        """,
        nativeQuery = true
    )
    fun countByActiveDuringYear(
        @Param("yearStart") yearStart: LocalDate,
        @Param("yearEnd") yearEnd: LocalDate,
    ): Long
}
