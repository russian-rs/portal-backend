package rs.russian.portal.mail.domain

enum class EmailOutboxStatus {
    CREATED,    // Created and waiting for sending
    RETRY,      // Marked for retry sending
    ERROR,      // Failed to send
    OK,         // Sent successfully
}
