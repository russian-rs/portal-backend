package rs.russian.portal.user.mapper

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

@Mapper(
    componentModel = SPRING,
    unmappedTargetPolicy = ERROR,
    imports = [ArrayList::class],
    uses = [FileInfoMapper::class]
)
abstract class UserMapper {

    @Autowired
    private lateinit var fileInfoMapper: FileInfoMapper

    @Mapping(target = "info", ignore = true)
    @Mapping(target = "id", source = "subject")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", expression = "java(new ArrayList<>())")
    @Mapping(target = "username", source = "nickName")
    @Mapping(target = "groups", source = "oidcUserInfo", qualifiedByName = ["mapGroups"])
    @Mapping(target = "disabled", constant = "false")
    abstract fun map(oidcUserInfo: OidcUserInfo): Account

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "info", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "username", source = "nickName")
    @Mapping(target = "groups", source = "oidcUserInfo", qualifiedByName = ["mapGroups"])
    @Mapping(target = "disabled", ignore = true)
    abstract fun update(oidcUserInfo: OidcUserInfo, @MappingTarget account: Account)

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "fullName", source = "account.fullName")
    abstract fun map(userInfo: UserInfo?): UserInfoDto

    @Named("mapGroups")
    fun mapGroups(oidcUserInfo: OidcUserInfo): Set<UserGroup> {
        return oidcUserInfo.userGroups()
    }
}
