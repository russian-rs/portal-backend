package rs.russian.portal.city.domain

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "city")
@AttributeOverride(name = "id", column = Column(name = "code"))
class City(
    @Id
    @Column(name = "code")
    override var id: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "name_cyrillic", nullable = false)
    var nameCyrillic: String,

    @Column(name = "has_mup", nullable = false)
    var hasMup: Boolean = false,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

    override var version: LocalDateTime? = null

) : JpaEntity<String>() {

    override fun equalityProperties() = setOf(City::id)

}
