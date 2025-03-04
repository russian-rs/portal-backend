package rs.russian.portal.maps.domain

import jakarta.persistence.Entity
import jakarta.persistence.Id
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

@Entity
class Playground(
    @Id
    override var id: Long?,
    override var version: LocalDateTime? = null,

    var url: String? = null,
    var date: String? = null,
    var photo: String? = null,
    var covering: String? = null,
    var drainage: String? = null,
    var fencing: String? = null,
    var security: String? = null,
    var light: String? = null,
    var lat: Double? = null,
    var lng: Double? = null

) : JpaEntity<Long>()
