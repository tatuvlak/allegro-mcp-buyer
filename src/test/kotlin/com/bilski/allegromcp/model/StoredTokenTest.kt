package com.bilski.allegromcp.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StoredTokenTest {

    @Test
    fun `token is not expired before expiry timestamp`() {
        val now = System.currentTimeMillis() / 1000
        val token = TokenResponse(
            accessToken = "access",
            tokenType = "Bearer",
            refreshToken = "refresh",
            expiresIn = 3600,
            scope = "scope",
            jti = "jti"
        ).toStoredToken(issuedAtEpochSeconds = now)

        assertFalse(token.isExpired())
    }

    @Test
    fun `token is expired when refresh window reaches expiry`() {
        val now = System.currentTimeMillis() / 1000
        val token = TokenResponse(
            accessToken = "access",
            tokenType = "Bearer",
            refreshToken = "refresh",
            expiresIn = 10,
            scope = null,
            jti = null
        ).toStoredToken(issuedAtEpochSeconds = now - 9)

        assertTrue(token.isExpired(refreshBeforeExpirySeconds = 2))
    }

    @Test
    fun `stored token converts back to token response`() {
        val tokenResponse = TokenResponse(
            accessToken = "a",
            tokenType = "Bearer",
            refreshToken = "r",
            expiresIn = 1,
            scope = "s",
            jti = "j"
        )

        val restored = tokenResponse.toStoredToken(issuedAtEpochSeconds = 1).toTokenResponse()

        assertEquals(tokenResponse, restored)
    }
}
