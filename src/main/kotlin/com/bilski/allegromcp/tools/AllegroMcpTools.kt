package com.bilski.allegromcp.tools

import com.bilski.allegromcp.service.AllegroApiService
import com.bilski.allegromcp.service.AllegroAuthService
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Component

@Component
class AllegroMcpTools(
    private val authService: AllegroAuthService,
    private val apiService: AllegroApiService,
    private val objectMapper: ObjectMapper
) {

    @Tool(
        name = "allegro_authenticate",
        description = """
            Start the OAuth device authorization flow for Allegro.
            This returns a URL that the user must visit to authorize the application.
            After authorization, use allegro_complete_authentication with the device_code to get the access token.
        """
    )
    fun initiateAuthentication(): String = runBlocking {
        try {
            val response = authService.initiateDeviceAuthorization()
            """
            |Authentication initiated!
            |
            |Please visit: ${response.verificationUriComplete}
            |
            |Or go to: ${response.verificationUri}
            |And enter code: ${response.userCode}
            |
            |After authorizing, call 'allegro_complete_authentication' with device_code: ${response.deviceCode}
            |
            |The code expires in ${response.expiresIn} seconds.
            """.trimMargin()
        } catch (e: Exception) {
            "Failed to initiate authentication: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_complete_authentication",
        description = "Complete the OAuth device authorization flow by exchanging the device code for an access token."
    )
    fun completeAuthentication(
        @ToolParam(description = "The device_code returned from allegro_authenticate") deviceCode: String
    ): String = runBlocking {
        try {
            val token = authService.pollForToken(deviceCode)
            """
            |Authentication successful!
            |Token type: ${token.tokenType}
            |Expires in: ${token.expiresIn} seconds
            |Scopes: ${token.scope ?: "default"}
            |
            |You can now use other Allegro tools to access your account.
            """.trimMargin()
        } catch (e: Exception) {
            "Failed to complete authentication: ${e.message}. Make sure you've authorized the application in your browser."
        }
    }

    @Tool(
        name = "allegro_check_auth_status",
        description = "Check if the user is currently authenticated with Allegro."
    )
    fun checkAuthStatus(): String {
        return if (authService.hasValidToken()) {
            "Authenticated with Allegro. Ready to make API calls. Token persistence file: ${authService.getTokenFileLocation()}"
        } else {
            "Not authenticated. Please use 'allegro_authenticate' to start the authentication flow."
        }
    }

    @Tool(
        name = "allegro_get_my_info",
        description = "Get information about the currently authenticated Allegro user."
    )
    fun getMyInfo(): String = runBlocking {
        try {
            val userInfo = apiService.getUserInfo()
            """
            |User Information:
            |  ID: ${userInfo.id}
            |  Login: ${userInfo.login}
            |  Name: ${userInfo.firstName ?: ""} ${userInfo.lastName ?: ""}
            |  Email: ${userInfo.email ?: "not available"}
            |  Company: ${userInfo.company?.name ?: "none"}
            """.trimMargin()
        } catch (e: Exception) {
            "Failed to get user info: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_get_my_orders",
        description = "Get the list of orders/purchases made by the authenticated user."
    )
    fun getMyOrders(
        @ToolParam(description = "Maximum number of orders to return (default: 10)") limit: Int = 10,
        @ToolParam(description = "Offset for pagination (default: 0)") offset: Int = 0,
        @ToolParam(description = "Filter by status: BOUGHT, FILLED_IN, READY_FOR_PROCESSING, CANCELLED, etc.") status: String? = null
    ): String = runBlocking {
        try {
            val orders = apiService.getMyOrders(limit, offset, status)
            if (orders.orders.isNullOrEmpty()) {
                "No orders found."
            } else {
                buildString {
                    appendLine("Found ${orders.orders.size} orders:")
                    appendLine()
                    orders.orders.forEach { order ->
                        appendLine("Order ID: ${order.id}")
                        appendLine("  Status: ${order.status}")
                        appendLine("  Seller: ${order.seller?.login ?: "unknown"}")
                        appendLine("  Created: ${order.createdAt}")
                        order.lineItems?.forEach { item ->
                            appendLine("  - ${item.offer?.name ?: "Unknown item"}")
                            appendLine("    Quantity: ${item.quantity}")
                            appendLine("    Price: ${item.price?.amount} ${item.price?.currency}")
                        }
                        order.payment?.let {
                            appendLine("  Total: ${it.totalAmount?.amount} ${it.totalAmount?.currency}")
                            appendLine("  Payment Status: ${it.status}")
                        }
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to get orders: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_get_order_details",
        description = "Get detailed information about a specific order."
    )
    fun getOrderDetails(
        @ToolParam(description = "The order ID to get details for") orderId: String
    ): String = runBlocking {
        try {
            val order = apiService.getOrderDetails(orderId)
            buildString {
                appendLine("Order Details: ${order.id}")
                appendLine("Status: ${order.status}")
                appendLine("Bought at: ${order.boughtAt}")
                appendLine("Updated at: ${order.updatedAt}")
                appendLine()
                appendLine("Buyer:")
                order.buyer?.let {
                    appendLine("  Login: ${it.login}")
                    appendLine("  Name: ${it.firstName} ${it.lastName}")
                    appendLine("  Email: ${it.email}")
                }
                appendLine()
                appendLine("Items:")
                order.lineItems?.forEach { item ->
                    appendLine("  - ${item.offer?.name}")
                    appendLine("    Offer ID: ${item.offer?.id}")
                    appendLine("    Quantity: ${item.quantity}")
                    appendLine("    Original Price: ${item.originalPrice?.amount} ${item.originalPrice?.currency}")
                    appendLine("    Final Price: ${item.price?.amount} ${item.price?.currency}")
                }
                appendLine()
                order.payment?.let {
                    appendLine("Payment:")
                    appendLine("  Type: ${it.type}")
                    appendLine("  Amount: ${it.paidAmount?.amount} ${it.paidAmount?.currency}")
                    appendLine("  Finished at: ${it.finishedAt}")
                }
                order.summary?.let {
                    appendLine()
                    appendLine("Total to pay: ${it.totalToPay?.amount} ${it.totalToPay?.currency}")
                }
            }
        } catch (e: Exception) {
            "Failed to get order details: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_get_watched_offers",
        description = "Get the list of offers that the user is watching/following."
    )
    fun getWatchedOffers(
        @ToolParam(description = "Maximum number of offers to return (default: 10)") limit: Int = 10,
        @ToolParam(description = "Offset for pagination (default: 0)") offset: Int = 0
    ): String = runBlocking {
        try {
            val watched = apiService.getWatchedOffers(limit, offset)
            if (watched.offers.isNullOrEmpty()) {
                "No watched offers found."
            } else {
                buildString {
                    appendLine("Watched Offers (${watched.offers.size}):")
                    appendLine()
                    watched.offers.forEach { offer ->
                        appendLine("- ${offer.name}")
                        appendLine("  ID: ${offer.id}")
                        appendLine("  Price: ${offer.sellingMode?.price?.amount} ${offer.sellingMode?.price?.currency}")
                        appendLine("  Seller: ${offer.seller?.login}")
                        offer.primaryImage?.url?.let { appendLine("  Image: $it") }
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to get watched offers: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_get_bought_items",
        description = "Get items that the user has bought (with BOUGHT status)."
    )
    fun getBoughtItems(
        @ToolParam(description = "Maximum number of items to return (default: 10)") limit: Int = 10,
        @ToolParam(description = "Offset for pagination (default: 0)") offset: Int = 0
    ): String = runBlocking {
        try {
            val items = apiService.getBoughtItems(limit, offset)
            if (items.orders.isNullOrEmpty()) {
                "No bought items found."
            } else {
                buildString {
                    appendLine("Bought Items:")
                    appendLine()
                    items.orders.forEach { order ->
                        appendLine("Order: ${order.id}")
                        appendLine("  Status: ${order.status}")
                        appendLine("  Created: ${order.createdAt}")
                        order.lineItems?.forEach { item ->
                            appendLine("  - ${item.offer?.name}")
                            appendLine("    Price: ${item.price?.amount} ${item.price?.currency}")
                            appendLine("    Quantity: ${item.quantity}")
                        }
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to get bought items: ${e.message}"
        }
    }

    @Tool(
        name = "allegro_search_products",
        description = "Search Allegro products by phrase and return matching offers."
    )
    fun searchProducts(
        @ToolParam(description = "Search phrase (e.g. 'laptop dell')") phrase: String,
        @ToolParam(description = "Maximum number of offers to return (default: 10)") limit: Int = 10,
        @ToolParam(description = "Offset for pagination (default: 0)") offset: Int = 0
    ): String = runBlocking {
        try {
            val normalizedPhrase = phrase.trim()
            if (normalizedPhrase.isEmpty()) {
                return@runBlocking "Search phrase cannot be empty."
            }

            val searchResults = apiService.searchProducts(normalizedPhrase, limit, offset)
            val promoted = searchResults.items?.promoted.orEmpty()
            val regular = searchResults.items?.regular.orEmpty()
            val offers = (promoted + regular).distinctBy { it.id }

            if (offers.isEmpty()) {
                "No products found for '$normalizedPhrase'."
            } else {
                buildString {
                    appendLine("Found ${offers.size} products for '$normalizedPhrase':")
                    appendLine()
                    offers.forEach { offer ->
                        appendLine("- ${offer.name ?: "Unnamed offer"}")
                        appendLine("  ID: ${offer.id ?: "unknown"}")
                        appendLine("  Price: ${offer.sellingMode?.price?.amount ?: "?"} ${offer.sellingMode?.price?.currency ?: ""}".trim())
                        appendLine("  Seller: ${offer.seller?.login ?: "unknown"}")
                        offer.images?.firstOrNull()?.url?.let { appendLine("  Image: $it") }
                        appendLine()
                    }
                }
            }
        } catch (e: Exception) {
            "Failed to search products: ${e.message}"
        }
    }
}
