package rs.russian.portal.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val frontendUri: String
)

@ConfigurationProperties(prefix = "app.s3")
data class S3Properties(
    val accessKey: String,
    val secretKey: String,
    val region: String,
    val endpoint: String,
    val presignDuration: Duration,
    val bucket: String
)
