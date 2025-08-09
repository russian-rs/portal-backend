package rs.russian.portal.user.repository.projections

interface UsersStatisticGroupCountProjection {
    val groupName: String
    val userCount: Int
}