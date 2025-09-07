package rs.russian.portal.user.repository.projections

import rs.russian.portal.user.domain.enums.Gender

interface GenderCountProjection {
    val gender: Gender?
    val count: Int
}