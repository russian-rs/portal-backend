package rs.russian.portal.shared.jpa.converter

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import rs.russian.portal.user.domain.enums.UserGroup

@Converter
class UserGroupSetConverter(
    private val objectMapper: ObjectMapper
) : AttributeConverter<Set<UserGroup>, String> {

    override fun convertToDatabaseColumn(attribute: Set<UserGroup>): String {
        return objectMapper.writeValueAsString(attribute)
    }

    override fun convertToEntityAttribute(value: String): Set<UserGroup> {
        return try {
            val groups = mutableSetOf<UserGroup>()
            objectMapper.readValue(value, object : TypeReference<Set<String>>() {}).forEach { groupName ->
                UserGroup.safeValueOf(groupName)?.let { groups.add(it) }
            }
            return groups
        } catch (ex: Exception) {
            mutableSetOf()
        }
    }
}
