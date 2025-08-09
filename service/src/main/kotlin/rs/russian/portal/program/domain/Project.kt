package rs.russian.portal.program.domain

import jakarta.persistence.*

@Entity
@Table(name = "project")
data class Project(
    @Id
    @Column(nullable = false, unique = true, name = "code")
    val code: String,

    @Column(name = "name_ru", nullable = false)
    val nameRu: String,

    @Column(name = "name_en", nullable = false)
    val nameEn: String,

    @Column(name = "name_sr", nullable = false)
    val nameSr: String,

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "project_statistic_group",
        joinColumns = [JoinColumn(name = "project_code")],
        inverseJoinColumns = [JoinColumn(name = "statistic_group_code")]
    )
    val statisticGroups: Set<StatisticGroup> = emptySet()
)