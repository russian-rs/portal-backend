package rs.russian.portal.announcement.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import rs.russian.generated.model.AnnouncementAudience
import rs.russian.generated.model.AnnouncementDto
import rs.russian.portal.announcement.domain.Announcement
import rs.russian.portal.announcement.domain.enums.AnnouncementAudience as DomainAnnouncementAudience

@Mapper
abstract class AnnouncementMapper {

    @Mapping(target = "read", source = "read")
    abstract fun map(announcement: Announcement, read: Boolean): AnnouncementDto

    protected fun mapAudience(audience: DomainAnnouncementAudience): AnnouncementAudience =
        AnnouncementAudience.valueOf(audience.name)
}
