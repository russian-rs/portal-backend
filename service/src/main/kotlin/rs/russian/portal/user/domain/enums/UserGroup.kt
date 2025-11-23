package rs.russian.portal.user.domain.enums

enum class UserGroup(val oauthGroup: String) {

    ADMIN("administrator"),
    ADMIN_VOLUNTEER("admin_of_volunteer"),
    DEVELOPER_WP("developer_wp"),
    DEVELOPER("developer"),
    VOLUNTEER("volounteer"),
    VOLUNTEER_CC("volounteer_cc"),
    LAWYERS("lawyers"),
    MEDIA("media"),
    TEACHER("teacher"),
    INTERVIEWER("interviewer"),
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
