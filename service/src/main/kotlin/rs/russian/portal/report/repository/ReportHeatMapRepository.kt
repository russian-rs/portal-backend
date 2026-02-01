package rs.russian.portal.report.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import rs.russian.portal.report.domain.Report
import rs.russian.portal.report.repository.projections.VolunteerWeekProjection
import java.time.LocalDate
import java.util.*
import org.springframework.stereotype.Repository as StereotypeRepository

@StereotypeRepository
interface ReportHeatMapRepository : Repository<Report, UUID> {

    @Query(
        value = """
    WITH input_accounts AS (
      SELECT a.username
      FROM account a
      WHERE a.username IN (:usernames)
    ),
    weeks AS (
      SELECT
          -- Если период начинается с середины недели, обрезаем начало недели
          GREATEST(gs::date, CAST(:startDate AS date))          AS week_start,
          
          -- Обрезаем конец недели по endDate или концу недели
          LEAST(gs::date + interval '6 days', CAST(:endDate AS date))::date AS week_end,
          
          extract(isoyear FROM gs)::int                         AS iso_year,
          extract(week    FROM gs)::int                         AS iso_week
      FROM generate_series(
             date_trunc('week', CAST(:startDate AS date)),
             date_trunc('week', CAST(:endDate   AS date)),
             interval '1 week'
           ) gs
    ),
    grid AS (
      SELECT ia.username, w.week_start, w.week_end, w.iso_year, w.iso_week
      FROM input_accounts ia
      CROSS JOIN weeks w
    ),
    worked_minutes AS (
      SELECT
          r.user_login                                          AS username,
          date_trunc('week', t.date)::date                      AS week_start,
          SUM(t.time_spent)::bigint                             AS minutes_worked
      FROM report r
      JOIN task   t ON t.report_id = r.id
      WHERE r.deleted = FALSE
        AND r.status  = 'ACCEPTED'
        AND t.date BETWEEN CAST(:startDate AS date) AND CAST(:endDate AS date)
      GROUP BY r.user_login, date_trunc('week', t.date)::date
    ),
    weekly_base AS (
      SELECT
          g.username,
          g.week_start,
          g.week_end,
          g.iso_year,
          g.iso_week,
          COALESCE(wm.minutes_worked, 0)                        AS minutes_worked,
          
          -- Считаем количество дней пересечения контракта и (возможно усеченной) недели
          (
            SELECT COALESCE(SUM(
                GREATEST(
                    0, 
                    (LEAST(g.week_end, c.end_date) - GREATEST(g.week_start, c.start_date)) + 1
                )
            ), 0)
            FROM contract c
            WHERE c.username = g.username
              AND c.type     = 'REGULAR'
              AND c.start_date <= g.week_end
              AND c.end_date   >= g.week_start
          )                                                     AS active_days
      FROM grid g
      LEFT JOIN worked_minutes wm
             ON wm.username   = g.username
             -- Соединяем по пересечению диапазонов недель, так как week_start может быть сдвинут
             AND wm.week_start <= g.week_end 
             AND wm.week_start + interval '6 days' >= g.week_start
    )
    SELECT
        wb.username                                              AS username,
        wb.iso_year                                              AS year,
        wb.iso_week                                              AS week,
        wb.week_start                                            AS weekStart,
        wb.week_end                                              AS weekEnd,
        ROUND(wb.minutes_worked / 60.0, 2)::numeric(10,2)        AS hoursWorked,
        ROUND((wb.active_days::numeric / 7.0) * 10.0)::int       AS hoursRequired
        
    FROM weekly_base wb
    ORDER BY wb.username, wb.week_start
    """,
        nativeQuery = true
    )
    fun findVolunteerHeatmap(
        @Param("usernames") usernames: Set<String>,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<VolunteerWeekProjection>
}
