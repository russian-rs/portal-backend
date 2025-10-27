package rs.russian.portal.program.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.Table

@Entity
@Table(name = "program")
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

    @ManyToMany
    @JoinTable(
        name = "program_project",
        joinColumns = [JoinColumn(name = "program_code", referencedColumnName = "code")],
        inverseJoinColumns = [JoinColumn(name = "project_code", referencedColumnName = "code")]
    )
    val projects: MutableSet<Project> = mutableSetOf()
)
