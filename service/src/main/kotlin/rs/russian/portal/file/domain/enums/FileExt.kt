package rs.russian.portal.file.domain.enums

import rs.russian.portal.shared.exception.UnsupportedFileFormat

enum class FileExt {
    DOC,
    DOCX,
    PDF,
    TXT,
    XLS,
    XLSX,
    PNG,
    JPG,
    JPEG,
    HEIC,
    GIF,
    WEBP,
    AVIF;

    companion object {

        fun of(extension: String?): FileExt {
            if (extension.isNullOrBlank()) {
                throw UnsupportedFileFormat()
            }
            return entries.find { it.name.lowercase() == extension.lowercase() } ?: throw UnsupportedFileFormat()
        }
    }
}

