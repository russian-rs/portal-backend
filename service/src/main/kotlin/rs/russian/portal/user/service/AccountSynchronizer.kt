package rs.russian.portal.user.service

import rs.russian.portal.user.domain.Account

interface AccountSynchronizer {

    fun sync(accounts: List<Account>)

    fun delete(account: Account)
}
