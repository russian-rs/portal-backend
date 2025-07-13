package rs.russian.portal.report.domain.specification

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import rs.russian.generated.model.ReportFilter
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Project
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Report_
import rs.russian.portal.report.domain.Task
import rs.russian.portal.report.domain.Task_
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.equal
import rs.russian.portal.user.domain.*

fun from(filter: ReportFilter): Specification<Report> {
    var specification = empty<Report>()

    val login = filter.login
    if (!login.isNullOrBlank()) {
        specification = specification.and(equal(Report_.ACCOUNT, Account_.USERNAME, login))
    }

    val dateFrom = filter.dateFrom
    val dateTo = filter.dateTo
    if (dateFrom != null && dateTo != null) {
        specification = specification.and { root, _, builder ->
            val join = root.join<Report, Task>(Report_.TASKS, JoinType.RIGHT)
            builder.between(join.get(Task_.DATE), dateFrom, dateTo)
        }
    } else if (dateFrom != null) {
        specification = specification.and { root, _, builder ->
            val join = root.join<Report, Task>(Report_.TASKS, JoinType.RIGHT)
            builder.greaterThanOrEqualTo(join.get(Task_.DATE), dateFrom)
        }
    } else if (dateTo != null) {
        specification = specification.and { root, _, builder ->
            val join = root.join<Report, Task>(Report_.TASKS, JoinType.RIGHT)
            builder.lessThanOrEqualTo(join.get(Task_.DATE), dateTo)
        }
    }

    val status = filter.status
    if (!status.isNullOrBlank()) {
        specification = specification.and(equal(Report_.STATUS, status))
    }

    filter.program
        .let { code ->
            when {
                code == null -> null
                code.isBlank() -> Specification<Report> { root, query, builder ->
                    query!!.distinct(true)
                    val account = root.join<Report, Account>(Report_.ACCOUNT)
                    val info = account.join<Account, UserInfo>(Account_.INFO)
                    val program = info.join<UserInfo, Program>(UserInfo_.PROGRAM, JoinType.LEFT)
                    builder.isNull(program.get<String>("code"))
                }
                else -> Specification<Report> { root, query, builder ->
                    query!!.distinct(true)
                    val account = root.join<Report, Account>(Report_.ACCOUNT)
                    val info = account.join<Account, UserInfo>(Account_.INFO)
                    val program = info.join<UserInfo, Program>(UserInfo_.PROGRAM, JoinType.LEFT)
                    builder.equal(program.get<String>("code"), code)
                }
            }
        }
        ?.let { specification = specification.and(it) }

    filter.project
        .let { code ->
            when {
                code == null -> null
                code.isBlank() -> Specification<Report> { root, query, builder ->
                    query!!.distinct(true)
                    val account = root.join<Report, Account>(Report_.ACCOUNT)
                    val info = account.join<Account, UserInfo>(Account_.INFO)
                    val project = info.join<UserInfo, Project>(UserInfo_.PROJECT, JoinType.LEFT)
                    builder.isNull(project.get<String>("code"))
                }
                else -> Specification<Report> { root, query, builder ->
                    query!!.distinct(true)
                    val account = root.join<Report, Account>(Report_.ACCOUNT)
                    val info = account.join<Account, UserInfo>(Account_.INFO)
                    val project = info.join<UserInfo, Project>(UserInfo_.PROJECT, JoinType.LEFT)
                    builder.equal(project.get<String>("code"), code)
                }
            }
        }
        ?.let { specification = specification.and(it) }

    return specification
}
