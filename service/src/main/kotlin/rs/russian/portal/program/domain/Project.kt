package rs.russian.portal.program.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "project")
class Project(
    @Id
    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "name_ru", nullable = false)
    val nameRu: String,

    @Column(name = "name_en", nullable = false)
    val nameEn: String,

    @Column(name = "name_sr", nullable = false)
    val nameSr: String,

    @ManyToMany
    @JoinTable(
        name = "project_statistic_group",
        joinColumns = [JoinColumn(name = "project_code")],
        inverseJoinColumns = [JoinColumn(name = "statistic_group_code")]
    )
    val statisticGroups: Set<StatisticGroup> = emptySet(),

    @ManyToMany(mappedBy = "projects")
    val programs: MutableSet<Program> = mutableSetOf()
)
