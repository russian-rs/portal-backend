package rs.russian.portal.file.api

import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.FilesApi
import rs.russian.generated.model.FileInfoDto
import rs.russian.portal.file.service.FileService

@RestController
class FilesController(
    private val fileService: FileService
) : FilesApi {

    override fun getFileInfo(id: String): ResponseEntity<FileInfoDto> {
        return ResponseEntity.ok(fileService.getFile(id))
    }

    override fun uploadFile(file: Resource): ResponseEntity<FileInfoDto> {
        return ResponseEntity.ok(fileService.createFile(file))
    }
}
