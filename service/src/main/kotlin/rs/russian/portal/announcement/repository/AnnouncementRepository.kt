package rs.russian.portal.announcement.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import rs.russian.portal.announcement.domain.Announcement
import java.util.UUID

@Repository
interface AnnouncementRepository : JpaRepository<Announcement, UUID> {

    @Query("""
        SELECT * FROM announcement
        WHERE active = true
          AND (audience = 'ALL' OR (audience = 'PROGRAM' AND program_code = :programCode))
        ORDER BY create_time DESC
    """, nativeQuery = true)
    fun findForUser(@Param("programCode") programCode: String?): List<Announcement>

    @Query("""
        SELECT COUNT(*) FROM announcement
        LEFT JOIN announcement_read ar
            ON ar.announcement_id = id AND ar.account_id = :accountId
        WHERE active = true
          AND (audience = 'ALL' OR (audience = 'PROGRAM' AND program_code = :programCode))
          AND ar.announcement_id IS NULL
    """, nativeQuery = true)
    fun countUnreadForUser(
        @Param("programCode") programCode: String?,
        @Param("accountId") accountId: Int,
    ): Long
}
