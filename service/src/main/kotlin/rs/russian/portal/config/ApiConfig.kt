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
import org.springframework.scheduling.annotation.Scheduled
import org.wordpress.api.TokenWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpTokenRequest
import rs.russian.portal.user.service.wordpress.WordpressUserService
import rs.russian.portal.user.service.wordpress.WordpressUserServiceImpl
import java.util.concurrent.ConcurrentHashMap

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
    fun tokenWordpressApis(wordpressProperties: WordpressProperties): Map<String, TokenWordpressApi> {
        return wordpressProperties.instances.associate { instance ->
            instance.name to TokenWordpressApi(client = wordpressApiClient(instance.baseUrl))
        }
    }

    @Bean
    @Profile("!local")
    fun wordpressUserServices(
        wpProps: WordpressProperties,
        tokenWordpressApis: Map<String, TokenWordpressApi>
    ): Map<String, WordpressUserService> {
        return wpProps.instances.associate { instance ->
            val tokenApi = tokenWordpressApis[instance.name] 
                ?: throw IllegalStateException("No TokenWordpressApi found for instance ${instance.name}")

            val apiClient = createWordpressApiClient(instance, tokenApi)

            instance.name to WordpressUserServiceImpl(apiClient)
        }
    }

    private fun createWordpressApiClient(
        instance: WordpressInstance,
        tokenWordpressApi: TokenWordpressApi
    ): UsersWordpressApi {
        val logger = LoggerFactory.getLogger("UsersWordpressApi")
        val token = tokenWordpressApi.getToken(WpTokenRequest(instance.username, instance.password)).token
        val interceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val url = originalUrl.newBuilder()
                .host(instance.baseUrl.toHttpUrl().host)
                .build()

            val request = originalRequest.newBuilder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .build()

            logger.info("${originalRequest.method} $url")

            chain.proceed(request)
        }
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
        okHttpClient.interceptors

        return UsersWordpressApi(client = okHttpClient)
    }

    @Scheduled(cron = "0 0 */12 * * *")
    fun updateWordpressApiClients(
        wpProps: WordpressProperties,
        wpUserServices: Map<String, WordpressUserService>,
    ) {
        wpProps.instances.forEach { instance ->
            val tokenApi = TokenWordpressApi(client = wordpressApiClient(instance.baseUrl))
            val apiClient = createWordpressApiClient(instance, tokenApi)
            wpUserServices[instance.name]?.apiClient = apiClient
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
