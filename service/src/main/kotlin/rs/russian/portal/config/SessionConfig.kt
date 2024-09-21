package rs.russian.portal.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.security.jackson2.SecurityJackson2Modules
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession

@Configuration
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 172800) // 2 * 24 * 60 * 60 = 48 hours
class SessionConfig : BeanClassLoaderAware {

    private lateinit var loader: ClassLoader

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.loader = classLoader
    }

    @Bean
    fun springSessionDefaultRedisSerializer(): RedisSerializer<Any> {
        return GenericJackson2JsonRedisSerializer(ObjectMapper().also {
            it.registerModules(SecurityJackson2Modules.getModules(this.loader))
        })
    }

}
