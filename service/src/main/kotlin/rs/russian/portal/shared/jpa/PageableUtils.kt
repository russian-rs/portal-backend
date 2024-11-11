package rs.russian.portal.shared.jpa

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest.of
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import rs.russian.generated.model.PageRequest
import rs.russian.generated.model.PageResponse

fun convert(pageRequest: PageRequest): Pageable {
    return of(
        pageRequest.pageNumber ?: 0,
        pageRequest.pageSize ?: 10,
        convertSort(pageRequest.sort) ?: Sort.unsorted()
    )
}

fun convertSort(sortList: List<String>?): Sort? {
    if (sortList.isNullOrEmpty()) {
        return null
    }
    var sort = Sort.unsorted()
    sortList.forEach { sortParam ->
        val sortPair = sortParam.split(";")
        if (sortPair.size == 2) {
            val newSort = Sort.by(Sort.Direction.fromString(sortPair[1]), sortPair[0])
            sort = sort.and(newSort)
        }
    }
    return sort
}

fun convert(page: Page<out JpaEntity<*>>): PageResponse {
    return PageResponse(
        pageNumber = page.number,
        pageSize = page.size,
        totalPages = page.totalPages,
        totalElements = page.totalElements
    )
}
