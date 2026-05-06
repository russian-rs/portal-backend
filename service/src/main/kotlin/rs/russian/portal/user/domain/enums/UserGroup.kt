package rs.russian.portal.user.domain.enums

enum class UserGroup(val oauthGroup: String) {

    ADMIN("administrator"),
    ADMIN_VOLUNTEER("admin_of_volunteer"),
    VOLUNTEER("volunteer"),
    VOLUNTEER_CLEAN_CITY("volunteer_clean_city"),
    DEVELOPER_WP("developer_wp"),
    PLAYGROUNDS_VOLUNTEER("playgrounds_volunteer"),
    ADMIN_SSO("administrator_sso"),
    ADMIN_WP("admin_wp"),
    DEVELOPER("developer"),
    GUIDES("um_guidesman"),
    INSIDE_VOLUNTEER("um_inside_volounteer"),
    MAIN_VOLUNTEER("um_main_volounteer"),
    LAWYERS("um_lawyers"),
    MEDIA("um_media"),
    INSTRUCTOR("stm_lms_instructor"),
    TEACHER("teacher"),
    SEO_MANAGER("wpseo_editor"),
    INTERVIEWER("interviewer"),
    MEMBER("um_member_of_diaspora"),
    ;

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
