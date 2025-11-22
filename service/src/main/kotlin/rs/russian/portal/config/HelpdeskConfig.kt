package rs.russian.portal.config

import com.helpdesk.api.RolesHelpdeskApi
import com.helpdesk.api.TicketsHelpdeskApi
import com.helpdesk.api.UsersHelpdeskApi
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import rs.russian.portal.shared.api.buildBearerApiClient
import rs.russian.portal.ticket.service.HelpdeskApiClient

@Configuration
open class HelpdeskConfig(
    private val helpdeskProperties: HelpdeskProperties,
) {

    private val apiClient: OkHttpClient = buildBearerApiClient(helpdeskProperties.apiKey)

    @Bean
    open fun helpdeskApiClient() = HelpdeskApiClient(
        rolesHelpdeskApi = RolesHelpdeskApi(helpdeskProperties.apiBaseUrl, apiClient),
        usersHelpdeskApi = UsersHelpdeskApi(helpdeskProperties.apiBaseUrl, apiClient),
        ticketsHelpdeskApi = TicketsHelpdeskApi(helpdeskProperties.apiBaseUrl, apiClient)
    )
}
