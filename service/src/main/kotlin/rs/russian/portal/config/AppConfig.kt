package rs.russian.portal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import rs.russian.portal.security.auth.AuthenticationProperties

@Configuration
@EnableConfigurationProperties(value = [AuthenticationProperties::class])
class AppConfig {

    @Bean
    fun objectMapper() = ObjectMapper().registerKotlinModule()
}
