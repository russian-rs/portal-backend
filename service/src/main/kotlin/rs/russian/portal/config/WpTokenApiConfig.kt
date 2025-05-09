package rs.russian.portal.config

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.wordpress.api.TokenWordpressApi

@Configuration
class WpTokenApiConfig {

    @Bean
    @Profile("!local")
    fun tokenWordpressApis(wordpressProperties: WordpressProperties): Map<String, TokenWordpressApi> {
        return wordpressProperties.instances.associate { instance ->
            instance.name to TokenWordpressApi(basePath = instance.baseUrl, client = wordpressApiClient(instance.baseUrl))
        }
    }

    @Bean
    @Profile("local")
    fun tokenWordpressApisLocal(): Map<String, TokenWordpressApi> = mapOf("local" to TokenWordpressApi())

    fun wordpressApiClient(baseUrl: String): OkHttpClient {
        val logger = LoggerFactory.getLogger("TokenWordpressApi")
        val interceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val url = originalUrl.newBuilder()
                .host(baseUrl.toHttpUrl().host)
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .build()

            logger.info("${originalRequest.method} $url")

            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
//            .addInterceptor(interceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }
}
