package rs.russian.portal.note.mapper

import org.mapstruct.Mapper
import rs.russian.generated.model.NoteDto
import rs.russian.portal.note.domain.Note
import rs.russian.portal.user.domain.Account
import java.time.LocalDateTime
import java.util.*

@Mapper(imports = [UUID::class, LocalDateTime::class, HashSet::class])
abstract class NoteMapper {

    abstract fun map(note: Note): NoteDto

    fun mapAccount(account: Account): String = account.username
}
