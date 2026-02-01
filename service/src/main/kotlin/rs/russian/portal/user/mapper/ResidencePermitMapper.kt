package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.generated.model.ResidencePermitDto
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.file.service.FileService
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.ResidencePermit
import java.util.*

@Mapper(
    imports = [UUID::class],
    uses = [FileInfoMapper::class]
)
abstract class ResidencePermitMapper {

    @Autowired
    protected lateinit var fileService: FileService

    @Mapping(target = "id", expression = "java(dto.getId() != null ? dto.getId() : UUID.randomUUID())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "account", source = "account")
    @Mapping(target = "frontSidePhoto", expression = "java(fileService.getFile(dto.getFrontSidePhoto().getId()))")
    @Mapping(target = "backSidePhoto", expression = "java(fileService.getFile(dto.getBackSidePhoto().getId()))")
    abstract fun toEntity(dto: ResidencePermitDto, account: Account): ResidencePermit

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "frontSidePhoto", expression = "java(fileService.getFile(dto.getFrontSidePhoto().getId()))")
    @Mapping(target = "backSidePhoto", expression = "java(fileService.getFile(dto.getBackSidePhoto().getId()))")
    abstract fun update(dto: ResidencePermitDto, @MappingTarget entity: ResidencePermit)

    abstract fun toDto(entity: ResidencePermit): ResidencePermitDto
}
