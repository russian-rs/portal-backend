package rs.russian.portal.report.domain.specification

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import rs.russian.generated.model.ReportFilter
import rs.russian.portal.program.domain.Program
import rs.russian.portal.program.domain.Program_
import rs.russian.portal.program.domain.Project
import rs.russian.portal.program.domain.Project_
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.domain.Report_
import rs.russian.portal.report.domain.Task
import rs.russian.portal.report.domain.Task_
import rs.russian.portal.shared.jpa.empty
import rs.russian.portal.shared.jpa.equal
import rs.russian.portal.user.domain.Account
import rs.russian.portal.user.domain.Account_

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

    filter.program?.let { programCode ->
        specification = specification.and(programEqual(programCode))
    }

    filter.project?.let { projectCode ->
        specification = specification.and(projectEqual(projectCode))
    }

    return specification
}

private fun programEqual(programCode: String) = Specification<Report> { root, query, builder ->
    val programJoin = root.join<Report, Program>(Report_.PROGRAM, JoinType.LEFT)

    if (programCode.isBlank()) { // Выбрать отчеты без заполненной программы
        builder.isNull(programJoin.get(Program_.code))
    } else { // Выбрать отчеты с указанной программой
        builder.equal(programJoin.get(Program_.code), programCode)
    }
}

private fun projectEqual(projectCode: String) = Specification<Report> { root, query, builder ->
    val projectJoin = root.join<Report, Project>(Report_.PROJECT, JoinType.LEFT)

    if (projectCode.isBlank()) { // Выбрать отчеты без заполненного проекта
        builder.isNull(projectJoin.get(Project_.code))
    } else { // Выбрать отчеты с указанным проектом
        builder.equal(projectJoin.get(Project_.code), projectCode)
    }
}
