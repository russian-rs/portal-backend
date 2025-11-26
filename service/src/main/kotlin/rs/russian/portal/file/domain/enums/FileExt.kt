package rs.russian.portal.file.domain.enums

import rs.russian.portal.shared.exception.UnsupportedFileFormat

enum class FileExt(val mime: String) {

    DOC("application/msword"),
    DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    PDF("application/pdf"),
    TXT("text/plain"),
    XLS("application/vnd.ms-excel"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    PNG("image/png"),
    JPG("image/jpeg"),
    JPEG("image/jpeg"),
    HEIC("image/heic"),
    GIF("image/gif"),
    WEBP("image/webp"),
    AVIF("image/avif");

    companion object {

        fun of(extension: String?): FileExt {
            if (extension.isNullOrBlank()) {
                throw UnsupportedFileFormat()
            }
            return entries.find { it.name.equals(extension, ignoreCase = true) }
                ?: throw UnsupportedFileFormat()
        }
    }
}

