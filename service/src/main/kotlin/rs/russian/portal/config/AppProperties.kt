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
    val bucket: String,
    val bucketService: String
)

@ConfigurationProperties(prefix = "app.authentik")
data class AuthentikProperties(
    val baseUrl: String,
    val apiKey: String
)

@ConfigurationProperties(prefix = "app.wordpress")
data class WordpressProperties(
    val instances: List<WordpressInstance>
)

data class WordpressInstance(
    val name: String,
    val baseUrl: String,
    val username: String,
    val password: String
)

@ConfigurationProperties(prefix = "app.outline")
data class OutlineProperties(
    val apiKey: String,
    val baseUrl: String
)
