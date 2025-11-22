package rs.russian.portal.config

import com.outline.api.GroupsOutlineApi
import com.outline.api.UsersOutlineApi
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import rs.russian.portal.shared.api.buildBearerApiClient
import rs.russian.portal.user.service.outline.OutlineApiClient

@Configuration
open class OutlineConfig(
    val outlineProperties: OutlineProperties,
) {

    private val apiClient: OkHttpClient = buildBearerApiClient(outlineProperties.apiKey)

    @Bean
    @Profile("!local")
    open fun outlineApiClient() = OutlineApiClient(
        groupsOutlineApi = GroupsOutlineApi(outlineProperties.baseUrl, apiClient),
        usersOutlineApi = UsersOutlineApi(outlineProperties.baseUrl, apiClient)
    )
}
