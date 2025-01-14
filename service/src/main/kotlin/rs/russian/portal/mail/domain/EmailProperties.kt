package rs.russian.portal.mail.domain

data class EmailProperties(
    var toList: List<String>,
    var ccList: List<String> = mutableListOf(),
    var subject: String,
    var body: String,
    var from: String = "Русская Диаспора <info@russian.rs>"
)
