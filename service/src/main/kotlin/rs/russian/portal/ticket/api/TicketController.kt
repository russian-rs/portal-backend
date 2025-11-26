package rs.russian.portal.ticket.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import rs.russian.generated.api.TicketsApi
import rs.russian.generated.model.TicketCreateRequest
import rs.russian.generated.model.TicketCreateResponse
import rs.russian.portal.ticket.service.TicketService

@RestController
class TicketController(
    private val ticketService: TicketService,
) : TicketsApi {

    override fun createTicket(request: TicketCreateRequest): ResponseEntity<TicketCreateResponse> {
        return ResponseEntity.ok(ticketService.createTicket(request))
    }

    override fun getTicketGroups(): ResponseEntity<List<String>> {
        return ResponseEntity.ok(ticketService.getTicketGroups())
    }

}
