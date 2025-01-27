package rs.russian.portal.note.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.note.domain.Note
import java.util.*

@Repository
interface NoteRepository : JpaRepository<Note, UUID>
