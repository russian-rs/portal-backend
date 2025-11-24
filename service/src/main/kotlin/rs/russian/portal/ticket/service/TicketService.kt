package rs.russian.portal.ticket.service

import com.helpdesk.model.ArticleDto
import com.helpdesk.model.AttachmentDto
import com.helpdesk.model.TicketDto
import org.apache.commons.lang3.StringUtils.isBlank
import org.apache.commons.lang3.StringUtils.isNotBlank
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.http.MediaType.TEXT_HTML_VALUE
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import rs.russian.generated.model.TicketCreateRequest
import rs.russian.generated.model.TicketCreateResponse
import rs.russian.portal.config.HelpdeskProperties
import rs.russian.portal.file.service.FileService
import rs.russian.portal.file.service.S3Service
import rs.russian.portal.shared.exception.InvalidRequestException
import rs.russian.portal.shared.exception.NotAuthorizedException
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import rs.russian.portal.user.service.AccountService
import java.util.*

@Service
class TicketService(
    private val s3Service: S3Service,
    private val fileService: FileService,
    private val accountService: AccountService,
    private val helpdeskApiClient: HelpdeskApiClient,
    private val helpdeskProperties: HelpdeskProperties,
    @Qualifier("ticketTemplateEngine") private val ticketTemplateEngine: SpringTemplateEngine,
) {

    fun createTicket(request: TicketCreateRequest): TicketCreateResponse {
        checkTicketCreationPermissions(request)
        checkTicketValidity(request)
        val ticket = if (isNotBlank(request.toUser)) {
            createTicketAsSystem(
                request.title,
                request.body,
                request.group,
                accountService.getAccountByLogin(request.toUser!!)
            )
        } else {
            createTicketAsUser(
                request.title,
                request.body,
                request.group,
                accountService.getAccountByLogin(request.fromUser!!),
                request.attachments?.toSet()
            )
        }
        return TicketCreateResponse(id = ticket.id, ticketLink = helpdeskProperties.ticketBaseUrl + "/${ticket.id}")
    }

    /**
     * Создание обращения от имени пользователя
     */
    fun createTicketAsUser(
        title: String,
        body: String,
        group: String,
        user: Account,
        attachments: Set<String>?,
    ): TicketDto {
        val attachments = fileService.findAllByIds(attachments).map {
            AttachmentDto(it.name, Base64.getEncoder().encodeToString(s3Service.get(it).readBytes()), it.suffix.mime)
        }.toMutableList()
        return helpdeskApiClient.createTicket(
            TicketDto(
                title = title,
                group = group,
                customer = user.email,
                article = ArticleDto(
                    body = body,
                    subject = title,
                    internal = false,
                    type = ArticleDto.Type.web,
                    contentType = TEXT_HTML_VALUE,
                    sender = ArticleDto.Sender.Customer,
                    attachments = attachments
                )
            )
        )
    }

    /**
     * Создание обращения от системы к пользователю (например запрос от куратора)
     */
    fun createTicketAsSystem(title: String, body: String, group: String, user: Account): TicketDto {
        val context = Context().also { it.setVariables(mapOf("text" to body)) }
        val text = ticketTemplateEngine.process("common_template", context)
        return helpdeskApiClient.createTicket(
            TicketDto(
                title = title,
                group = group,
                customer = user.email,
                article = ArticleDto(
                    body = text,
                    subject = title,
                    to = user.email,
                    internal = false,
                    type = ArticleDto.Type.email,
                    contentType = TEXT_HTML_VALUE,
                    sender = ArticleDto.Sender.Agent
                )
            )
        )
    }

    @Cacheable("getTicketGroups")
    fun getTicketGroups(): List<String> {
        return helpdeskApiClient.getActiveGroups().map { it.name!! }
    }

    private fun checkTicketCreationPermissions(request: TicketCreateRequest) {
        val currentUser = accountService.getCurrentAccount()
        if (isNotBlank(request.toUser) && !currentUser.groups.contains(UserGroup.ADMIN_VOLUNTEER)) {
            throw NotAuthorizedException()
        }
    }

    private fun checkTicketValidity(request: TicketCreateRequest) {
        val ticketGroups = getTicketGroups()
        if (!ticketGroups.contains(request.group)) {
            throw InvalidRequestException("Ticket group '${request.group}' is not valid")
        }
        if (isNotBlank(request.toUser) && isNotBlank(request.fromUser)) {
            throw InvalidRequestException("Only 1 value possible: toUser or fromUser")
        }
        if (isBlank(request.toUser) && isBlank(request.fromUser)) {
            throw InvalidRequestException("At least 1 value required: toUser or fromUser")
        }
    }
}
