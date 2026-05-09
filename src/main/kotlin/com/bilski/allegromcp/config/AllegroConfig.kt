package com.bilski.allegromcp.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@ConfigurationProperties(prefix = "allegro")
data class AllegroProperties(
    val api: ApiProperties = ApiProperties(),
    val sandbox: Boolean = false,
    val clientId: String = "",
    val clientSecret: String = "",
    val clientIdFile: String = "",
    val clientSecretFile: String = "",
    val auth: AuthProperties = AuthProperties()
) {
    data class ApiProperties(
        val baseUrl: String = "https://api.allegro.pl",
        val authUrl: String = "https://allegro.pl/auth/oauth",
        val sandboxBaseUrl: String = "https://api.allegro.pl.allegrosandbox.pl",
        val sandboxAuthUrl: String = "https://allegro.pl.allegrosandbox.pl/auth/oauth"
    )

    data class AuthProperties(
        val tokenFile: String = "",
        val refreshBeforeExpirySeconds: Long = 60
    )

    fun getActiveBaseUrl(): String = if (sandbox) api.sandboxBaseUrl else api.baseUrl
    fun getActiveAuthUrl(): String = if (sandbox) api.sandboxAuthUrl else api.authUrl
}

@Configuration
@EnableConfigurationProperties(AllegroProperties::class)
class AllegroConfig(
    private val allegroProperties: AllegroProperties
) {

    @Bean
    fun allegroWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl(allegroProperties.getActiveBaseUrl())
            .build()
    }
}
