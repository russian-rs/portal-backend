package rs.russian.portal.shared.jpa

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

fun <T> empty(): Specification<T> {
    return Specification { _, _, _ -> null }
}

fun <T> isNotNull(fieldName: String): Specification<T> {
    return Specification { root, _, builder ->
        builder.isNotNull(root.get<Any>(fieldName))
    }
}

fun <T> isNull(fieldName: String): Specification<T> {
    return Specification { root, _, builder ->
        builder.isNull(root.get<Any>(fieldName))
    }
}

fun <T, V : Any> equal(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.equal(root.get<Any>(fieldName), value)
    }
}

fun <T, V : Any> equal(fieldName: String, subFieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        val sub = root.join<T, Any>(fieldName, JoinType.LEFT)
        builder.equal(sub.get<Any>(subFieldName), value)
    }
}

fun <T, V : Any> equal(fieldName: String, subFieldName: String, subSubFieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        val sub = root.join<T, Any>(fieldName, JoinType.LEFT)
        val subSub = sub.join<T, Any>(subFieldName, JoinType.LEFT)
        builder.equal(subSub.get<Any>(subSubFieldName), value)
    }
}

fun <T, V> notEqual(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.notEqual(root.get<Any>(fieldName), value)
    }
}

fun <T, V> notEqual(fieldName: String, subFieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        val sub = root.join<T, Any>(fieldName, JoinType.LEFT)
        builder.notEqual(sub.get<Any>(subFieldName), value)
    }
}

fun <T, V : Comparable<V>> less(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.lessThan(root.get(fieldName), value)
    }
}

fun <T, V : Comparable<V>> lessOrEqual(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.lessThanOrEqualTo(root.get(fieldName), value)
    }
}

fun <T, V : Comparable<V>> greater(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.greaterThan(root.get(fieldName), value)
    }
}

fun <T, V : Comparable<V>> greaterOrEqual(fieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        builder.greaterThanOrEqualTo(root.get(fieldName), value)
    }
}

fun <T, V : Comparable<V>> greaterOrEqual(fieldName: String, subFieldName: String, value: V): Specification<T> {
    return Specification { root, _, builder ->
        val sub = root.join<T, Any>(fieldName, JoinType.LEFT)
        builder.greaterThanOrEqualTo(sub.get(subFieldName), value)
    }
}

fun <T> betweenLocalDates(fieldName: String, start: LocalDate, end: LocalDate): Specification<T> {
    return Specification { root, _, builder ->
        builder.between(root.get(fieldName), start, end)
    }
}

fun <T> betweenLocalDateTimes(fieldName: String, start: LocalDateTime, end: LocalDateTime): Specification<T> {
    return Specification { root, _, builder ->
        builder.between(root.get(fieldName), start, end)
    }
}

fun <T> betweenOffsetDateTimes(fieldName: String, start: OffsetDateTime, end: OffsetDateTime): Specification<T> {
    return Specification { root, _, builder ->
        builder.between(root.get(fieldName), start, end)
    }
}

fun <T, V : Any> contains(fieldName: String, values: Collection<V>): Specification<T> {
    return if (values.isEmpty()) {
        empty()
    } else if (values.size == 1) {
        equal(fieldName, values.iterator().next())
    } else {
        Specification { root, _, _ ->
            root.get<V>(fieldName).`in`(values)
        }
    }
}

fun <T, V : Any> notContains(fieldName: String, values: Collection<V>): Specification<T> {
    return if (values.isEmpty()) {
        empty()
    } else if (values.size == 1) {
        notEqual(fieldName, values.iterator().next())
    } else {
        Specification { root, _, _ ->
            root.get<V>(fieldName).`in`(values).not()
        }
    }
}

fun <T> like(fieldName: String, value: String): Specification<T> {
    return Specification { root, _, builder ->
        builder.like(builder.lower(root.get(fieldName)), "%${value.lowercase()}%")
    }
}

fun <T> like(fieldName: String, subFieldName: String, value: String): Specification<T> {
    return Specification { root, _, builder ->
        val sub = root.join<T, Any>(fieldName, JoinType.LEFT)
        builder.like(builder.lower(sub.get(subFieldName)), "%${value.lowercase()}%")
    }
}
