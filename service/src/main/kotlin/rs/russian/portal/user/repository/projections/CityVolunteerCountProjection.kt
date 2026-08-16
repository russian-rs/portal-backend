package rs.russian.portal.user.repository.projections

/** City fields are `null` on the roll-up row aggregating values absent from the `city` dictionary. */
interface CityVolunteerCountProjection {
    val cityCode: String?
    val cityName: String?
    val cityNameCyrillic: String?
    val volunteerCount: Int
}
