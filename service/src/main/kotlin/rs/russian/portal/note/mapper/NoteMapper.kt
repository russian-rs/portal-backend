package rs.russian.portal.note.mapper

import org.mapstruct.Mapper
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.ReportingPolicy.ERROR
import rs.russian.generated.model.NoteDto
import rs.russian.portal.note.domain.Note
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.util.*

@Mapper(
    componentModel = SPRING,
    unmappedTargetPolicy = ERROR,
    imports = [UUID::class, LocalDateTime::class, HashSet::class]
)
abstract class NoteMapper {

    abstract fun map(note: Note): NoteDto

    fun mapAccount(account: Account): String = account.username
}
