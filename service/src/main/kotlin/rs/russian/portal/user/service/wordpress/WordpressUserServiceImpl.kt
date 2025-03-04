package rs.russian.portal.user.service.wordpress

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.wordpress.api.TokenWordpressApi
import org.wordpress.api.UsersWordpressApi
import org.wordpress.model.WpTokenRequest
import org.wordpress.model.WpUser
import rs.russian.portal.config.WordpressProperties

@Service
@Profile("!local")
class WordpressUserServiceImpl(
    private val wpProps: WordpressProperties,
    private val tokenWordpressApi: TokenWordpressApi
) : WordpressUserService, InitializingBean {

    private lateinit var apiClient: UsersWordpressApi

    override fun getUser(username: String): WpUser? {
        val users = apiClient.searchUsers(search = username.replace("@", ""))
        return users.find { it.username == username }
    }

    override fun createUser(user: WpUser): WpUser {
        return apiClient.createUser(user)
    }

    override fun updateUser(user: WpUser): WpUser {
        return apiClient.updateUser(user.id, user)
    }

    override fun deleteUser(username: String) {
        getUser(username)?.let {
            apiClient.deleteUser(it.id)
        }
    }

    @Scheduled(cron = "0 0 */12 * * *")
    override fun afterPropertiesSet() = updateApiClient()

    private fun updateApiClient() {
        val logger = LoggerFactory.getLogger("UsersWordpressApi")
        val token = tokenWordpressApi.getToken(WpTokenRequest(wpProps.username, wpProps.password)).token
        val interceptor = Interceptor { chain ->
            val originalRequest = chain.request()
            val originalUrl = originalRequest.url

            val url = originalUrl.newBuilder()
                .host(wpProps.baseUrl.toHttpUrl().host)
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

        this.apiClient = UsersWordpressApi(client = okHttpClient)
    }

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)
    }
}
