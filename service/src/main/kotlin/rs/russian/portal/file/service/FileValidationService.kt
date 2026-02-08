package rs.russian.portal.file.service

import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import rs.russian.portal.file.domain.enums.FileExt
import rs.russian.portal.shared.exception.UnsupportedFileFormat

@Service
class FileValidationService {

    private val tika = Tika()

    private val maxFileSizes = mapOf(
        "image" to 10_000_000L,     // 10MB for images
        "document" to 20_000_000L,  // 20MB for documents
    )

    fun validateFile(file: Resource, expectedExtension: FileExt) {
        validateFilename(file.filename)
        validateFileSize(file, expectedExtension)
        validateMimeType(file, expectedExtension)
    }

    private fun validateFileSize(file: Resource, extension: FileExt) {
        val size = file.contentLength()
        val category = when (extension) {
            FileExt.PNG, FileExt.JPG, FileExt.JPEG, FileExt.GIF,
            FileExt.WEBP, FileExt.AVIF, FileExt.HEIC -> "image"
            else -> "document"
        }

        val maxSize = maxFileSizes[category]!!

        if (size > maxSize) {
            throw UnsupportedFileFormat("File size ${size / 1_000_000}MB exceeds maximum ${maxSize / 1_000_000}MB")
        }

        if (size == 0L) {
            throw UnsupportedFileFormat("Empty file not allowed")
        }
    }

    private fun validateMimeType(file: Resource, expectedExtension: FileExt) {
        val detectedMimeType = file.inputStream.use { tika.detect(it, file.filename) }
        val expectedMimeType = expectedExtension.mime

        val isValid = isMimeTypeValid(detectedMimeType, expectedMimeType, expectedExtension)

        if (!isValid) {
            log.warn(
                "MIME type mismatch for file '{}': expected {}, detected {}",
                file.filename, expectedMimeType, detectedMimeType
            )
            throw UnsupportedFileFormat("File content does not match expected type: expected $expectedMimeType, detected $detectedMimeType")
        }
    }

    private fun isMimeTypeValid(
        detectedMimeType: String,
        expectedMimeType: String,
        expectedExtension: FileExt,
    ): Boolean = when {
        // Exact match
        detectedMimeType == expectedMimeType -> true

        // Text files may have various text/* subtypes
        expectedExtension == FileExt.TXT && detectedMimeType.startsWith("text/") -> true

        // JPG and JPEG are both image/jpeg
        expectedExtension in listOf(FileExt.JPG, FileExt.JPEG) &&
            detectedMimeType == "image/jpeg" -> true

        // HEIC may be detected as image/heif
        expectedExtension == FileExt.HEIC &&
            detectedMimeType in listOf("image/heic", "image/heif", "image/heif-sequence") -> true

        // Some MS Office docs may have legacy MIME types
        expectedExtension == FileExt.DOC &&
            detectedMimeType in listOf("application/msword", "application/x-tika-msoffice") -> true

        expectedExtension == FileExt.XLS &&
            detectedMimeType in listOf("application/vnd.ms-excel", "application/x-tika-msoffice") -> true

        // Office Open XML types (Tika stream detection may return generic x-tika-ooxml for all OOXML formats)
        expectedExtension == FileExt.DOCX &&
            detectedMimeType in listOf(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/x-tika-ooxml",
            ) -> true

        expectedExtension == FileExt.XLSX &&
            detectedMimeType in listOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/x-tika-ooxml",
            ) -> true

        else -> false
    }

    private fun validateFilename(filename: String?) {
        if (filename.isNullOrBlank()) {
            throw UnsupportedFileFormat("Filename is required")
        }

        // Check for directory traversal
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw UnsupportedFileFormat("Invalid filename: path traversal detected")
        }

        // Check for null bytes
        if (filename.contains("\u0000")) {
            throw UnsupportedFileFormat("Invalid filename: null byte detected")
        }

        // Validate filename length
        if (filename.length > 255) {
            throw UnsupportedFileFormat("Filename too long (max 255 characters)")
        }

        // Check for dangerous extensions in double extension attack
        val dangerousExtensions = listOf("exe", "bat", "cmd", "sh", "php", "jsp", "asp", "js", "vbs", "ps1")
        val filenameLower = filename.lowercase()
        for (ext in dangerousExtensions) {
            if (filenameLower.contains(".$ext.")) {
                throw UnsupportedFileFormat("Suspicious filename detected")
            }
        }
    }

    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[^a-zA-Zа-яА-ЯёЁ0-9._\\-\\s]"), "_")
            .replace(Regex("\\s+"), "_")
            .take(255)
    }

    companion object {
        private val log = LoggerFactory.getLogger(FileValidationService::class.java)
    }
}
