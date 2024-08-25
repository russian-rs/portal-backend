package rs.russian.portal.user.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import rs.russian.portal.shared.jpa.JpaEntity
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id override var id: Long? = null,
    override var version: LocalDateTime? = null,
    private var username: String,
    private var email: String,
    private var password: String
): JpaEntity<Long>(), UserDetails {

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true)
    private var tokens: MutableSet<UserToken> = mutableSetOf()

    fun addToken(token: String) {
        tokens.add(UserToken(token = token, user = this))
    }

    fun removeAllTokens() {
        tokens.clear()
    }

    override fun getPassword(): String = password

    override fun getUsername(): String = username

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return mutableListOf(SimpleGrantedAuthority("ADMIN"))
    }

    override fun equalityProperties() = setOf(User::username)

}
