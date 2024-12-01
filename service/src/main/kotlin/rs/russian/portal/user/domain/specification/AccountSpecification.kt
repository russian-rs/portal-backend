package rs.russian.portal.user.domain.specification

import org.springframework.data.jpa.domain.Specification
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.isTrue
import rs.russian.portal.shared.jpa.like
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_
import rs.russian.portal.user.domain.UserInfo_

fun searchSpecification(query: String): Specification<Account> {
    if (query.isEmpty()) {
        return empty()
    }
    val active = isTrue<Account>(Account_.ACTIVE)
    val like = like<Account>(Account_.FULL_NAME, query)
        .or(like(Account_.USERNAME, query))
        .or(like(Account_.EMAIL, query))
        .or(like(Account_.ID, query))
        .or(like(Account_.INFO, UserInfo_.TELEGRAM, query))
        .or(like(Account_.INFO, UserInfo_.PHONE, query))
    return active.and(like)
}
