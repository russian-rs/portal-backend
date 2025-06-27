package rs.russian.portal.project.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
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
    val nameSr: String
)
