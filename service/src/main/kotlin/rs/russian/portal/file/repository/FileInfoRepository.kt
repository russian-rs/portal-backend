package rs.russian.portal.file.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.file.domain.FileInfo

@Repository
interface FileInfoRepository : JpaRepository<FileInfo, String> {

    fun findAllByIdIn(ids: List<String>): List<FileInfo>
}
