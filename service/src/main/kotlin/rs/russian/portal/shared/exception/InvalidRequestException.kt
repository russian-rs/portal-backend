package rs.russian.portal.shared.exception

class InvalidRequestException(
    override val message: String
) : RuntimeException(message)
