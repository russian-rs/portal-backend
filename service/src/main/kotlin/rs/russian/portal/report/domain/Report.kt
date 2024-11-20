package rs.russian.portal.report.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.EnumType.STRING
import rs.russian.portal.shared.enums.ReportStatus
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

@Entity
@NamedEntityGraph(
    name = Report.GRAPH_TASKS_ACCOUNT,
    attributeNodes = [
        NamedAttributeNode("tasks", subgraph = Task.GRAPH_FULL),
        NamedAttributeNode("account", subgraph = Account.GRAPH_INFO)],
    subgraphs = [
        NamedSubgraph(name = Account.GRAPH_INFO, attributeNodes = [NamedAttributeNode("info")]),
        NamedSubgraph(
            name = Task.GRAPH_FULL,
            attributeNodes = [NamedAttributeNode("customer"), NamedAttributeNode("files")]
        )
    ]
)
data class Report(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = LocalDateTime.now(),

    var createTime: OffsetDateTime = OffsetDateTime.now(),

    @Enumerated(STRING)
    var status: ReportStatus = ReportStatus.CREATED,

    @OneToMany(mappedBy = "report", cascade = [ALL], orphanRemoval = true)
    var tasks: List<Task> = ArrayList<Task>(),

    @ManyToOne
    @JoinColumn(name = "user_login", referencedColumnName = "username")
    var account: Account

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Report::id)

    companion object {
        const val GRAPH_TASKS_ACCOUNT = "TasksAccount"
    }
}
