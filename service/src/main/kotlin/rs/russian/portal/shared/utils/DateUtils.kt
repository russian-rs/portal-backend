package rs.russian.portal.shared.utils

import java.time.*

fun toOffsetDateTime(date: LocalDate): OffsetDateTime =
    OffsetDateTime.of(date, LocalTime.of(0, 0), ZoneOffset.UTC)

fun toOffsetDateTime(dateTime: LocalDateTime): OffsetDateTime =
    OffsetDateTime.of(dateTime, ZoneOffset.UTC)
