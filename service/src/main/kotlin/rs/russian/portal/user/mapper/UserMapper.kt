package rs.russian.portal.user.mapper

import io.authentik.model.User
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.MappingTarget
import org.mapstruct.Named
import org.mapstruct.ReportingPolicy.ERROR
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import rs.russian.generated.model.UserInfoDto
import rs.russian.portal.file.mapper.FileInfoMapper
import rs.russian.portal.shared.enums.UserGroup
import rs.russian.portal.shared.security.userGroups
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo
import java.time.LocalDateTime

@Mapper(
    componentModel = SPRING,
    unmappedTargetPolicy = ERROR,
    imports = [ArrayList::class, LocalDateTime::class],
    uses = [FileInfoMapper::class]
)
abstract class UserMapper {

    @Autowired
    private lateinit var fileInfoMapper: FileInfoMapper

    @Mapping(target = "info", ignore = true)
    @Mapping(target = "id", source = "subject")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "username", source = "nickName")
    @Mapping(target = "reports", expression = "java(new ArrayList<>())")
    @Mapping(target = "lastSynced", expression = "java(LocalDateTime.now())")
    @Mapping(target = "fullName", source = "oidcUserInfo", qualifiedByName = ["nameOidc"])
    @Mapping(target = "groups", source = "oidcUserInfo", qualifiedByName = ["mapGroups"])
    abstract fun map(oidcUserInfo: OidcUserInfo): Account

    @Mapping(target = "info", ignore = true)
    @Mapping(target = "id", source = "uid")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", expression = "java(new ArrayList<>())")
    @Mapping(target = "fullName", source = "ssoUser", qualifiedByName = ["nameSso"])
    @Mapping(target = "lastSynced", expression = "java(LocalDateTime.now())")
    @Mapping(target = "groups", source = "groupsObj", qualifiedByName = ["mapGroupsSso"])
    abstract fun map(ssoUser: User): Account

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "info", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "username", source = "nickName")
    @Mapping(target = "lastSynced", expression = "java(LocalDateTime.now())")
    @Mapping(target = "fullName", source = "oidcUserInfo", qualifiedByName = ["nameOidc"])
    @Mapping(target = "groups", source = "oidcUserInfo", qualifiedByName = ["mapGroups"])
    abstract fun update(oidcUserInfo: OidcUserInfo, @MappingTarget account: Account)

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "info", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "lastSynced", expression = "java(LocalDateTime.now())")
    @Mapping(target = "fullName", source = "ssoUser", qualifiedByName = ["nameSso"])
    @Mapping(target = "groups", source = "groupsObj", qualifiedByName = ["mapGroupsSso"])
    abstract fun update(ssoUser: User, @MappingTarget account: Account)

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "fullName", source = "account.fullName")
    @Mapping(target = "groups", source = "account.groups")
    @Mapping(target = "active", source = "account.active")
    abstract fun map(userInfo: UserInfo?): UserInfoDto

    @Named("mapGroups")
    fun mapGroups(oidcUserInfo: OidcUserInfo): Set<UserGroup> {
        return oidcUserInfo.userGroups()
    }

    @Named("mapGroupsSso")
    fun mapGroupsSso(groupsSso: MutableList<io.authentik.model.UserGroup>?): Set<UserGroup> {
        if (groupsSso.isNullOrEmpty()) {
            return mutableSetOf()
        }
        val groups = mutableSetOf<UserGroup>()
        groupsSso.forEach { groupSso ->
            UserGroup.of(groupSso.name)?.let {
                groups.add(it)
            }
        }
        return groups
    }

    @Named("nameSso")
    fun name(ssoUser: User): String {
        return ssoUser.name.ifBlank {
            ssoUser.username
        }
    }

    @Named("nameOidc")
    fun name(oidcUserInfo: OidcUserInfo): String {
        return if (oidcUserInfo.fullName.isNullOrBlank()) {
            oidcUserInfo.nickName
        } else {
            oidcUserInfo.fullName
        }
    }
}
