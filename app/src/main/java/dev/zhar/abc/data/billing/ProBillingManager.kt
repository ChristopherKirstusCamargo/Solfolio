package dev.zhar.abc.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import dev.zhar.abc.data.SettingsStore
import dev.zhar.abc.domain.ProStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProBillingManager(context: Context, private val settingsStore: SettingsStore) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(ProStatus())
    val status: StateFlow<ProStatus> = _status.asStateFlow()
    private var productDetails: ProductDetails? = null
    private var connecting = false

    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        scope.launch {
            val cached = settingsStore.settings.first().proEntitled
            _status.value = _status.value.copy(owned = cached)
        }
    }

    @Synchronized fun start() {
        if (client.isReady) { refresh(); return }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
                else _status.value = _status.value.copy(message = "Google Play indisponível no momento.")
            }
            override fun onBillingServiceDisconnected() { connecting = false }
        })
    }

    fun stop() { /* A reconexão automática permanece inativa sem novas chamadas. */ }

    fun purchase(activity: Activity): BillingResult {
        val details = productDetails ?: return errorResult("O produto PRO ainda não está disponível.")
        val offer = details.oneTimePurchaseOfferDetailsList?.firstOrNull()
            ?: return errorResult("Nenhuma oferta PRO disponível.")
        val productBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
        offer.offerToken?.let(productBuilder::setOfferToken)
        val productParams = productBuilder.build()
        return client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(productParams)).build(),
        )
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty(), authoritative = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _status.value = _status.value.copy(message = "A compra não pôde ser concluída.")
        }
    }

    private fun refresh() {
        queryProduct()
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) processPurchases(purchases, authoritative = true)
        }
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, detailsResult ->
            val details = detailsResult.productDetailsList.firstOrNull()
            productDetails = details
            val price = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            _status.value = _status.value.copy(
                available = result.responseCode == BillingClient.BillingResponseCode.OK && details != null,
                formattedPrice = price ?: _status.value.formattedPrice,
            )
        }
    }

    private fun processPurchases(purchases: List<Purchase>, authoritative: Boolean) {
        val purchase = purchases.firstOrNull { PRODUCT_ID in it.products }
        val owned = purchase?.purchaseState == Purchase.PurchaseState.PURCHASED
        val pending = purchase?.purchaseState == Purchase.PurchaseState.PENDING
        _status.value = _status.value.copy(owned = owned || (!authoritative && _status.value.owned), pending = pending, message = null)
        scope.launch {
            if (authoritative || owned) settingsStore.setProEntitled(owned)
        }
        if (owned && purchase != null && !purchase.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()) { }
        }
    }

    private fun errorResult(message: String) = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.ERROR)
        .setDebugMessage(message)
        .build()

    companion object { const val PRODUCT_ID = "solfolio_pro_lifetime" }
}
