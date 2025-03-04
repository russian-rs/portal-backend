package rs.russian.portal.maps.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.maps.domain.Playground

@Repository
interface PlaygroundRepository : JpaRepository<Playground, Long>
