package rs.russian.portal.user.domain

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
    val code: ProgramCode
)