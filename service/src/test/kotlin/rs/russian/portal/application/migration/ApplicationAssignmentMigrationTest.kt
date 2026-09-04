package rs.russian.portal.application.migration

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.SQLException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApplicationAssignmentMigrationTest {
    @Test
    fun `migration preserves old rows validates employees and supports clearing deletion and rollback`() {
        EmbeddedPostgres.builder().start().use { postgres ->
            postgres.postgresDatabase.connection.use { connection ->
                connection.createStatement().use { sql ->
                    sql.execute("CREATE TABLE account (username VARCHAR(255) PRIMARY KEY)")
                    sql.execute("CREATE TABLE application (id INT PRIMARY KEY)")
                    sql.execute("INSERT INTO application VALUES (1)")
                    sql.execute("INSERT INTO account VALUES ('employee')")
                }
                Liquibase("liquibase/release-1.21.0/_changelog.yaml", ClassLoaderResourceAccessor(), JdbcConnection(connection)).use { liquibase ->
                    liquibase.update(Contexts(), LabelExpression())
                    connection.autoCommit = true
                    connection.createStatement().use { sql ->
                        sql.executeQuery("SELECT assignee FROM application WHERE id = 1").use { result ->
                            result.next(); assertNull(result.getString(1))
                        }
                        sql.execute("UPDATE application SET assignee = 'employee' WHERE id = 1")
                        sql.execute("UPDATE account SET username = 'renamed' WHERE username = 'employee'")
                        sql.executeQuery("SELECT assignee FROM application WHERE id = 1").use { result ->
                            result.next(); assertEquals("renamed", result.getString(1))
                        }
                        assertThrows<SQLException> { sql.execute("UPDATE application SET assignee = 'missing' WHERE id = 1") }
                        sql.execute("DELETE FROM account WHERE username = 'renamed'")
                        sql.executeQuery("SELECT assignee FROM application WHERE id = 1").use { result ->
                            result.next(); assertNull(result.getString(1))
                        }
                    }
                    liquibase.rollback(1, Contexts(), LabelExpression())
                    connection.createStatement().use { sql ->
                        sql.executeQuery("SELECT count(*) FROM application").use { result ->
                            result.next(); assertEquals(1, result.getInt(1))
                        }
                    }
                }
            }
        }
    }
}
