package rs.russian.portal.program.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

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

    @ElementCollection(targetClass = StatisticGroup::class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
    name = "project_statistic_group",
    joinColumns = [JoinColumn(name = "project_code")]
    )
    @Column(name = "group_name")
    val statisticGroups: Set<StatisticGroup> = emptySet(),
)