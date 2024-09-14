package rs.russian.portal.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource

@Configuration
@EnableScheduling
@EnableConfigurationProperties(value = [AppProperties::class])
@EnableSchedulerLock(defaultLockAtMostFor = "PT59S")
class AppConfig {

    @Bean
    fun objectMapper() = ObjectMapper().registerKotlinModule()

    @Bean
    fun shedlockProvider(datasource: DataSource): LockProvider = JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(JdbcTemplate(datasource))
            .usingDbTime()
            .build()
    )
}
