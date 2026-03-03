package rs.russian.portal.shared.security

import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.user.domain.enums.UserGroup

/**
 * Аннотация для проверки прав доступа пользователя на основе его ролей.
 *
 * Проверка выполняется в [AuthorizedAnnotationAspect].
 * Поддерживает только обычных пользователей (OIDC/SSO).
 * Сервисные аккаунты (JWT с is_service_account=true) НЕ могут вызывать методы с этой аннотацией.
 *
 * @property allowed Список разрешенных групп. Если не пуст, у пользователя должна быть хотя бы одна из этих групп.
 * @property disallowed Список запрещенных групп. Если не пуст, у пользователя не должно быть ни одной из этих групп.
 * @throws IllegalArgumentException если заданы одновременно оба параметра или не задан ни один.
 * @throws NotAuthorizedException если у пользователя недостаточно прав или это сервисный аккаунт.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authorized(
    val allowed: Array<UserGroup> = [],
    val disallowed: Array<UserGroup> = [],
)

/**
 * Аннотация для ограничения доступа только для сервисных учетных записей.
 *
 * Метод, помеченный этой аннотацией, может быть вызван только если запрос аутентифицирован
 * с помощью JWT токена (Bearer auth) и этот токен принадлежит сервисному аккаунту.
 * Обычные сессионные пользователи (через SSO) и другие JWT (не сервисные) не смогут вызвать такой метод.
 *
 * Это единственная аннотация, позволяющая вызов метода сервисным аккаунтом.
 *
 * @throws NotAuthorizedException если запрос не является сервисным (не JWT или не сервисный аккаунт).
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthorizedService

@Aspect
@Component
class AuthorizedAnnotationAspect {

    @Before("@annotation(authorized)")
    fun beforeMethodExecution(authorized: Authorized) {
        if (isServiceAccount()) {
            throw NotAuthorizedException()
        }
        if (authorized.allowed.isNotEmpty() && authorized.disallowed.isNotEmpty()) {
            throw IllegalArgumentException("Only one of the parameters [allowed, disallowed] should be specified")
        }
        if (authorized.allowed.isEmpty() && authorized.disallowed.isEmpty()) {
            throw IllegalArgumentException("At least one of the parameters [allowed, disallowed] should be specified")
        }
        val currentUserRoles = currentUserRoles() ?: throw NotAuthorizedException()
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

    @Before("@annotation(rs.russian.portal.shared.security.AuthorizedService)")
    fun beforeServiceMethodExecution() {
        if (!isServiceAccount()) {
            throw NotAuthorizedException()
        }
    }

    @Before("within(@org.springframework.web.bind.annotation.RestController *) && !@annotation(rs.russian.portal.shared.security.AuthorizedService) && !@annotation(rs.russian.portal.shared.security.Authorized)")
    fun beforeAnyControllerMethod() {
        if (isServiceAccount()) {
            throw NotAuthorizedException()
        }
    }

    private fun isServiceAccount(): Boolean {
        val authentication = currentAuthentication()
        if (authentication !is JwtAuthenticationToken) {
            return false
        }
        return authentication.tokenAttributes["is_service_account"] as? Boolean ?: false
    }
}
