package rs.russian.portal.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession
import org.springframework.session.web.http.CookieSerializer
import org.springframework.session.web.http.DefaultCookieSerializer


@Configuration
@EnableRedisIndexedHttpSession(
    maxInactiveIntervalInSeconds = 172800, // 2 * 24 * 60 * 60 = 48 hours
    redisNamespace = "portal-backend-sessions"
)
class RedisSessionConfig : BeanClassLoaderAware {

    private lateinit var loader: ClassLoader

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.loader = classLoader
    }

    @Bean
    @Profile("!test")
    fun springSessionDefaultRedisSerializer(): RedisSerializer<Any> {
        return GenericJackson2JsonRedisSerializer(ObjectMapper().also {
            it.registerModules(SecurityJackson2Modules.getModules(this.loader))
        })
    }

    @Bean
    @Profile("test")
    fun springSessionDefaultRedisSerializerTest(objectMapper: ObjectMapper): RedisSerializer<Any> {
        objectMapper.registerModules(SecurityJackson2Modules.getModules(this.loader))
        return GenericJackson2JsonRedisSerializer(objectMapper)
    }


    @Bean
    fun cookieSerializer(): CookieSerializer {
        val cookieSerializer = DefaultCookieSerializer()
        cookieSerializer.setCookieMaxAge(2 * 24 * 60 * 60) // 48 hours in seconds
        cookieSerializer.setUseSecureCookie(true)
        return cookieSerializer
    }

}
