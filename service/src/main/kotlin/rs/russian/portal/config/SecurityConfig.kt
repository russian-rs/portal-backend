package rs.russian.portal.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.SecurityFilterChain
import rs.russian.portal.user.UserService

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userService: UserService
) {

    @Bean
    fun securityFilterChain(
        httpSecurity: HttpSecurity
    ): SecurityFilterChain = httpSecurity
        .csrf { it.disable() }
        .authorizeHttpRequests {
            it.anyRequest().authenticated()
        }
        .formLogin { it.disable() }
        .oauth2Login {
            it
                .authorizationEndpoint { endpoint ->
                    endpoint.baseUri("/oauth2/login")
                }
                .redirectionEndpoint { endpoint ->
                    endpoint.baseUri("/oauth2/code")
                }
                .successHandler { _, _, authentication ->
                    userService.createOrUpdateUser(authentication.principal as OidcUser)
                }
        }
        .build()

}
