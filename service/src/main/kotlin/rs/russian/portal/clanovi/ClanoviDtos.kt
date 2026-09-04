package rs.russian.portal.clanovi

import java.time.OffsetDateTime
import java.util.UUID

data class ClanoviApplicationListItem(
    val id: UUID,
    val status: String,
    val type: String,
    val created: OffsetDateTime,
    val updated: OffsetDateTime,
    val email: String,
    val name: String,
)

data class ClanoviApplicationListResponse(
    val since: OffsetDateTime,
    val nextSince: OffsetDateTime,
    val truncated: Boolean,
    val items: List<ClanoviApplicationListItem>,
)
