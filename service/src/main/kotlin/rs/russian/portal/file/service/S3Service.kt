package rs.russian.portal.file.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import rs.russian.portal.config.S3Properties
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.shared.utils.CacheService
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.File
import java.io.InputStream
import java.net.URL

@Service
class S3Service(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties
) {

    fun get(fileInfo: FileInfo): InputStream {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(fileInfo.getIdWithSuffix())
            .build()
        return s3Client.getObject(getObjectRequest)
    }

    @Cacheable(cacheNames = [CacheService.S3_FILE_CACHE], key = "#idWithSuffix")
    fun getUrl(idWithSuffix: String): URL {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(idWithSuffix)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(s3Properties.presignDuration)
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url()
    }

    suspend fun upload(file: Resource, fileInfo: FileInfo): URL {
        val temp = withContext(Dispatchers.IO) {
            File.createTempFile(fileInfo.id!!, null)
        }
        FileUtils.copyInputStreamToFile(file.inputStream, temp)
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(fileInfo.getIdWithSuffix())
            .build()
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(temp))
        temp.delete()
        return getUrl(fileInfo.getIdWithSuffix())
    }

    suspend fun remove(fileInfo: FileInfo) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(fileInfo.getIdWithSuffix())
                .build()
        )
    }
}
