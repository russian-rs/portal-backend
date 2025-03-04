package rs.russian.portal

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class PortalBackendApplication

fun main(args: Array<String>) {
	runApplication<PortalBackendApplication>(*args)
}
