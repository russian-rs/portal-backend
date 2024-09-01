package rs.russian.portal.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import rs.russian.portal.security.auth.AuthenticationErrorHandler
import rs.russian.portal.security.jwt.JwtAuthenticationFilter
import rs.russian.portal.security.jwt.JwtErrorFilter
import rs.russian.portal.security.utils.CustomPasswordEncoder

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtErrorFilter: JwtErrorFilter,
    private val userDetailsService: UserDetailsService,
    private val customPasswordEncoder: CustomPasswordEncoder,
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationErrorHandler: AuthenticationErrorHandler,
    private val authenticationConfiguration: AuthenticationConfiguration
) {

    @Bean
    fun authenticationManager(): AuthenticationManager = authenticationConfiguration.authenticationManager

    @Bean
    fun authenticationProvider(): AuthenticationProvider = DaoAuthenticationProvider().also {
        it.setUserDetailsService(userDetailsService)
        it.setPasswordEncoder(customPasswordEncoder)
    }

    @Bean
    fun securityFilterChain(
        httpSecurity: HttpSecurity,
        authenticationProvider: AuthenticationProvider
    ): SecurityFilterChain {
        httpSecurity
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests {
                it
                    .requestMatchers(*WHITELIST).permitAll()
                    .anyRequest().authenticated()
            }
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtErrorFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(authenticationErrorHandler, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return httpSecurity.build()
    }

    companion object {
        private val WHITELIST = arrayOf(
            "/auth/login",
            "/auth/refresh"
        )
    }

}
