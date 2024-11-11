package rs.russian.portal.file.service

import kotlinx.coroutines.runBlocking
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.generated.model.FileInfoDto
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.file.repository.FileInfoRepository
import rs.russian.portal.shared.enums.Bucket
import rs.russian.portal.shared.enums.FileExt
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class FileService(
    private val s3Service: S3Service,
    private val accountService: AccountService,
    private val fileInfoMapper: FileInfoMapper,
    private val fileInfoRepository: FileInfoRepository
) {

    @Transactional(readOnly = true)
    fun getFile(id: String): FileInfoDto {
        val fileInfo = fileInfoRepository.findById(id).orElseThrow()
        return fileInfoMapper.map(fileInfo)!!
    }

    @Transactional(readOnly = true)
    fun findAllByIds(ids: List<String>): List<FileInfo> {
        return fileInfoRepository.findAllByIdIn(ids)
    }

    @Transactional
    fun createFile(file: Resource): FileInfoDto {
        val id = UUID.randomUUID().toString()
        val fileInfo = fileInfoRepository.saveAndFlush(
            FileInfo(
                id = id,
                name = file.filename ?: id,
                suffix = FileExt.of(getFileSuffix(file.filename)),
                size = file.contentLength(),
                bucket = Bucket.FILES,
                author = accountService.getCurrentUser()
            )
        )
        val fileUrl = runBlocking { s3Service.upload(file, fileInfo) }
        return fileInfoMapper.map(fileInfo, fileUrl.toString())
    }

    private fun getFileSuffix(name: String?): String? {
        if (name == null) {
            return null
        }
        val split = name.split(".")
        return if (split.size < 2) {
            null
        } else {
            split.last()
        }
    }
}
