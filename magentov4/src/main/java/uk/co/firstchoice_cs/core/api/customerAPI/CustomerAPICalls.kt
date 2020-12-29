package uk.co.firstchoice_cs.core.api.customerAPI

import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.GlobalAppData
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.customerBaseUrl
import uk.co.firstchoice_cs.Settings.bearerCustomerAuth
import uk.co.firstchoice_cs.Settings.customerPassword
import uk.co.firstchoice_cs.Settings.customerUsername
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.firstchoice.BuildConfig


object CustomerAPICalls : KoinComponent {
    private val okHttpClient: OkHttpClient by inject(named(Settings.UNSAFE_SERVICE))


    fun auth(): Auth? {
        var res: Auth? = null
        val response: Response?
        val url = customerBaseUrl() + "Auth/login"
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

        val body =
            "{\"username\":\"${customerUsername()}\",\"password\":\"${customerPassword()}\"}".toRequestBody(
                mediaType
            )
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                res = Gson().fromJson(responseBody, Auth::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun isDuplicatePO(customerNum:String, poNumber: String): Boolean {

       // https://api.firstchoice-cs.co.uk/ig11/Order/CheckPo?poNum=test&custNum=123
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = "{\"custNum\": $customerNum \"poNum\": $poNumber}".toRequestBody(mediaType)
        val url = customerBaseUrl() + "Order/CheckPo"
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                if(response.code==204)
                    false
                else
                    return true
            }

        } catch (e: Exception) {
            App.instance.setCustomerAPIToken("", 0)
            e.printStackTrace()
        }
        return false
    }

    fun getCustomer(customerNum: Int): Customer? {
        var res: Customer? = null
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = "{\"custNum\": $customerNum}".toRequestBody(mediaType)
        val url = customerBaseUrl() + "Customer"
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                res = Gson().fromJson(responseBody, Customer::class.java)
            }

        } catch (e: Exception) {
            App.instance.setCustomerAPIToken("", 0)
            e.printStackTrace()
        }
        return res
    }

    fun getPrice(sku: String, qty: Int): Product? {
        var res: Product? = null
        val client = OkHttpClient().newBuilder().build()
        val mediaType = "application/json".toMediaTypeOrNull()
        if (App.customer == null)
            LoggingHelper.debugMsg("getPrice", "app customer == null")

        val custID = if (App.loginState.value==Settings.LoginState.LOGGED_IN_AS_GUEST)
            GlobalAppData().nonB2BCustID
        else App.customer?.custId

        if (BuildConfig.DEBUG && custID.isNullOrBlank()) {
        if (App.loginState.value==Settings.LoginState.LOGGED_IN_AS_GUEST)
            LoggingHelper.debugMsg("getPrice", "customer id is null : guest")
        else
            LoggingHelper.debugMsg("getPrice", "customer id is null : nonB2BCustID")
          }

        val bodyStr = "{  \"custId\": \"${custID}\",  \"partNum\": \"${sku}\",  \"qty\": ${qty}}"
        val body = bodyStr.toRequestBody(mediaType)
        val url = customerBaseUrl() + "PriceStock/CustomerPrice"
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val ps = Gson().fromJson(responseBody, PriceStock::class.java)

                if (ps != null && !ps.product.isNullOrEmpty()) {
                    res = ps.product[0]
                    App.globalData.addPriceStock(sku, qty, res) //here we add the price stock
                } else {
                    LoggingHelper.debugMsg("getPrice", "proce stock is null")
                }
            } else {
                LoggingHelper.debugMsg("getPrice", "response not successful")
            }

        } catch (e: Exception) {
            LoggingHelper.debugMsg("getPrice", "exception = " + e.localizedMessage)
            e.printStackTrace()
        }
        return res
    }

    fun getAddresses(customerNum: Int): Addresses? {
        var res: Addresses? = null
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json".toMediaTypeOrNull()
        val url = customerBaseUrl() + "Customer/AddressList"
        val body = "{\"custNum\": $customerNum}".toRequestBody(mediaType)
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                res = Gson().fromJson(responseBody, Addresses::class.java)
            }

        } catch (e: Exception) {
            App.instance.setCustomerAPIToken("", 0)
            e.printStackTrace()
        }
        return res
    }

    fun getOrders(customerNum: Int): Orders? {
        var res: Orders? = null
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json".toMediaTypeOrNull()
        val url = customerBaseUrl() + "Order/List"
        val body = "{\"custNum\": $customerNum}".toRequestBody(mediaType)
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                res = Gson().fromJson(responseBody, Orders::class.java)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun getOrderDetails(customerNum: Int, orderID: Int): OrderDetails? {
        var res: OrderDetails? = null
        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json".toMediaTypeOrNull()
        val url = customerBaseUrl() + "Order/status"
        val body = "{\"custNum\": $customerNum,\"orderNum\": $orderID}".toRequestBody(mediaType)
        try {
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", bearerCustomerAuth())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                res = Gson().fromJson(responseBody, OrderDetails::class.java)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }
}