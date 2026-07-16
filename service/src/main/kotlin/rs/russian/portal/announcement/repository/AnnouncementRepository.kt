package rs.russian.portal.announcement.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.announcement.domain.Announcement
import java.util.UUID

@Repository
interface AnnouncementRepository : JpaRepository<Announcement, UUID> {

    fun findAllByActiveTrueOrderByCreateTimeDesc(): List<Announcement>
}
