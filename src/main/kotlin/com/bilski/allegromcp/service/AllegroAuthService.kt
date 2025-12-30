package com.bilski.allegromcp.service

import com.bilski.allegromcp.config.AllegroProperties
import com.bilski.allegromcp.model.DeviceCodeResponse
import com.bilski.allegromcp.model.TokenResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

@Service
class AllegroAuthService(
    private val allegroProperties: AllegroProperties
) {
    private val logger = LoggerFactory.getLogger(AllegroAuthService::class.java)
    private val tokenStore = ConcurrentHashMap<String, TokenResponse>()

    private val authWebClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(allegroProperties.getActiveAuthUrl())
            .build()
    }

    fun getBasicAuthHeader(): String {
        val credentials = "${allegroProperties.clientId}:${allegroProperties.clientSecret}"
        return "Basic ${Base64.getEncoder().encodeToString(credentials.toByteArray())}"
    }

    suspend fun initiateDeviceAuthorization(): DeviceCodeResponse {
        logger.info("Initiating device authorization flow")

        return authWebClient.post()
            .uri("/device")
            .header("Authorization", getBasicAuthHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("client_id", allegroProperties.clientId))
            .retrieve()
            .awaitBody<DeviceCodeResponse>()
    }

    suspend fun pollForToken(deviceCode: String): TokenResponse {
        logger.info("Polling for access token")

        val response = authWebClient.post()
            .uri("/token")
            .header("Authorization", getBasicAuthHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    .with("device_code", deviceCode)
            )
            .retrieve()
            .awaitBody<TokenResponse>()

        // Store token for later use
        tokenStore["current"] = response
        return response
    }

    suspend fun refreshToken(refreshToken: String): TokenResponse {
        logger.info("Refreshing access token")

        val response = authWebClient.post()
            .uri("/token")
            .header("Authorization", getBasicAuthHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                BodyInserters
                    .fromFormData("grant_type", "refresh_token")
                    .with("refresh_token", refreshToken)
            )
            .retrieve()
            .awaitBody<TokenResponse>()

        tokenStore["current"] = response
        return response
    }

    fun getCurrentToken(): TokenResponse? = tokenStore["current"]

    fun setToken(token: TokenResponse) {
        tokenStore["current"] = token
    }

    fun hasValidToken(): Boolean = tokenStore["current"] != null
}
