package rs.russian.portal.user.domain.enums

enum class UserGroup(val oauthGroup: String) {

    ADMIN("administrator"),
    ADMIN_VOLUNTEER("admin_of_volunteer"),
    ADMIN_SSO("administrator_sso"),
    DEVELOPER("developer"),
    GUIDES("um_guidesman"),
    INSIDE_VOLUNTEER("um_inside_volounteer"),
    MAIN_VOLUNTEER("um_main_volounteer"),
    LAWYERS("um_lawyers"),
    MEDIA("um_media"),
    TEACHER("stm_lms_instructor"),
    SEO_MANAGER("wpseo_editor"),
    MEMBER("um_member_of_diaspora");

    companion object {

        fun of(groupName: String?): UserGroup? {
            if (groupName.isNullOrBlank()) {
                return null
            }
            return entries.find { it.oauthGroup == groupName }
        }

        fun safeValueOf(name: String?): UserGroup? {
            if (name.isNullOrBlank()) {
                return null
            }
            return entries.find { it.name == name }
        }
    }
}
