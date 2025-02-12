package rs.russian.portal.user.domain.specification

import org.springframework.data.jpa.domain.Specification
import rs.russian.generated.model.UserSearchFilter
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.equal
import rs.russian.portal.shared.jpa.like
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_
import rs.russian.portal.user.domain.UserInfo_

fun searchSpecification(query: String, filter: UserSearchFilter?): Specification<Account> {
    var resultSpec: Specification<Account> = empty()

    if (query.isNotBlank()) {
        val querySpec = like<Account>(Account_.FULL_NAME, query)
            .or(like(Account_.USERNAME, query))
            .or(like(Account_.EMAIL, query))
            .or(like(Account_.INFO, UserInfo_.TELEGRAM, query))
            .or(like(Account_.INFO, UserInfo_.PHONE, query))
        resultSpec = resultSpec.and(querySpec)
    }

    filter?.let {
        var filterSpec: Specification<Account> = empty()

        if (filter.onlyInactive)
            filterSpec = filterSpec.and(equal(Account_.ACTIVE, false))

        resultSpec = resultSpec.and(filterSpec)
    }

    return resultSpec
}
