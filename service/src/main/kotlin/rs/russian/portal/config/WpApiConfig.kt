package rs.russian.portal.config

import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
open class WpApiConfig(
    private val wpProps: WordpressProperties,
    private val tokenWordpressApis: Map<String, TokenWordpressApi>,
    private val env: Environment,
) {
    private val tokens: MutableMap<String, String?> =
        if (env.activeProfiles.any { "local".equals(it, ignoreCase = true) }) {
            mutableMapOf("local" to "")
        } else ConcurrentHashMap()

    private fun receiveTokenByInstance(instance: WordpressInstance): String? {
        val tokenApi = tokenWordpressApis[instance.name]
            ?: throw IllegalStateException("No TokenWordpressApi found for instance ${instance.name}") // impossible
        val token = try {
            tokenApi.getToken(WpTokenRequest(instance.username, instance.password)).token
        } catch (e: Exception) {
            log.error("Failed to get token for instance ${instance.name}", e)
            null
        }
        return token
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
    open fun wordpressUserServices(): Map<String, WordpressUserService> = wpUserServices

    private fun createWordpressApiClient(instance: WordpressInstance): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val actualToken = tokens.computeIfAbsent(instance.name) { receiveTokenByInstance(instance) }
            if (actualToken == null) {
                log.error("Failed to retrieve token for instance ${instance.name}, request will not be authorized and will fail. Wait for the next scheduled token update.")
                return@Interceptor chain.proceed(chain.request())
            }

            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $actualToken")
                .build()

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
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
