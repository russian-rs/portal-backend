package rs.russian.portal.config

import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.wordpress.api.TokenWordpressApi

@Configuration
open class WpTokenApiConfig {

    @Bean
    @Profile("!local")
    open fun tokenWordpressApis(wordpressProperties: WordpressProperties): Map<String, TokenWordpressApi> =
        wordpressProperties.instances.associate { instance ->
            instance.name to TokenWordpressApi(basePath = instance.baseUrl, client = OkHttpClient())
        }

    @Bean
    @Profile("local")
    open fun tokenWordpressApisLocal(): Map<String, TokenWordpressApi> = mapOf("local" to TokenWordpressApi())
}
