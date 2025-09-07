package rs.russian.portal.user.repository.projections

interface AgeSliceCountProjection {
    val age15to18Count: Int
    val age18to30Count: Int
    val age30to40Count: Int
    val age40to65Count: Int
    val age65AndAboveCount: Int
}