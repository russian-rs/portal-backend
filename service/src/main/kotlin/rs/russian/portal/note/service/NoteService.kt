package rs.russian.portal.note.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import rs.russian.portal.note.domain.Note
import rs.russian.portal.note.repository.NoteRepository

@Service
class NoteService(
    private val repository: NoteRepository
) {

    @Transactional
    fun save(note: Note) = repository.save(note)
}
