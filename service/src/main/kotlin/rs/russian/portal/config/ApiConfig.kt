package rs.russian.portal.config

import io.authentik.api.CoreAuthentikApi
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ApiConfig {
    
    @Bean
    fun coreAuthentikApi(authentikProperties: AuthentikProperties): CoreAuthentikApi {
        return CoreAuthentikApi(client = authentikApiClient(authentikProperties.baseUrl, authentikProperties.apiKey))
    }

    fun authentikApiClient(baseUrl: String, apiKey: String): OkHttpClient {
        val interceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val newUrl = baseUrl.toHttpUrl().newBuilder()
                .addPathSegments(originalUrl.encodedPath)
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            println(newUrl)

            chain.proceed(newRequest)
        }

        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}
