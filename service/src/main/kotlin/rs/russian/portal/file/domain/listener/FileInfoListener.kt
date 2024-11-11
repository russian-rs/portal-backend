package rs.russian.portal.file.domain.listener

import jakarta.persistence.PostRemove
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.service.S3Service

@Component
class FileInfoListener(
    private val s3Service: S3Service
) {

    @PostRemove
    fun postRemove(fileInfo: FileInfo) = runBlocking { s3Service.remove(fileInfo) }
}
