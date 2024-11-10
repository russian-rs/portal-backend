package rs.russian.portal.user.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy.ERROR
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import rs.russian.generated.model.AccountDto
import rs.russian.generated.model.UserInfoDto
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.UserInfo

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR, imports = [ArrayList::class])
interface UserMapper {

    @Mapping(target = "info", ignore = true)
    @Mapping(target = "id", source = "subject")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", expression = "java(new ArrayList<>())")
    @Mapping(target = "username", source = "nickName")
    fun map(oidcUserInfo: OidcUserInfo): Account

    fun map(account: Account): AccountDto

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "info", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reports", ignore = true)
    @Mapping(target = "username", source = "nickName")
    fun update(oidcUserInfo: OidcUserInfo, @MappingTarget account: Account)

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "username", source = "account.username")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "fullName", source = "account.fullName")
    fun map(userInfo: UserInfo?): UserInfoDto

}
