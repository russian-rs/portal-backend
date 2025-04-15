package rs.russian.portal.user.domain

import jakarta.persistence.*
import rs.russian.generated.model.ProgramCode

@Entity
@Table(name = "program")
data class Program(
    @Id
    @Column(nullable = false, unique = true, name = "code")
    @Enumerated(EnumType.STRING)
    val code: ProgramCode
)