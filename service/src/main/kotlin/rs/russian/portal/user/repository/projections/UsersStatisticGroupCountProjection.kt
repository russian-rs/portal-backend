package rs.russian.portal.user.repository.projections

import rs.russian.portal.program.domain.StatisticGroup

interface UsersStatisticGroupCountProjection {
    val groupName: StatisticGroup
    val userCount: Int
}