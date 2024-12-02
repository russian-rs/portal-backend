package rs.russian.portal.user.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account.Companion.GRAPH_INFO
import java.util.*

@Repository
interface AccountRepository : JpaRepository<Account, Int> {

    @EntityGraph(value = GRAPH_INFO)
    override fun findById(id: Int): Optional<Account>

    @EntityGraph(value = GRAPH_INFO)
    fun findByUsername(username: String): Optional<Account>

    @EntityGraph(value = GRAPH_INFO)
    fun findAllByUsernameIn(usernames: List<String>): List<Account>

    @EntityGraph(value = GRAPH_INFO)
    fun findAll(specification: Specification<Account>, pageable: Pageable): Page<Account>

    @EntityGraph(value = GRAPH_INFO)
    fun findAll(specification: Specification<Account>): List<Account>
}
