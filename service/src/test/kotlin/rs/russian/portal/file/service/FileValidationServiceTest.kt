package rs.russian.portal.file.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.core.io.ByteArrayResource
import rs.russian.portal.file.domain.enums.FileExt
import rs.russian.portal.shared.exception.UnsupportedFileFormat

class FileValidationServiceTest {

    private lateinit var fileValidationService: FileValidationService

    @BeforeEach
    fun setup() {
        fileValidationService = FileValidationService()
    }

    @Test
    fun `should accept valid PDF file`() {
        val pdfContent = "%PDF-1.4\n%Test PDF content here".toByteArray()
        val resource = createResource(pdfContent, "document.pdf")

        assertDoesNotThrow {
            fileValidationService.validateFile(resource, FileExt.PDF)
        }
    }

    @Test
    fun `should accept valid PNG file`() {
        // PNG magic bytes
        val pngContent = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) +
            ByteArray(100) { 0 }
        val resource = createResource(pngContent, "image.png")

        assertDoesNotThrow {
            fileValidationService.validateFile(resource, FileExt.PNG)
        }
    }

    @Test
    fun `should accept valid JPEG file`() {
        // JPEG magic bytes
        val jpegContent = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) +
            ByteArray(100) { 0 }
        val resource = createResource(jpegContent, "image.jpg")

        assertDoesNotThrow {
            fileValidationService.validateFile(resource, FileExt.JPG)
        }
    }

    @Test
    fun `should accept valid GIF file`() {
        // GIF magic bytes
        val gifContent = "GIF89a".toByteArray() + ByteArray(100) { 0 }
        val resource = createResource(gifContent, "image.gif")

        assertDoesNotThrow {
            fileValidationService.validateFile(resource, FileExt.GIF)
        }
    }

    @Test
    fun `should accept valid text file`() {
        val textContent = "This is a plain text file content".toByteArray()
        val resource = createResource(textContent, "document.txt")

        assertDoesNotThrow {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }
    }

    @Test
    fun `should reject executable renamed as PDF`() {
        // DOS MZ executable header
        val exeContent = byteArrayOf(0x4D, 0x5A) + ByteArray(100) { 0 }
        val resource = createResource(exeContent, "malware.pdf")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.PDF)
        }

        assertTrue(exception.message?.contains("does not match") == true)
    }

    @Test
    fun `should reject PNG file disguised as PDF`() {
        // PNG magic bytes
        val pngContent = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) +
            ByteArray(100) { 0 }
        val resource = createResource(pngContent, "fake.pdf")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.PDF)
        }

        assertTrue(exception.message?.contains("does not match") == true)
    }

    @Test
    fun `should reject file with directory traversal in name`() {
        val content = "test content".toByteArray()
        val resource = createResource(content, "../../../etc/passwd")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }

        assertEquals("Invalid filename: path traversal detected", exception.message)
    }

    @Test
    fun `should reject file with backslash path traversal`() {
        val content = "test content".toByteArray()
        val resource = createResource(content, "..\\..\\windows\\system32\\config")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }

        assertEquals("Invalid filename: path traversal detected", exception.message)
    }

    @Test
    fun `should reject file with null byte in name`() {
        val content = "test content".toByteArray()
        val resource = createResource(content, "test\u0000.txt")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }

        assertEquals("Invalid filename: null byte detected", exception.message)
    }

    @Test
    fun `should reject file with double dangerous extension`() {
        val content = "test content".toByteArray()
        val resource = createResource(content, "document.exe.pdf")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.PDF)
        }

        assertEquals("Suspicious filename detected", exception.message)
    }

    @Test
    fun `should reject file with php extension in name`() {
        val content = "test content".toByteArray()
        val resource = createResource(content, "shell.php.jpg")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.JPG)
        }

        assertEquals("Suspicious filename detected", exception.message)
    }

    @Test
    fun `should reject empty file`() {
        val emptyContent = ByteArray(0)
        val resource = createResource(emptyContent, "empty.txt")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }

        assertEquals("Empty file not allowed", exception.message)
    }

    @Test
    fun `should reject file with blank filename`() {
        val content = "test".toByteArray()
        val resource = createResource(content, "   ")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.TXT)
        }

        assertEquals("Filename is required", exception.message)
    }

    @Test
    fun `should reject file exceeding image size limit`() {
        // Create content larger than 10MB
        val largeContent = ByteArray(11_000_000)
        val resource = createResource(largeContent, "large.png")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.PNG)
        }

        assertTrue(exception.message?.contains("exceeds maximum") == true)
    }

    @Test
    fun `should reject file exceeding document size limit`() {
        // Create content larger than 20MB
        val largeContent = ByteArray(21_000_000)
        val resource = createResource(largeContent, "large.pdf")

        val exception = assertThrows<UnsupportedFileFormat> {
            fileValidationService.validateFile(resource, FileExt.PDF)
        }

        assertTrue(exception.message?.contains("exceeds maximum") == true)
    }

    @Test
    fun `should sanitize filename with special characters`() {
        val result = fileValidationService.sanitizeFilename("test<>file\"name.pdf")

        assertEquals("test__file_name.pdf", result)
        assertTrue(result.matches(Regex("[a-zA-Z0-9._\\-]+")))
    }

    @Test
    fun `should sanitize filename with path components`() {
        val result = fileValidationService.sanitizeFilename("../../../test.pdf")

        // Path traversal characters are replaced with underscores
        assertFalse(result.contains("/"))
        assertTrue(result.endsWith(".pdf"))
    }

    @Test
    fun `should preserve cyrillic characters in filename`() {
        val result = fileValidationService.sanitizeFilename("документ.pdf")

        assertTrue(result.contains("документ"))
    }

    @Test
    fun `should truncate long filename`() {
        val longName = "a".repeat(300) + ".pdf"

        val result = fileValidationService.sanitizeFilename(longName)

        assertTrue(result.length <= 255)
    }

    @Test
    fun `should replace spaces with underscores`() {
        val result = fileValidationService.sanitizeFilename("my document file.pdf")

        assertEquals("my_document_file.pdf", result)
    }

    private fun createResource(content: ByteArray, filename: String): ByteArrayResource {
        return object : ByteArrayResource(content) {
            override fun getFilename(): String = filename
        }
    }
}
