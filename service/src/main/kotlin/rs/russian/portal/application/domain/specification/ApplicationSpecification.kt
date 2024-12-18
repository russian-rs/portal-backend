package rs.russian.portal.application.domain.specification

import org.springframework.data.jpa.domain.Specification
import rs.russian.portal.application.domain.Application
import rs.russian.portal.application.domain.Application_
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.like


fun searchSpecification(query: String?): Specification<Application> {
    if (query.isNullOrEmpty()) {
        return empty()
    }
    return like<Application>(Application_.NAME, query)
        .or(like(Application_.EMAIL, query))
        .or(like(Application_.PHONE, query))
        .or(like(Application_.TELEGRAM, query))
        .or(like(Application_.ID, query))
}
