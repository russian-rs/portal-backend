package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.wordpress.model.WpUser
import rs.russian.portal.user.domain.Account
import java.util.*

@Mapper(imports = [UUID::class])
abstract class WordpressUserMapper {

    @Mapping(target = "id", constant = "0")
    @Mapping(target = "name", source = "account.fullName")
    @Mapping(target = "slug", source = "account.username")
    @Mapping(target = "nickname", source = "account.username")
    @Mapping(target = "password", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "firstName", source = "account.fullName", qualifiedByName = ["firstName"])
    @Mapping(target = "lastName", source = "account.fullName", qualifiedByName = ["lastName"])
    abstract fun create(account: Account, roles: Set<String>): WpUser

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "name", source = "account.fullName")
    @Mapping(target = "slug", source = "account.username")
    @Mapping(target = "nickname", source = "account.username")
    @Mapping(target = "firstName", source = "account.fullName", qualifiedByName = ["firstName"])
    @Mapping(target = "lastName", source = "account.fullName", qualifiedByName = ["lastName"])
    abstract fun update(account: Account, roles: Set<String>, @MappingTarget wpUser: WpUser): WpUser

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
}
