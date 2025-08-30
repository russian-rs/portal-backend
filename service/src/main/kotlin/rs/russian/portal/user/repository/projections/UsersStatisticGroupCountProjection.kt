package rs.russian.portal.user.repository.projections

interface UsersStatisticGroupCountProjection {
    val groupCode: String
    val userCount: Int
}