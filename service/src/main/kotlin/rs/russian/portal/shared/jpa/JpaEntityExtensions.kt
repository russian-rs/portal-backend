package rs.russian.portal.shared.jpa

import org.hibernate.proxy.HibernateProxy
import kotlin.reflect.KProperty1

@Suppress("UNUSED")
internal abstract class JpaEntityExtensions {
    companion object {

        internal fun <T> getEffectiveClass(obj: T): Class<*> {
            return if (obj is HibernateProxy) obj.hibernateLazyInitializer.persistentClass else obj!!::class.java
        }

        /**
         * Calculates the hash code of an object based on the values of the specified properties.
         *
         * @param obj the object for which to calculate the hash code
         * @param properties the collection of properties to be included in the hash code calculation
         *
         * @return the calculated hash code for the object
         */
        internal fun <T> propertyHashCode(
            obj: T,
            properties: Collection<KProperty1<out T, Any?>>
        ): Int = properties.map {
                @Suppress("UNCHECKED_CAST")
                it as KProperty1<T, Any?>
            }.map { it.get(obj) }.hashCode()

        /**
         * Checks if the given properties of two objects are equal.
         *
         * @param first the first object to compare
         * @param second the second object to compare
         * @param properties the collection of properties to be compared
         * @return true if the properties of the objects are equal, false otherwise
         */
        internal inline fun <reified T> propertyEquals(
            first: T,
            second: Any?,
            properties: Collection<KProperty1<out T, Any?>>
        ): Boolean {

            if (first === second) return true
            if (second === null || second !is T) return false
            if (getEffectiveClass(first) != getEffectiveClass(second)) return false

            return properties.map {
                @Suppress("UNCHECKED_CAST")
                it as KProperty1<T, Any?>
            }.all { it.get(first) == it.get(second) }
        }
    }
}
