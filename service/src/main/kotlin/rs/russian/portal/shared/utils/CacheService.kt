package rs.russian.portal.shared.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component
import rs.russian.portal.file.domain.FileInfo
import java.util.concurrent.TimeUnit.HOURS

@Component
class CacheService {

    @CacheEvict(cacheNames = [S3_FILE_CACHE_NAME], key = "#fileInfo")
    fun resetS3Cache(fileInfo: FileInfo) = Unit

    companion object {
        const val S3_FILE_CACHE_NAME = "s3_cache"
        const val PROGRAM_DICT_CACHE_NAME = "program_dict_cache"
        const val PROJECT_DICT_CACHE_NAME = "project_dict_cache"
        const val CITIES_CACHE_NAME = "cities_cache"
        const val OFFICIAL_GROUP_DICT_CACHE_NAME = "official_group_dict_cache"

        val CACHE_MAP = mapOf<String, Cache<Any, Any>>(
            S3_FILE_CACHE_NAME to Caffeine.newBuilder().expireAfterWrite(12, HOURS).build(),
            PROGRAM_DICT_CACHE_NAME to Caffeine.newBuilder().expireAfterWrite(1, HOURS).build(),
            PROJECT_DICT_CACHE_NAME to Caffeine.newBuilder().expireAfterWrite(1, HOURS).build(),
            CITIES_CACHE_NAME to Caffeine.newBuilder().expireAfterWrite(1, HOURS).build(),
            OFFICIAL_GROUP_DICT_CACHE_NAME to Caffeine.newBuilder().expireAfterWrite(1, HOURS).build(),
        )
    }
}
