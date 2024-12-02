package rs.russian.portal.file.api

import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.FilesApi
import rs.russian.generated.model.FileInfoDto
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.file.service.FileService
import rs.russian.portal.user.service.AccountService

@RestController
class FilesController(
    private val fileService: FileService,
    private val fileInfoMapper: FileInfoMapper,
    private val accountService: AccountService
) : FilesApi {

    override fun getFileInfo(id: String): ResponseEntity<FileInfoDto> {
        val file = fileService.getFile(id)
        return ResponseEntity.ok(fileInfoMapper.map(file))
    }

    override fun uploadFile(file: Resource): ResponseEntity<FileInfoDto> {
        return ResponseEntity.ok(fileService.createFile(file, accountService.getCurrentAccount()))
    }
}
