package rs.russian.portal

import io.zonky.test.db.AutoConfigureEmbeddedDatabase
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@AutoConfigureTestDatabase
@AutoConfigureEmbeddedDatabase
@ActiveProfiles("local", "no-auth", "test")
class PortalBackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}
