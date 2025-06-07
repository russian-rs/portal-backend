package rs.russian.portal.config

import io.authentik.api.CoreAuthentikApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.wordpress.api.TokenWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpTokenRequest
import rs.russian.portal.user.service.wordpress.WordpressUserService
import rs.russian.portal.user.service.wordpress.WordpressUserServiceImpl

@Configuration
class AuthentikApiConfig() {

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

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

}
