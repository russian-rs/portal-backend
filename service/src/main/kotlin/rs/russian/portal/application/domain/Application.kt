package rs.russian.portal.application.domain

import jakarta.persistence.*
import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.EnumType.STRING
import org.hibernate.annotations.SQLRestriction
import rs.russian.generated.model.ContractTypeEnum
import rs.russian.portal.application.domain.ApplicationStatus.CREATED
import rs.russian.portal.application.domain.ApplicationType.NEW
import rs.russian.portal.application.domain.listener.ApplicationEntityListener
import rs.russian.portal.note.domain.Note
import rs.russian.portal.shared.jpa.JpaEntity
import rs.russian.portal.user.domain.enums.Gender
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Entity
@NamedEntityGraph(
    name = Application.GRAPH_FULL,
    attributeNodes = [NamedAttributeNode("notes")]
)
@EntityListeners(ApplicationEntityListener::class)
class Application(
    @Id
    override var id: UUID? = UUID.randomUUID(),
    override var version: LocalDateTime? = null,

    @Enumerated(STRING)
    var status: ApplicationStatus = CREATED,
    @Enumerated(STRING)
    var type: ApplicationType = NEW,
    var created: LocalDateTime = LocalDateTime.now(),

    var email: String,
    var name: String,
    var patronymic: String? = null,
    var birthDate: LocalDate? = null,
    var passport: String? = null,
    var citizenship: String? = null,
    var telegram: String? = null,
    var inSerbia: Boolean? = null,
    var enterDate: LocalDate? = null,
    var city: String? = null,
    var postalCode: String? = null,
    var address: String? = null,
    var phone: String? = null,
    var residenceRequired: Boolean? = null,
    var occupation: String? = null,
    @Enumerated(STRING)
    @Column(name = "gender")
    var gender: Gender? = null,
    var hasExperience: Boolean? = null,
    var experience: String? = null,
    var languages: String? = null,
    var skills: String? = null,
    var goal: String? = null,
    var bio: String? = null,

    var refuseReason: String? = null,
    var comment: String? = null,

    var contractFrom: LocalDate? = null,
    var contractUntil: LocalDate? = null,
    @Enumerated(STRING)
    var contractType: ContractTypeEnum? = null,

    @SQLRestriction("entity_type = 'APPLICATION'")
    @OneToMany(mappedBy = "entityId", cascade = [ALL], orphanRemoval = true)
    var notes: MutableSet<Note> = mutableSetOf(),

    ) : JpaEntity<UUID>() {

    companion object {
        const val GRAPH_FULL = "Application.Full"
    }
}
