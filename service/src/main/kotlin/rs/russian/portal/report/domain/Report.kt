package rs.russian.portal.report.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.EnumType.STRING
import org.hibernate.annotations.SQLRestriction
import rs.russian.portal.note.domain.Note
import rs.russian.portal.report.domain.enums.ReportStatus
import rs.russian.portal.report.domain.listener.ReportEntityListener
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@Entity
@EntityListeners(ReportEntityListener::class)
@SQLRestriction("deleted = false")
@NamedEntityGraph(
    name = Report.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("tasks", subgraph = Task.GRAPH_FULL),
        NamedAttributeNode("account", subgraph = Account.GRAPH_FULL),
        NamedAttributeNode("notes")],
    subgraphs = [
        NamedSubgraph(name = Account.GRAPH_FULL, attributeNodes = [NamedAttributeNode("info")]),
        NamedSubgraph(
            name = Task.GRAPH_FULL,
            attributeNodes = [NamedAttributeNode("customer"), NamedAttributeNode("files")]
        )
    ]
)
data class Report(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(STRING)
    var status: ReportStatus = ReportStatus.CREATED,

    @OneToMany(mappedBy = "report", cascade = [ALL], orphanRemoval = true)
    var tasks: MutableSet<Task> = mutableSetOf(),

    @ManyToOne
    @JoinColumn(name = "user_login", referencedColumnName = "username")
    var account: Account,

    @SQLRestriction("entity_type = 'REPORT'")
    @OneToMany(mappedBy = "entityId", cascade = [ALL], orphanRemoval = true)
    var notes: MutableSet<Note> = mutableSetOf(),

    var deleted: Boolean = false

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Report::id, Report::status, Report::deleted)

    companion object {
        const val GRAPH_FULL = "TasksAccountNotes"
    }
}
