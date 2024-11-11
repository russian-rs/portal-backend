package rs.russian.portal.shared.enums

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
            return entries.find { it.name.lowercase() == extension } ?: throw UnsupportedFileFormat()
        }
    }
}

