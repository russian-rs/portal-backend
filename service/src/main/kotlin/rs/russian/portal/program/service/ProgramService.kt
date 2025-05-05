package rs.russian.portal.program.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ProgramDto
import rs.russian.portal.program.mapper.ProgramMapper
import rs.russian.portal.program.repository.ProgramRepository

@Service
class ProgramService(
    private val programRepository: ProgramRepository,
    private val programMapper: ProgramMapper
    ) {

    @Transactional(readOnly = true)
    fun getPrograms(): List<ProgramDto>? {
        return programRepository.findAll().map { programMapper.toDto(it) }
    }
}