package rs.russian.portal.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.stereotype.Component
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.service.AccountService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
@Profile("no-auth")
class DefaultUserFilter(
    private val accountService: AccountService
) : Filter, CommandLineRunner {

    companion object {
        const val USERNAME = "default_user"
        const val EMAIL = "default@mail.com"
        const val FULL_NAME = "John Smith"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        SecurityContextHolder.getContext().authentication = getDefaultOAuth2Token()
        chain.doFilter(request, response)
    }

    fun getDefaultOAuth2Token(): OAuth2AuthenticationToken {
        val authorities = setOf(SimpleGrantedAuthority("ROLE_USER"))
        val claims = mutableMapOf<String, Any>()
        claims["email"] = EMAIL
        claims["sub"] = USERNAME
        claims["name"] = FULL_NAME
        claims["nickname"] = USERNAME
        claims["preferredUsername"] = USERNAME
        claims["groups"] = UserGroup.entries.map { it.oauthGroup }

        val oidcUser = DefaultOidcUser(
            authorities,
            OidcIdToken(
                "value",
                LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC),
                LocalDateTime.now().plusDays(1).toInstant(ZoneOffset.UTC),
                claims
            ),
            OidcUserInfo(claims)
        )
        return OAuth2AuthenticationToken(oidcUser, authorities, "id")
    }

    override fun run(vararg args: String?) {
        val account = Account(
            id = 0,
            email = EMAIL,
            username = USERNAME,
            fullName = FULL_NAME,
            groups = UserGroup.entries.toSet()
        )
        account.info = UserInfo(id = account.username, account = account, birthDate = LocalDate.now())

        val existUser = accountService.findAccountByLogin(account.username)
        if (existUser == null) {
            accountService.save(account)
        } else {
            existUser.email = account.email
            existUser.groups = account.groups
            existUser.username = account.username
            existUser.fullName = account.fullName
            existUser.info = existUser.info.also {
                it?.birthDate = account.info?.birthDate
            }
            accountService.save(existUser)
        }
    }
}
