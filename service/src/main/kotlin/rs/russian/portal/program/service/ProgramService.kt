package rs.russian.portal.program.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.ProgramDto
import rs.russian.generated.model.ProgramMapDto
import rs.russian.portal.program.mapper.ProgramMapper
import rs.russian.portal.program.repository.ProgramRepository
import rs.russian.portal.shared.utils.CacheService.Companion.PROGRAM_DICT_CACHE_NAME

@Service
class ProgramService(
    private val programRepository: ProgramRepository,
    private val programMapper: ProgramMapper
) {

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [PROGRAM_DICT_CACHE_NAME], key = "'all'")
    fun getPrograms(): List<ProgramDto> =
        programRepository.findAll().map(programMapper::toDto)

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [PROGRAM_DICT_CACHE_NAME], key = "'map'")
    fun getProgramMap(): List<ProgramMapDto> =
        programRepository.findAll().map { p ->
            ProgramMapDto(
                programCode = p.code,
                projectCodes = p.projects
                    .map { it.code }
                    .sorted()
                    .toCollection(linkedSetOf())
            )
        }.sortedBy { it.programCode }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [PROGRAM_DICT_CACHE_NAME], key = "'byProject:' + #projectCode")
    fun getProgramsByProject(projectCode: String): List<ProgramDto> =
        programRepository.findAll()
            .asSequence()
            .filter { program -> program.projects.any { it.code == projectCode } }
            .map(programMapper::toDto)
            .toList()
}
