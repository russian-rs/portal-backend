package rs.russian.portal.config

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.smithy.kotlin.runtime.net.url.Url
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.benmanes.caffeine.cache.Caffeine
import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import rs.russian.portal.shared.utils.CacheService
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

@Configuration
@EnableCaching
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT59S")
@EnableConfigurationProperties(value = [AppProperties::class, S3Properties::class, AuthentikProperties::class])
class AppConfig {

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper()
        .registerModules(JavaTimeModule())
        .registerKotlinModule()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Bean
    fun shedlockProvider(datasource: DataSource): LockProvider = JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(JdbcTemplate(datasource))
            .usingDbTime()
            .build()
    )

    @Bean
    fun s3Client(props: S3Properties): S3Client {
        return S3Client {
            region = props.region
            endpointUrl = Url.parse(props.endpoint)
            forcePathStyle = true
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = props.accessKey
                secretAccessKey = props.secretKey
            }
        }
    }

    @Bean
    fun cacheManager(): CaffeineCacheManager {
        val cacheManager = CaffeineCacheManager()

        cacheManager.registerCustomCache(
            CacheService.S3_FILE_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build()
        )

        return cacheManager
    }
}
