package rs.russian.portal.user.repository.projections

/**
 * One settlement in the per-city volunteer breakdown. All three city fields are `null` on the single
 * roll-up row that aggregates every `user_info.city` value absent from the `city` dictionary.
 */
interface CityVolunteerCountProjection {
    val cityCode: String?
    val cityName: String?
    val cityNameCyrillic: String?
    val volunteerCount: Int
}
