package rs.russian.portal.file.mapper

import kotlinx.coroutines.runBlocking
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import org.springframework.beans.factory.annotation.Autowired
import rs.russian.generated.model.FileInfoDto
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.service.S3Service

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
abstract class FileInfoMapper {

    @Autowired
    private lateinit var s3Service: S3Service

    @Mapping(target = "contentLength", source = "fileInfo.size")
    abstract fun map(fileInfo: FileInfo, link: String): FileInfoDto

    fun map(fileInfo: FileInfo?): FileInfoDto? {
        if (fileInfo == null) return null
        val link = runBlocking { s3Service.getUrl(fileInfo.getIdWithSuffix()) }
        return map(fileInfo, link.toString())
    }
}
