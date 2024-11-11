package rs.russian.portal.shared.utils

import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component
import rs.russian.portal.file.domain.FileInfo

@Component
class CacheService {

    @CacheEvict(cacheNames = [S3_FILE_CACHE], key = "#fileInfo")
    fun resetS3Cache(fileInfo: FileInfo) = Unit

    companion object {
        const val S3_FILE_CACHE = "s3_cache"
    }
}
