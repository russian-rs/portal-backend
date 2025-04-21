package rs.russian.portal.config

import io.authentik.api.CoreAuthentikApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import org.springframework.context.annotation.Profile
import org.wordpress.api.TokenWordpressApi
import rs.russian.portal.user.service.wordpress.WordpressUserService
import rs.russian.portal.user.service.wordpress.WordpressUserServiceImpl

@Configuration
class ApiConfig {

    @Bean
    @Profile("!no-auth")
    fun coreAuthentikApi(authentikProperties: AuthentikProperties): CoreAuthentikApi {
        return CoreAuthentikApi(
            client = authentikApiClient(
                authentikProperties.baseUrl,
                authentikProperties.apiKey,
                CoreAuthentikApi::class.simpleName
            )
        )
    }

    fun authentikApiClient(baseUrl: String, apiKey: String, name: String?): OkHttpClient {
        val logger = LoggerFactory.getLogger(name ?: "OkHttpClient")
        val interceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val url = originalUrl.newBuilder()
                .host(baseUrl.toHttpUrl().host)
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            logger.info("${originalRequest.method} $url")

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }

    @Bean
    @Profile("!local")
    @DependsOn(value = ["objectMapper"])
    fun tokenWordpressApi(wordpressProperties: WordpressProperties): TokenWordpressApi {
        return TokenWordpressApi(client = wordpressApiClient(wordpressProperties.instances.first().baseUrl))
    }

    @Bean
    @Profile("!local")
    fun wordpressUserServices(
        wpProps: WordpressProperties,
        tokenWordpressApi: TokenWordpressApi
    ): Map<String, WordpressUserService> {
        return wpProps.instances.associate { instance ->
            instance.name to WordpressUserServiceImpl(
                instance = instance,
                tokenWordpressApi = tokenWordpressApi
            )
        }
    }

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

        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}
