package rs.russian.portal.application.api

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.shared.security.Authorized
import rs.russian.portal.shared.security.AuthorizedAnnotationAspect

class ApplicationAssignmentAuthorizationTest {
    private val aspect = AuthorizedAnnotationAspect()
    private val annotations = ApplicationController::class.java.methods
        .filter { it.name in setOf("assignApplication", "getApplicationAssignees", "updateApplication") }
        .map { it.getAnnotation(Authorized::class.java)!! }

    @AfterEach
    fun cleanup() = SecurityContextHolder.clearContext()

    private fun authenticate(group: String, service: Boolean = false) {
        val jwt = Jwt.withTokenValue("test").header("alg", "none").claim("sub", "employee")
            .claim("groups", listOf(group)).claim("is_service_account", service).build()
        SecurityContextHolder.getContext().authentication = JwtAuthenticationToken(jwt)
    }

    @Test
    fun `both application employee roles can read candidates assign and update`() {
        listOf("admin_of_volunteer", "interviewer").forEach { group ->
            authenticate(group)
            annotations.forEach(aspect::beforeMethodExecution)
        }
    }

    @Test
    fun `volunteers unauthenticated callers and service accounts cannot assign`() {
        SecurityContextHolder.clearContext()
        annotations.forEach { assertThrows<NotAuthorizedException> { aspect.beforeMethodExecution(it) } }
        authenticate("volunteer")
        annotations.forEach { assertThrows<NotAuthorizedException> { aspect.beforeMethodExecution(it) } }
        authenticate("interviewer", service = true)
        annotations.forEach { assertThrows<NotAuthorizedException> { aspect.beforeMethodExecution(it) } }
    }
}
