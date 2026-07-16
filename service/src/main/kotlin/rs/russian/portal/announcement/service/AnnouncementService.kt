package rs.russian.portal.announcement.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.AnnouncementCreateRequest
import rs.russian.generated.model.AnnouncementDto
import rs.russian.generated.model.UnreadAnnouncementsCountDto
import rs.russian.portal.announcement.domain.Announcement
import rs.russian.portal.announcement.domain.AnnouncementRead
import rs.russian.portal.announcement.domain.enums.AnnouncementAudience
import rs.russian.portal.announcement.mapper.AnnouncementMapper
import rs.russian.portal.announcement.repository.AnnouncementReadRepository
import rs.russian.portal.announcement.repository.AnnouncementRepository
import rs.russian.portal.shared.exception.InvalidRequestException
import rs.russian.portal.shared.security.currentUserLogin
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.service.AccountService
import java.util.UUID

@Service
class AnnouncementService(
    private val announcementRepository: AnnouncementRepository,
    private val announcementReadRepository: AnnouncementReadRepository,
    private val announcementMapper: AnnouncementMapper,
    private val accountService: AccountService,
) {

    @Transactional(readOnly = true)
    fun getForCurrentUser(): List<AnnouncementDto> {
        val account = accountService.getCurrentAccount()
        val programCode = account.info?.program?.code
        val readIds = announcementReadRepository.findAnnouncementIdsByAccountId(account.id!!).toSet()

        return announcementRepository.findAllByActiveTrueOrderByCreateTimeDesc()
            .asSequence()
            .filter { matchesAudience(it, account, programCode) }
            .map { announcementMapper.map(it, readIds.contains(it.id)) }
            .toList()
    }

    @Transactional(readOnly = true)
    fun getUnreadCount(): UnreadAnnouncementsCountDto {
        val unread = getForCurrentUser().count { !it.read }
        return UnreadAnnouncementsCountDto(unread)
    }

    @Transactional
    fun markRead(announcementId: UUID) {
        val account = accountService.getCurrentAccount()
        val announcement = announcementRepository.findById(announcementId)
            .orElseThrow { EntityNotFoundException("Announcement $announcementId not found") }

        if (!matchesAudience(announcement, account, account.info?.program?.code)) {
            throw InvalidRequestException("Announcement is not available for current user")
        }

        if (announcementReadRepository.existsByAnnouncementIdAndAccountId(announcementId, account.id!!)) {
            return
        }

        announcementReadRepository.save(
            AnnouncementRead(
                announcementId = announcementId,
                accountId = account.id!!,
            )
        )
    }

    @Transactional
    fun create(request: AnnouncementCreateRequest): AnnouncementDto {
        validateCreateRequest(request)

        val announcement = announcementRepository.save(
            Announcement(
                createdBy = currentUserLogin() ?: "system",
                title = request.title.trim(),
                body = request.body.trim(),
                audience = mapAudience(request.audience),
                programCode = request.programCode?.trim()?.takeIf { it.isNotEmpty() },
            )
        )

        return announcementMapper.map(announcement, read = true)
    }

    private fun validateCreateRequest(request: AnnouncementCreateRequest) {
        val audience = mapAudience(request.audience)
        if (audience == AnnouncementAudience.PROGRAM && request.programCode.isNullOrBlank()) {
            throw InvalidRequestException("programCode is required when audience is PROGRAM")
        }
    }

    private fun mapAudience(audience: AnnouncementCreateRequest.Audience): AnnouncementAudience =
        when (audience) {
            AnnouncementCreateRequest.Audience.ALL -> AnnouncementAudience.ALL
            AnnouncementCreateRequest.Audience.PROGRAM -> AnnouncementAudience.PROGRAM
        }

    private fun matchesAudience(announcement: Announcement, account: Account, programCode: String?): Boolean {
        if (!account.active) {
            return false
        }

        return when (announcement.audience) {
            AnnouncementAudience.ALL -> true
            AnnouncementAudience.PROGRAM ->
                !programCode.isNullOrBlank() && announcement.programCode.equals(programCode, ignoreCase = true)
        }
    }
}
