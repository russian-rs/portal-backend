package rs.russian.portal.program.domain

import jakarta.persistence.*

@Entity
@Table(name = "project")
@NamedEntityGraph(
    name = Project.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("program", subgraph = Program.GRAPH_FULL)
    ],
    subgraphs = [
        NamedSubgraph(
            name = Program.GRAPH_FULL,
            attributeNodes = [NamedAttributeNode("projects")]
        )
    ]
)
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

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "program_code", nullable = false)
    val program: Program,

    @ManyToMany
    @JoinTable(
        name = "project_statistic_group",
        joinColumns = [JoinColumn(name = "project_code")],
        inverseJoinColumns = [JoinColumn(name = "statistic_group_code")]
    )
    val statisticGroups: Set<StatisticGroup> = emptySet(),
) {

    companion object {
        const val GRAPH_FULL = "Project.Full"
    }
}
