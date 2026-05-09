package com.bilski.allegromcp.service

import com.bilski.allegromcp.config.AllegroProperties
import com.bilski.allegromcp.model.DeviceCodeResponse
import com.bilski.allegromcp.model.StoredToken
import com.bilski.allegromcp.model.TokenResponse
import com.bilski.allegromcp.model.toStoredToken
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference

@Service
class AllegroAuthService(
    private val allegroProperties: AllegroProperties,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(AllegroAuthService::class.java)
    private val tokenStore = AtomicReference<StoredToken?>()
    private val tokenMutex = Mutex()
    private val tokenPath: Path by lazy { Paths.get(allegroProperties.auth.tokenFile).toAbsolutePath().normalize() }

    private val authWebClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(allegroProperties.getActiveAuthUrl())
            .build()
    }

    @PostConstruct
    fun initializeTokenStore() {
        loadPersistedToken()
    }

    private fun resolveSecret(value: String, filePath: String): String {
        if (value.isNotBlank()) {
            return value
        }
        if (filePath.isBlank()) {
            return ""
        }

        return try {
            Files.readString(Paths.get(filePath)).trim()
        } catch (e: Exception) {
            logger.warn("Failed to read secret from file '{}': {}", filePath, e.message)
            ""
        }
    }

    private fun getClientId(): String = resolveSecret(allegroProperties.clientId, allegroProperties.clientIdFile)

    private fun getClientSecret(): String = resolveSecret(allegroProperties.clientSecret, allegroProperties.clientSecretFile)

    private fun requireClientCredentials() {
        val clientId = getClientId()
        val clientSecret = getClientSecret()
        require(clientId.isNotBlank() && clientSecret.isNotBlank()) {
            "Allegro client credentials are not configured. Set ALLEGRO_CLIENT_ID and ALLEGRO_CLIENT_SECRET (or *_FILE variants)."
        }
    }

    private fun getBasicAuthHeader(): String {
        requireClientCredentials()
        val credentials = "${getClientId()}:${getClientSecret()}"
        return "Basic ${Base64.getEncoder().encodeToString(credentials.toByteArray())}"
    }

    suspend fun initiateDeviceAuthorization(): DeviceCodeResponse {
        logger.info("Initiating device authorization flow")

        return authWebClient.post()
            .uri("/device")
            .header("Authorization", getBasicAuthHeader())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("client_id", getClientId()))
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

        storeToken(response)
        return response
    }

    private suspend fun refreshAccessToken(refreshToken: String): TokenResponse {
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

        val mergedResponse = if (response.refreshToken.isNullOrBlank()) {
            response.copy(refreshToken = refreshToken)
        } else {
            response
        }
        storeToken(mergedResponse)
        return mergedResponse
    }

    private fun storeToken(token: TokenResponse) {
        val storedToken = token.toStoredToken()
        tokenStore.set(storedToken)
        persistToken(storedToken)
    }

    private fun persistToken(token: StoredToken) {
        try {
            val tokenDirectory = tokenPath.parent ?: Paths.get(".").toAbsolutePath().normalize()
            Files.createDirectories(tokenDirectory)
            setPosixPermissions(tokenDirectory, setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            ))

            val tempFile = Files.createTempFile(tokenDirectory, "token-", ".tmp")
            objectMapper.writeValue(tempFile.toFile(), token)
            setPosixPermissions(tempFile, setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ))
            Files.move(tempFile, tokenPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            setPosixPermissions(tokenPath, setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ))
        } catch (e: Exception) {
            logger.warn("Failed to persist token to '{}': {}", tokenPath, e.message)
        }
    }

    private fun loadPersistedToken() {
        if (!Files.exists(tokenPath)) {
            return
        }
        try {
            val persistedToken = objectMapper.readValue(tokenPath.toFile(), StoredToken::class.java)
            tokenStore.set(persistedToken)
            logger.info("Loaded persisted Allegro token from {}", tokenPath)
        } catch (e: Exception) {
            logger.warn("Failed to read persisted token from '{}': {}", tokenPath, e.message)
        }
    }

    private fun setPosixPermissions(path: Path, permissions: Set<PosixFilePermission>) {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(path, permissions)
        }
    }

    suspend fun getValidAccessToken(): String = tokenMutex.withLock {
        val refreshBeforeExpirySeconds = allegroProperties.auth.refreshBeforeExpirySeconds
        val currentToken = tokenStore.get()
            ?: throw IllegalStateException("No access token available. Please authenticate first using the authenticate tool.")

        if (!currentToken.isExpired(refreshBeforeExpirySeconds)) {
            return@withLock currentToken.accessToken
        }

        val refreshToken = currentToken.refreshToken
            ?: throw IllegalStateException("Access token expired and no refresh token is available. Please authenticate again.")

        logger.info("Access token expired or close to expiry, refreshing automatically")
        val refreshed = refreshAccessToken(refreshToken)
        refreshed.accessToken
    }

    fun getCurrentToken(): TokenResponse? = tokenStore.get()?.toTokenResponse()

    fun setToken(token: TokenResponse) {
        storeToken(token)
    }

    fun hasValidToken(): Boolean {
        val refreshBeforeExpirySeconds = allegroProperties.auth.refreshBeforeExpirySeconds
        val currentToken = tokenStore.get() ?: return false
        return !currentToken.isExpired(refreshBeforeExpirySeconds) || !currentToken.refreshToken.isNullOrBlank()
    }

    fun getTokenFileLocation(): String = tokenPath.toString()

    suspend fun clearToken() {
        tokenMutex.withLock {
            tokenStore.set(null)
            try {
                Files.deleteIfExists(tokenPath)
            } catch (e: Exception) {
                logger.warn("Failed to delete token file '{}': {}", tokenPath, e.message)
            }
        }
    }
}
