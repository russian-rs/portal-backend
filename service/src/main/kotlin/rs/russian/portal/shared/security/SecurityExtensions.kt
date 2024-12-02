package rs.russian.portal.shared.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import rs.russian.portal.shared.enums.UserGroup

fun currentUser() = SecurityContextHolder.getContext().authentication.principal as OidcUser

fun currentUserLogin(): String = currentUser().nickName

fun currentUserRoles(): Set<UserGroup> = currentUser().userInfo.userGroups()

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
