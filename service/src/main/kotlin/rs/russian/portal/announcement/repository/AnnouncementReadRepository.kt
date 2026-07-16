package rs.russian.portal.announcement.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import rs.russian.portal.announcement.domain.AnnouncementRead
import java.util.UUID

@Repository
interface AnnouncementReadRepository : JpaRepository<AnnouncementRead, UUID> {

    @Query("select r.announcementId from AnnouncementRead r where r.accountId = :accountId")
    fun findAnnouncementIdsByAccountId(accountId: Int): List<UUID>

    fun existsByAnnouncementIdAndAccountId(announcementId: UUID, accountId: Int): Boolean
}
