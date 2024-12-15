package rs.russian.portal.application.domain

enum class ApplicationStatus(
    val progress: Int,
    val terminated: Boolean
) {
    CREATED(0, false),
    IN_PROGRESS(10, false),
    CLARIFICATION(30, false),
    READY_TO_SEND(50, false),
    DOCS_SENT(70, false),
    DOCS_RECEIVED(90, false),

    DONE(100, true),
    DENY(0, true),
    DOUBLE(0, true)
}
