package rs.russian.portal.note.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.NoteApi
import rs.russian.portal.note.service.NoteService
import java.util.*

@RestController
class NoteController(
    private val noteService: NoteService
) : NoteApi {

    override fun deleteNote(id: UUID): ResponseEntity<Unit> {
        noteService.delete(id)
        return ResponseEntity.ok().build()
    }
}
