package rs.russian.portal.report.domain

import jakarta.persistence.*
import rs.russian.portal.file.domain.FileInfo
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@NamedEntityGraph(
    name = Task.GRAPH_FULL,
    attributeNodes = [NamedAttributeNode("customer"), NamedAttributeNode("files")],
)
data class Task(
    @Id
    @Column(name = "id")
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = LocalDateTime.now(),

    var date: LocalDate,
    var name: String,
    var description: String,
    var timeSpent: Int,
    var result: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    var report: Report,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_login", referencedColumnName = "username")
    var customer: Account? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(
        name = "task_to_file",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "file_id")]
    )
    var files: List<FileInfo> = mutableListOf()

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Task::id)

    companion object {
        const val GRAPH_FULL = "TaskCustomerFiles"
    }
}
