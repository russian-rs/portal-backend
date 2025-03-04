package rs.russian.portal.maps.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.PlaygroundDto
import rs.russian.generated.model.VolunteerMapDto
import rs.russian.portal.maps.mapper.PlaygroundMapper
import rs.russian.portal.maps.mapper.VolunteerMapper
import rs.russian.portal.maps.repository.PlaygroundRepository
import rs.russian.portal.user.service.AccountService

@Service
class MapsService(
    private val accountService: AccountService,
    private val volunteerMapper: VolunteerMapper,
    private val playgroundMapper: PlaygroundMapper,
    private val playgroundRepository: PlaygroundRepository
) {

    fun getVolunteersMap(): List<VolunteerMapDto> {
        val users = accountService.findAll()
        return users.map { volunteerMapper.map(it) }
    }

    @Transactional(readOnly = true)
    fun getPlaygroundsMap(): List<PlaygroundDto> {
        val playgrounds = playgroundRepository.findAll()
        return playgrounds.map { playgroundMapper.map(it) }
    }
}
