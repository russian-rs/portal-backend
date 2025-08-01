package rs.russian.portal.file.domain

import jakarta.persistence.*
import jakarta.persistence.FetchType.LAZY
import rs.russian.portal.file.domain.enums.FileExt
import rs.russian.portal.file.domain.listener.FileInfoListener
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime

@Entity
@NamedEntityGraph(
    name = FileInfo.GRAPH_AUTHOR,
    attributeNodes = [NamedAttributeNode("author")]
)
@EntityListeners(FileInfoListener::class)
class FileInfo(
    @Id
    override var id: String? = null,
    override var version: LocalDateTime? = null,

    var name: String,
    var size: Long,

    @Enumerated(EnumType.STRING)
    var suffix: FileExt,

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "author", referencedColumnName = "username")
    var author: Account

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(FileInfo::id, FileInfo::name, FileInfo::suffix)

    fun getIdWithSuffix(): String {
        return "${id}.${suffix.name.lowercase()}"
    }

    companion object {
        const val GRAPH_AUTHOR = "FileInfo.Author"
    }
}
