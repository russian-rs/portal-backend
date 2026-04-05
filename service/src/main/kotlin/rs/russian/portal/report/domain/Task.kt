package rs.russian.portal.report.domain

import jakarta.persistence.*
import jakarta.persistence.FetchType.LAZY
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
class Task(
    @Id
    @Column(name = "id")
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    var date: LocalDate,
    var name: String,
    var description: String,
    @Column(name = "name_sr")
    var nameSr: String? = null,
    @Column(name = "description_sr")
    var descriptionSr: String? = null,
    var timeSpent: Int,
    var result: String? = null,

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "report_id")
    var report: Report,

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "customer_login", referencedColumnName = "username")
    var customer: Account? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinTable(
        name = "task_to_file",
        joinColumns = [JoinColumn(name = "task_id")],
        inverseJoinColumns = [JoinColumn(name = "file_id")]
    )
    var files: MutableSet<FileInfo> = mutableSetOf()

) : JpaEntity<UUID>() {

    override fun equalityProperties() =
        setOf(Task::id, Task::date, Task::name)

    companion object {
        const val GRAPH_FULL = "Task.Full"
    }
}
