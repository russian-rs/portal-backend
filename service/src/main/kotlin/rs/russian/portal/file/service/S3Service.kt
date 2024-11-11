package rs.russian.portal.file.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import rs.russian.portal.config.S3Properties
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.shared.utils.CacheService
import java.io.File
import kotlin.time.toKotlinDuration

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties
) {

    @Cacheable(cacheNames = [CacheService.S3_FILE_CACHE], key = "#fileInfo")
    suspend fun get(fileInfo: FileInfo): Url {
        val request = s3Client.presignGetObject(GetObjectRequest {
            key = fileInfo.getIdWithSuffix()
            bucket = s3Properties.bucket
        }, s3Properties.presignDuration.toKotlinDuration())
        return request.url
    }

    suspend fun upload(file: Resource, fileInfo: FileInfo): Url {
        val temp = withContext(Dispatchers.IO) {
            File.createTempFile(fileInfo.id!!, null)
        }
        FileUtils.copyInputStreamToFile(file.inputStream, temp)
        s3Client.putObject(
            PutObjectRequest {
                key = fileInfo.getIdWithSuffix()
                bucket = s3Properties.bucket
                body = temp.asByteStream()
            }
        )
        return get(fileInfo)
    }

    suspend fun remove(fileInfo: FileInfo) {
        s3Client.deleteObject(
            DeleteObjectRequest {
                key = fileInfo.getIdWithSuffix()
                bucket = s3Properties.bucket
            }
        )
    }
}
