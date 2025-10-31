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

        it.onlyInactive?.let {
            filterSpec = filterSpec.and(equal(Account_.ACTIVE, false))
        }

        it.onlyActive?.let {
            filterSpec = filterSpec.and(equal(Account_.ACTIVE, true))
        }

        it.program?.let { program ->
            filterSpec = filterSpec.and(programEqual(program))
        }

        it.project?.let { project ->
            filterSpec = filterSpec.and(projectEqual(project))
        }

        resultSpec = resultSpec.and(filterSpec)
    }

    return resultSpec
}

private fun programEqual(programCode: String) = Specification { root, _, builder ->
    val infoJoin: Join<Account, UserInfo> = root.join(Account_.info, JoinType.LEFT)
    val programJoin: Join<UserInfo, Program> = infoJoin.join(UserInfo_.program, JoinType.LEFT)

    if (programCode.isBlank()) { // Выбрать пользователей без заполненной программы
        builder.isNull(programJoin.get(Program_.code))
    } else { // Выбрать пользователей с указанной программой
        builder.equal(programJoin.get(Program_.code), programCode)
    }
}

private fun projectEqual(projectCode: String) = Specification { root, _, builder ->
    val infoJoin: Join<Account, UserInfo> = root.join(Account_.info, JoinType.LEFT)
    val projectJoin: Join<UserInfo, Project> = infoJoin.join(UserInfo_.project, JoinType.LEFT)

    if (projectCode.isBlank()) { // Выбрать пользователей без заполненного проекта
        builder.isNull(projectJoin.get(Project_.code))
    } else { // Выбрать пользователей с указанным проектом
        builder.equal(projectJoin.get(Project_.code), projectCode)
    }
}
