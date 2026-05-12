package rs.russian.portal.program.domain

import jakarta.persistence.*

@Entity
@Table(name = "program")
@NamedEntityGraph(
    name = Program.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("projects"),
        NamedAttributeNode("officialGroup")
    ]
)
class Program(
    @Id
    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "name_ru", nullable = false)
    val nameRu: String,

    @Column(name = "name_en", nullable = false)
    val nameEn: String,

    @Column(name = "name_sr", nullable = false)
    val nameSr: String,

    @OneToMany(mappedBy = "program")
    val projects: Set<Project> = emptySet(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "official_group_code")
    val officialGroup: OfficialGroup? = null,
) {

    companion object {
        const val GRAPH_FULL = "Program.Full"
    }
}
