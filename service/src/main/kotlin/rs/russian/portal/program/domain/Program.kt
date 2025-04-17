package rs.russian.portal.program.domain

import jakarta.persistence.*

@Entity
@Table(name = "program")
data class Program(
    @Id
    @Column(nullable = false, unique = true, name = "code")
    val code: String,

    @Column(name = "name_ru", nullable = false)
    val nameRu: String,

    @Column(name = "name_en", nullable = false)
    val nameEn: String,

    @Column(name = "name_sr", nullable = false)
    val nameSr: String
)