package rs.russian.portal.config

import jakarta.servlet.http.HttpServletResponse.SC_MOVED_PERMANENTLY
import jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders.LOCATION
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import rs.russian.portal.user.service.AccountService

@Configuration
@EnableWebSecurity
open class SecurityConfig(
    private val accountService: AccountService,
    private val appProperties: AppProperties,
) {

    @Bean
    @Profile("!no-auth")
    open fun securityFilterChain(
        httpSecurity: HttpSecurity,
    ): SecurityFilterChain = httpSecurity
        .csrf {
            // Cookie-based CSRF tokens for SPA frontend
            it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            it.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
            // Disable CSRF for public endpoints (they have captcha protection)
            it.ignoringRequestMatchers(*CSRF_DISABLED_ENDPOINTS)
        }
        .authorizeHttpRequests {
            it
                .requestMatchers(*WHITELIST).permitAll()
                .anyRequest().authenticated()
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
                    accountService.createOrUpdateAccount(authentication.principal as OidcUser)
                    res.status = SC_MOVED_PERMANENTLY
                    res.setHeader(LOCATION, "${appProperties.frontendUri}/login")
                }
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, res, _ ->
                res.sendError(SC_UNAUTHORIZED)
            }
        }
        .sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
        }
        .build()

    @Bean
    @Profile("no-auth")
    open fun securityFilterChainNoAuth(
        httpSecurity: HttpSecurity,
        defaultUserFilter: DefaultUserFilter,
    ): SecurityFilterChain = httpSecurity
        .csrf {
            it.disable()
        }
        .authorizeHttpRequests {
            it.anyRequest().permitAll()
        }
        .sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
        }
        .addFilterBefore(defaultUserFilter, OAuth2LoginAuthenticationFilter::class.java)
        .build()

    companion object {
        val WHITELIST = arrayOf(
            "/actuator/health",
            "/application/create",
            "/application/status/{id}",
            "/application/searchByEmail",
            "/turnstile",
            "/cities",
            "/cities/search",
            "/cities/{code}",
            "/csrf",
        )

        // Endpoints that don't need CSRF (public endpoints protected by captcha)
        private val CSRF_DISABLED_ENDPOINTS = arrayOf(
            "/application/create",
            "/actuator/health",
        )
    }

}
