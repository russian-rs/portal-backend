package rs.russian.portal.config

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.wordpress.api.CustomWordpressApi
import org.wordpress.api.TokenWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpTokenRequest
import rs.russian.portal.user.service.wordpress.WordpressUserService
import rs.russian.portal.user.service.wordpress.WordpressUserServiceImpl
import rs.russian.portal.user.service.wordpress.WordpressUserServiceMock
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger(WpApiConfig::class.java)

@Configuration
class WpApiConfig(
    private val wpProps: WordpressProperties,
    private val tokenWordpressApis: Map<String, TokenWordpressApi>,
    private val env: Environment
) {
    private val tokens = if (env.activeProfiles.any { "local".equals(it, ignoreCase = true) }) {
        mutableMapOf("local" to "")
    } else ConcurrentHashMap()

    private fun receiveTokenByInstance(instance: WordpressInstance): String {
        val tokenApi = tokenWordpressApis[instance.name]
            ?: throw IllegalStateException("No TokenWordpressApi found for instance ${instance.name}") // impossible
        val token = try {
            tokenApi.getToken(WpTokenRequest(instance.username, instance.password)).token
        } catch (e: Exception) {
            log.error("Failed to get token for instance ${instance.name}", e)
            throw e
        }
        return token!!
    }

    private var wpUserServices: Map<String, WordpressUserService> =
        if (env.activeProfiles.any { "local".equals(it, ignoreCase = true) }) {
            mapOf("local" to WordpressUserServiceMock())
        } else wpProps.instances.associate { instance ->

            val apiClient = createWordpressApiClient(instance)
            val userWordpressApi = UsersWordpressApi(basePath = instance.baseUrl, client = apiClient)
            val customWordpressApi = CustomWordpressApi(basePath = instance.baseUrl, client = apiClient)

            instance.name to WordpressUserServiceImpl(
                instanceName = instance.name,
                apiClient = userWordpressApi,
                customWpApi = customWordpressApi
            )
        }

    @Bean
    @Profile("!local")
    fun wordpressUserServices(): Map<String, WordpressUserService> = wpUserServices

    private fun createWordpressApiClient(instance: WordpressInstance): OkHttpClient {
        val token = tokens.computeIfAbsent(instance.name) { receiveTokenByInstance(instance) }

        val authInterceptor = Interceptor { chain ->

            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()

            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Scheduled(cron = "0 0 */12 * * *")
    fun updateWordpressTokens() {
        if (env.activeProfiles.any { "local".equals(it, ignoreCase = true) }) {
            return
        }

        wpProps.instances.forEach { instance ->
            tokens.put(instance.name, receiveTokenByInstance(instance))
        }
    }
}
