package rs.russian.portal.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

@Component
class SchemaInitializer : BeanPostProcessor {
    @Value("\${spring.liquibase.default-schema}")
    private val schemaName: String? = null

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is DataSource) {
            try {
                val conn: Connection = bean.connection
                val statement: Statement = conn.createStatement()
                statement.execute(String.format("CREATE SCHEMA IF NOT EXISTS %s", schemaName))
            } catch (e: SQLException) {
                throw RuntimeException(e)
            }
        }
        return bean
    }
}
