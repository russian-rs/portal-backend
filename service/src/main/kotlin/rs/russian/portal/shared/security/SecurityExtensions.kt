package rs.russian.portal.shared.security

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser

fun currentUser() = SecurityContextHolder.getContext().authentication.principal as OidcUser

fun currentUserId() = currentUser().subject
