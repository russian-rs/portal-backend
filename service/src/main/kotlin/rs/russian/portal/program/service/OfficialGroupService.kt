package rs.russian.portal.program.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.OfficialGroupDto
import rs.russian.portal.program.mapper.OfficialGroupMapper
import rs.russian.portal.program.repository.OfficialGroupRepository
import rs.russian.portal.shared.utils.CacheService.Companion.OFFICIAL_GROUP_DICT_CACHE_NAME

@Service
class OfficialGroupService(
    private val officialGroupRepository: OfficialGroupRepository,
    private val officialGroupMapper: OfficialGroupMapper
) {

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = [OFFICIAL_GROUP_DICT_CACHE_NAME], key = "'all'")
    fun getOfficialGroup(): List<OfficialGroupDto> =
        officialGroupRepository.findAll().map(officialGroupMapper::toDto)
}