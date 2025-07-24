package rs.russian.portal.config

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import rs.russian.portal.user.service.AccountSynchroniser
import rs.russian.portal.user.service.outline.OutlineServiceImpl

@Configuration
class OutlineConfig(
    val outlineProperties: OutlineProperties
) {

    @Bean
    @Profile("!local")
    fun outlineSynchroniserService(): AccountSynchroniser = OutlineServiceImpl(
        groupsOutlineApi = GroupsOutlineApi(outlineProperties.baseUrl, apiClient),
        usersOutlineApi = UsersOutlineApi(outlineProperties.baseUrl, apiClient)
    )

    private val apiClient: OkHttpClient = run {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${outlineProperties.apiKey}")
                .build()
            chain.proceed(request)
        }

        return@run OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(headerInterceptor)
            .build()
    }
}
