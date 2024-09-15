package rs.russian.portal.config

import jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY
import jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession
import rs.russian.portal.user.UserService

@Configuration
@EnableWebSecurity
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 3 * 24 * 60 * 30) // 3 days session idle
class SecurityConfig(
    private val userService: UserService,
    private val appProperties: AppProperties
) {

    @Bean
    fun securityFilterChain(
        httpSecurity: HttpSecurity
    ): SecurityFilterChain = httpSecurity
        .csrf {
            it.disable()
        }
        .authorizeHttpRequests {
            it
                .requestMatchers(*WHITELIST).permitAll()
                .anyRequest().authenticated()
        }
        .sessionManagement {
            it.maximumSessions(2)
        }
        .oauth2Login {
            it
                .authorizationEndpoint { endpoint ->
                    endpoint.baseUri("/oauth2/login")
                }
                .redirectionEndpoint { endpoint ->
                    endpoint.baseUri("/oauth2/code")
                }
                .successHandler { _, res, authentication ->
                    userService.createOrUpdateUser(authentication.principal as OidcUser)
                    res.status = SC_MOVED_PERMANENTLY
                    res.setHeader(LOCATION, appProperties.frontendUri)
                }
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, res, _ ->
                res.sendError(SC_UNAUTHORIZED)
            }
        }
        .build()

    companion object {
        val WHITELIST = arrayOf(
            "/actuator/health"
        )
    }

}
