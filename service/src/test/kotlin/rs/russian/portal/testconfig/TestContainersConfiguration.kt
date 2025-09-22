package rs.russian.portal.testconfig

import com.redis.testcontainers.RedisContainer
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestContainersConfiguration {

    @Bean
    fun redisContainer(): RedisContainer {
        val redisContainer = RedisContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withReuse(true)
        redisContainer.start()
        return redisContainer
    }

    @Bean
    fun redisConnectionFactory(redisContainer: RedisContainer): RedisConnectionFactory {
        return LettuceConnectionFactory(redisContainer.host, redisContainer.firstMappedPort)
    }
}