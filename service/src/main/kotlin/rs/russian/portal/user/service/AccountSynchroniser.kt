package rs.russian.portal.user.service

import rs.russian.portal.user.domain.Account

interface AccountSynchroniser {
    fun sync(accounts: List<Account>)

    fun delete(account: Account)
}
