package rs.russian.portal.program.domain

import jakarta.persistence.*

@Entity
@Table(name = "program")
@NamedEntityGraph(
    name = Program.GRAPH_FULL,
    attributeNodes = [
        NamedAttributeNode("projects")
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
) {

    companion object {
        const val GRAPH_FULL = "Program.Full"
    }
}
