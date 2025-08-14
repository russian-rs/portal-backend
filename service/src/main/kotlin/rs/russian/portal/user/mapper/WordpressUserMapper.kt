package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.enums.UserGroup
import java.util.*

@Mapper(imports = [UUID::class])
abstract class WordpressUserMapper {

    @Mapping(target = "id", constant = "0")
    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "slug", source = "username")
    @Mapping(target = "nickname", source = "username")
    @Mapping(target = "roles", source = "groups", qualifiedByName = ["roles"])
    @Mapping(target = "password", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "firstName", source = "fullName", qualifiedByName = ["firstName"])
    @Mapping(target = "lastName", source = "fullName", qualifiedByName = ["lastName"])
    abstract fun map(account: Account): WpUser

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "name", source = "fullName")
    @Mapping(target = "slug", source = "username")
    @Mapping(target = "nickname", source = "username")
    @Mapping(target = "roles", source = "groups", qualifiedByName = ["roles"])
    @Mapping(target = "firstName", source = "fullName", qualifiedByName = ["firstName"])
    @Mapping(target = "lastName", source = "fullName", qualifiedByName = ["lastName"])
    abstract fun update(account: Account, @MappingTarget wpUser: WpUser)

    @Named("firstName")
    fun firstName(fullName: String): String {
        return if (fullName.contains(' ')) {
            fullName.split(' ').firstOrNull() ?: fullName
        } else {
            fullName
        }
    }

    @Named("lastName")
    fun lastName(fullName: String): String {
        return if (fullName.contains(' ')) {
            fullName.split(' ').lastOrNull() ?: ""
        } else {
            ""
        }
    }

    @Named("roles")
    fun roles(groups: Set<UserGroup>): List<String> {
        return groups.map { it.oauthGroup }
    }
}
