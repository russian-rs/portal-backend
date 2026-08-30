package rs.russian.portal.announcement.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import rs.russian.portal.announcement.domain.AnnouncementRead
import java.util.UUID

@Repository
interface AnnouncementReadRepository : JpaRepository<AnnouncementRead, UUID> {

    @Query("select r.announcementId from AnnouncementRead r where r.accountId = :accountId")
    fun findAnnouncementIdsByAccountId(accountId: Int): List<UUID>

    @Modifying
    @Query(
        value = """
            INSERT INTO announcement_read
                (id, version, announcement_id, account_id, read_time)
            VALUES
                (:id, CURRENT_TIMESTAMP, :announcementId, :accountId, CURRENT_TIMESTAMP)
            ON CONFLICT (announcement_id, account_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun insertIfAbsent(
        @Param("id") id: UUID,
        @Param("announcementId") announcementId: UUID,
        @Param("accountId") accountId: Int,
    ): Int
}
