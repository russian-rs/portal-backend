package rs.russian.portal.file.domain.listener

import jakarta.persistence.PostRemove
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation.REQUIRES_NEW
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.shared.utils.CacheService

@Component
class FileInfoListener(
    private val s3Service: S3Service,
    private val cacheService: CacheService
) {

    @PostRemove
    @Transactional(propagation = REQUIRES_NEW)
    fun postRemove(fileInfo: FileInfo) {
        runBlocking { s3Service.remove(fileInfo) }
        cacheService.resetS3Cache(fileInfo)
    }
}
