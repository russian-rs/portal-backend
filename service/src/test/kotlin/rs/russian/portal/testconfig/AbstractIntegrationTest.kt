package rs.russian.portal.testconfig

import io.github.serpro69.kfaker.provider.Internet
import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.properties.Delegates

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("local", "no-auth", "test")
@Import(TestContainersConfiguration::class)
abstract class AbstractIntegrationTest
