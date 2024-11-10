package rs.russian.portal.shared.jpa

import jakarta.persistence.*
import rs.russian.portal.shared.jpa.JpaEntityExtensions.Companion.propertyEquals
import rs.russian.portal.shared.jpa.JpaEntityExtensions.Companion.propertyHashCode
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties

/**
 * Base class for all JPA entities with correct equals and hashCode implementations
 * Inspired by <a href="https://discuss.kotlinlang.org/t/data-classes-equals-and-hashcode-for-use-with-jpa/4868/2">this discussion</a>
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@Suppress("UNUSED")
abstract class JpaEntity<ID_TYPE> : Serializable where ID_TYPE : Comparable<ID_TYPE> {

    @get:[Access(AccessType.PROPERTY) Id]
    abstract var id: ID_TYPE?

    @get:[Access(AccessType.PROPERTY) Version]
    abstract var version: LocalDateTime?

    /**
     * Returns a list of equality properties that are used for equals and hashCode. <b>
     * In most cases you should override it based on yours business logic. <b>
     * The default is to return all declared public properties that are neither id nor version. <b>
     */
    protected open fun equalityProperties(): Collection<KProperty1<out JpaEntity<ID_TYPE>, Any?>> =
        publicProperties().filter { it.name != "id" && it.name != "version" }

    final override fun equals(other: Any?): Boolean =
        if (equalityProperties.isEmpty()) super.equals(other) else propertyEquals(this, other, dataProperties)

    final override fun hashCode(): Int =
        if (equalityProperties.isEmpty()) super.hashCode() else propertyHashCode(this, dataProperties)

    final override fun toString(): String =
        "${this::class.simpleName}(${equalityProperties().joinToString(",") { "${it::name}=${it::get}" }})"

    @get:Transient
    private val equalityProperties: Collection<KProperty1<out JpaEntity<ID_TYPE>, Any?>>
        get() = emptyPropertiesMap<ID_TYPE>().computeIfAbsent(this::class) { _ -> equalityProperties() }

    @get:Transient
    private val dataProperties: Collection<KProperty1<out JpaEntity<ID_TYPE>, Any?>>
        get() = emptyPropertiesMap<ID_TYPE>().computeIfAbsent(this::class) { _ -> publicProperties() }

    private fun publicProperties(): Collection<KProperty1<out JpaEntity<ID_TYPE>, Any?>> =
        this::class.declaredMemberProperties.filter { it.visibility === KVisibility.PUBLIC }

    companion object {
        private fun <ID_TYPE> emptyPropertiesMap():
                MutableMap<KClass<out JpaEntity<ID_TYPE>>, Collection<KProperty1<out JpaEntity<ID_TYPE>, Any?>>>
                where ID_TYPE : Comparable<ID_TYPE> = mutableMapOf()
    }
}
