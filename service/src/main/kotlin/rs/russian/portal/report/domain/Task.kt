package rs.russian.portal.report.domain

import jakarta.persistence.*
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
data class Task(
    @Id
    @Column(name = "id")
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = LocalDateTime.now(),

    var date: LocalDate,
    var name: String,
    var description: String? = null,
    var timeSpent: Int,
    var result: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    var report: Report

) : JpaEntity<UUID>() {

    override fun equalityProperties() = setOf(Task::id)
}
