package com.bilski.allegromcp.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

// OAuth Models
data class DeviceCodeResponse(
    @JsonProperty("device_code") val deviceCode: String,
    @JsonProperty("user_code") val userCode: String,
    @JsonProperty("verification_uri") val verificationUri: String,
    @JsonProperty("verification_uri_complete") val verificationUriComplete: String,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("interval") val interval: Int
)

data class TokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("token_type") val tokenType: String,
    @JsonProperty("refresh_token") val refreshToken: String?,
    @JsonProperty("expires_in") val expiresIn: Int,
    @JsonProperty("scope") val scope: String?,
    @JsonProperty("jti") val jti: String?
)

// Order Models
@JsonIgnoreProperties(ignoreUnknown = true)
data class OrdersResponse(
    val checkoutForms: List<Order>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Order(
    val id: String,
    val status: String,
    val buyer: Buyer?,
    val payment: Payment?,
    val lineItems: List<LineItem>?,
    val summary: OrderSummary?,
    val updatedAt: String?,
    val boughtAt: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Buyer(
    val id: String?,
    val email: String?,
    val login: String?,
    val firstName: String?,
    val lastName: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Payment(
    val id: String?,
    val type: String?,
    val paidAmount: Amount?,
    val finishedAt: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Amount(
    val amount: String?,
    val currency: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LineItem(
    val id: String?,
    val offer: OfferReference?,
    val quantity: Int?,
    val originalPrice: Amount?,
    val price: Amount?,
    val boughtAt: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OfferReference(
    val id: String?,
    val name: String?,
    val external: ExternalId?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExternalId(
    val id: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OrderSummary(
    val totalToPay: Amount?
)

// My Orders (as buyer)
@JsonIgnoreProperties(ignoreUnknown = true)
data class MyOrdersResponse(
    val orders: List<MyOrder>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MyOrder(
    val id: String,
    val status: String?,
    val seller: Seller?,
    val lineItems: List<MyOrderLineItem>?,
    val payment: MyOrderPayment?,
    val createdAt: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Seller(
    val id: String?,
    val login: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MyOrderLineItem(
    val id: String?,
    val offer: MyOrderOffer?,
    val quantity: Int?,
    val price: Amount?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MyOrderOffer(
    val id: String?,
    val name: String?,
    val imageUrl: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class MyOrderPayment(
    val id: String?,
    val status: String?,
    val totalAmount: Amount?
)

// Watched Offers
@JsonIgnoreProperties(ignoreUnknown = true)
data class WatchedOffersResponse(
    val offers: List<WatchedOffer>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WatchedOffer(
    val id: String?,
    val name: String?,
    val sellingMode: SellingMode?,
    val primaryImage: Image?,
    val seller: Seller?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SellingMode(
    val format: String?,
    val price: Amount?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Image(
    val url: String?
)

// User Info
@JsonIgnoreProperties(ignoreUnknown = true)
data class UserInfo(
    val id: String?,
    val login: String?,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val company: Company?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Company(
    val name: String?
)
