package rs.russian.portal.user.domain.specification

import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import rs.russian.generated.model.UserSearchFilter
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Program_
import rs.russian.portal.program.domain.Project
import rs.russian.portal.program.domain.Project_
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.equal
import rs.russian.portal.shared.jpa.like
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_
import rs.russian.portal.user.domain.UserInfo
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

        it.programCodes
            ?.let { codes ->
                val programSpec = if (codes.isEmpty()) {
                    Specification<Account> { root, _, builder ->
                        val infoJoin: Join<Account, UserInfo> =
                            root.join(Account_.info, JoinType.LEFT)

                        val progJoin: Join<UserInfo, Program> =
                            infoJoin.join(UserInfo_.program, JoinType.LEFT)

                        builder.isNull(progJoin.get<String>(Program_.code))
                    }
                } else {
                    Specification<Account> { root, _, _ ->
                        val infoJoin: Join<Account, UserInfo> =
                            root.join(Account_.info, JoinType.LEFT)

                        val progJoin: Join<UserInfo, Program> =
                            infoJoin.join(UserInfo_.program, JoinType.LEFT)

                        progJoin.get(Program_.code)
                            .`in`(codes)
                    }
                }

                filterSpec = filterSpec.and(programSpec)
            }

        it.projectCodes
            ?.let { codes ->
                val projectSpec = if (codes.isEmpty()) {
                    Specification<Account> { root, _, builder ->
                        val infoJoin: Join<Account, UserInfo> =
                            root.join(Account_.info, JoinType.LEFT)

                        val projJoin: Join<UserInfo, Project> =
                            infoJoin.join(UserInfo_.project, JoinType.LEFT)

                        builder.isNull(projJoin.get<String>(Project_.code))
                    }
                } else {
                    Specification<Account> { root, _, _ ->
                        val infoJoin: Join<Account, UserInfo> =
                            root.join(Account_.info, JoinType.LEFT)

                        val projJoin: Join<UserInfo, Project> =
                            infoJoin.join(UserInfo_.project, JoinType.LEFT)

                        projJoin.get(Project_.code)
                            .`in`(codes)
                    }
                }

                filterSpec = filterSpec.and(projectSpec)
            }

        resultSpec = resultSpec.and(filterSpec)
    }

    return resultSpec
}


