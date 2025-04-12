package rs.russian.portal.program.domain

import jakarta.persistence.*
import rs.russian.generated.model.ProgramCode

@Entity
@Table(name = "program")
data class Program(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    val code: ProgramCode,

    @Column(nullable = false)
    val nameRu: String,

    @Column(nullable = false)
    val nameEn: String,

    @Column(nullable = false)
    val nameRs: String
)