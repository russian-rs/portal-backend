package rs.russian.portal.file.domain

import jakarta.persistence.*
import rs.russian.portal.file.domain.listener.FileInfoListener
import rs.russian.portal.shared.enums.Bucket
import rs.russian.portal.shared.enums.FileExt
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.util.*

@Entity
@NamedEntityGraph(
    name = FileInfo.GRAPH_AUTHOR,
    attributeNodes = [NamedAttributeNode("author")]
)
@EntityListeners(FileInfoListener::class)
data class FileInfo(
    @Id
    override var id: String? = UUID.randomUUID().toString(),
    override var version: LocalDateTime? = LocalDateTime.now(),

    var name: String,
    var size: Long,

    @Enumerated(EnumType.STRING)
    var suffix: FileExt,
    @Enumerated(EnumType.STRING)
    var bucket: Bucket = Bucket.FILES,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author", referencedColumnName = "username")
    var author: Account

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(FileInfo::id)

    fun getIdWithSuffix(): String {
        return "${id}.${suffix.name.lowercase()}"
    }

    companion object {
        const val GRAPH_AUTHOR = "Author"
    }
}
