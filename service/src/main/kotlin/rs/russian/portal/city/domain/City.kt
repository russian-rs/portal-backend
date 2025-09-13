package rs.russian.portal.city.domain

import jakarta.persistence.*

@Entity
@Table(name = "city")
data class City(
    @Id
    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "name_cyrillic", nullable = false)
    val nameCyrillic: String,

    @Column(name = "has_mup", nullable = false)
    val hasMup: Boolean = false,

    @Column(name = "active", nullable = false)
    val active: Boolean = true,
)
