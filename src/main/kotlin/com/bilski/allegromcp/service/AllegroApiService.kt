package com.bilski.allegromcp.service

import com.bilski.allegromcp.config.AllegroProperties
import com.bilski.allegromcp.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitBodyOrNull

@Service
class AllegroApiService(
    private val allegroProperties: AllegroProperties,
    private val authService: AllegroAuthService
) {
    private val logger = LoggerFactory.getLogger(AllegroApiService::class.java)

    private val apiWebClient: WebClient by lazy {
        WebClient.builder()
            .baseUrl(allegroProperties.getActiveBaseUrl())
            .build()
    }

    private fun getAuthHeader(): String {
        val token = authService.getCurrentToken()
            ?: throw IllegalStateException("No access token available. Please authenticate first using the authenticate tool.")
        return "Bearer ${token.accessToken}"
    }

    suspend fun getMyOrders(
        limit: Int = 10,
        offset: Int = 0,
        status: String? = null
    ): MyOrdersResponse {
        logger.info("Fetching user orders with limit=$limit, offset=$offset, status=$status")

        val uri = buildString {
            append("/order/checkout-forms?limit=$limit&offset=$offset")
            status?.let { append("&status=$it") }
        }

        return apiWebClient.get()
            .uri(uri)
            .header("Authorization", getAuthHeader())
            .header("Accept", "application/vnd.allegro.public.v1+json")
            .retrieve()
            .awaitBody()
    }

    suspend fun getOrderDetails(orderId: String): Order {
        logger.info("Fetching order details for: $orderId")

        return apiWebClient.get()
            .uri("/order/checkout-forms/$orderId")
            .header("Authorization", getAuthHeader())
            .header("Accept", "application/vnd.allegro.public.v1+json")
            .retrieve()
            .awaitBody()
    }

    suspend fun getWatchedOffers(limit: Int = 10, offset: Int = 0): WatchedOffersResponse {
        logger.info("Fetching watched offers with limit=$limit, offset=$offset")

        return apiWebClient.get()
            .uri("/sale/offer-watching?limit=$limit&offset=$offset")
            .header("Authorization", getAuthHeader())
            .header("Accept", "application/vnd.allegro.public.v1+json")
            .retrieve()
            .awaitBodyOrNull() ?: WatchedOffersResponse(emptyList())
    }

    suspend fun getUserInfo(): UserInfo {
        logger.info("Fetching current user info")

        return apiWebClient.get()
            .uri("/me")
            .header("Authorization", getAuthHeader())
            .header("Accept", "application/vnd.allegro.public.v1+json")
            .retrieve()
            .awaitBody()
    }

    suspend fun getBoughtItems(
        limit: Int = 10,
        offset: Int = 0
    ): MyOrdersResponse {
        logger.info("Fetching bought items with limit=$limit, offset=$offset")

        return apiWebClient.get()
            .uri("/order/checkout-forms?limit=$limit&offset=$offset&fulfillment.status=BOUGHT")
            .header("Authorization", getAuthHeader())
            .header("Accept", "application/vnd.allegro.public.v1+json")
            .retrieve()
            .awaitBody()
    }
}
