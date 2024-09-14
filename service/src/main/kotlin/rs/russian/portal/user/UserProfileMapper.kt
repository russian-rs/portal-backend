package rs.russian.portal.user

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingConstants.ComponentModel.SPRING
import org.mapstruct.MappingTarget
import org.mapstruct.ReportingPolicy.ERROR
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import rs.russian.generated.model.UserInfo
import rs.russian.portal.user.domain.UserProfile

@Mapper(componentModel = SPRING, unmappedTargetPolicy = ERROR)
interface UserProfileMapper {

    @Mapping(target = "id", source = "subject")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "username", source = "nickName")
    fun map(oidcUserInfo: OidcUserInfo): UserProfile

    fun map(userProfile: UserProfile): UserInfo

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "username", source = "nickName")
    fun update(oidcUserInfo: OidcUserInfo, @MappingTarget userProfile: UserProfile)
}
