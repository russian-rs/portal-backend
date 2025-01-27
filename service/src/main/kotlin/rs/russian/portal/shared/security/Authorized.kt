package rs.russian.portal.shared.security

import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.stereotype.Component
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.user.domain.enums.UserGroup

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authorized(
    val allowed: Array<UserGroup> = [],
    val disallowed: Array<UserGroup> = []
)

@Aspect
@Component
class AuthorizedAnnotationAspect {

    @Before("@annotation(authorized)")
    fun beforeMethodExecution(authorized: Authorized) {
        if (authorized.allowed.isNotEmpty() && authorized.disallowed.isNotEmpty()) {
            throw IllegalArgumentException("Only one of the parameters [allowed, disallowed] should be specified")
        }
        if (authorized.allowed.isEmpty() && authorized.disallowed.isEmpty()) {
            throw IllegalArgumentException("At least one of the parameters [allowed, disallowed] should be specified")
        }
        val currentUserRoles = currentUserRoles()
        if (authorized.allowed.isNotEmpty()) {
            if (currentUserRoles.none { it in authorized.allowed }) {
                throw NotAuthorizedException()
            }
        }
        if (authorized.disallowed.isNotEmpty()) {
            if (currentUserRoles.any { it in authorized.disallowed }) {
                throw NotAuthorizedException()
            }
        }
    }
}
