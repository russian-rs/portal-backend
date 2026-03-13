package rs.russian.portal.shared.security

import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import rs.russian.portal.user.domain.enums.UserGroup

fun currentAuthentication(): Authentication? = SecurityContextHolder.getContext().authentication

fun currentUser(): OidcUser? {
    return try {
        currentAuthentication()?.principal as? OidcUser
    } catch (_: Exception) {
        null
    }
}

fun currentUserLogin(): String? {
    val authentication = currentAuthentication()
    return when (val principal = authentication?.principal) {
        is OidcUser -> principal.nickName
        is Jwt -> principal.getClaimAsString("preferred_username") ?: principal.subject
        else -> null
    }
}

fun currentUserRoles(): Set<UserGroup>? {
    return when (val authentication = currentAuthentication()) {
        is JwtAuthenticationToken -> {
            val groups = authentication.tokenAttributes["groups"]
            if (groups is Collection<*>) {
                groups.filterIsInstance<String>().mapNotNull { UserGroup.of(it) }.toSet()
            } else {
                emptySet()
            }
        }

        else -> {
            val principal = authentication?.principal
            if (principal is OidcUser) {
                principal.userInfo?.userGroups()
            } else {
                null
            }
        }
    }
}

fun OidcUserInfo.userGroups(): Set<UserGroup> {
    val groups = mutableSetOf<UserGroup>()
    if (claims["groups"] is Collection<*>) {
        (claims["groups"] as Collection<*>).forEach { oauthGroup ->
            if (oauthGroup is String) {
                UserGroup.of(oauthGroup)?.let { groups.add(it) }
            }
        }
    }
    return groups
}
