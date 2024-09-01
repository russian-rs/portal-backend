package rs.russian.portal.security.utils

import org.springframework.security.core.context.SecurityContextHolder
import rs.russian.portal.user.domain.UserProfile

fun currentUser() = SecurityContextHolder.getContext().authentication.principal as UserProfile
