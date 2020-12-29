package uk.co.firstchoice_cs.core.api.magentoAPI

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import uk.co.firstchoice_cs.SavePrefs
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.magentoAdminPassword
import uk.co.firstchoice_cs.Settings.magentoAdminUsername
import uk.co.firstchoice_cs.Settings.magentoBaseUrl
import uk.co.firstchoice_cs.Settings.magentoBearerAdmin
import uk.co.firstchoice_cs.Settings.magentoBearerCustomer
import uk.co.firstchoice_cs.core.helpers.SafetyChecks.safeString


object MagentoAPI : KoinComponent {
    val client: OkHttpClient by inject(named(Settings.DEFAULT_SERVICE))
    fun getCustomerToken(): String {
       // val client = unsafeClient
        var token = ""
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("username", safeString(SavePrefs.username))
                .addFormDataPart("password", safeString(SavePrefs.password))
                .build()
        val request = Request.Builder()
                .url(magentoBaseUrl() + "integration/customer/token")
                .method("POST", body)
                .addHeader("Content-Type", "multipart/form-data")
                .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                token = response.body?.string()?:""
                return token.substring(1, token.length - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return token
    }

    fun getAdminToken(): String {
        var token = ""
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("username", magentoAdminUsername())
                .addFormDataPart("password", magentoAdminPassword())
                .build()
        val request = Request.Builder()
                .url(magentoBaseUrl() + "integration/admin/token")
                .method("POST", body)
                .addHeader("Content-Type", "multipart/form-data")
                .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                token = response.body?.string()?:""
                return token.substring(1, token.length - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return token
    }

    //
    // This gets the customer details for the logged in token
    //
    fun getCustomerDetails(): Customer? {
        var res: Customer? = null
        try {
            val request = Request.Builder()
                    .url(magentoBaseUrl() + "customers/me")
                    .method("GET", null)
                    .addHeader("Authorization", magentoBearerCustomer())
                    .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                SavePrefs.saveUserDetails(s)
                res = Gson().fromJson(s, Customer::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //
    // This creates a customer order using the admin token and a json string for the order
    //
    fun createOrder(order: String): OrderResult? {
        var res: OrderResult? = null
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = order.toRequestBody(mediaType)
        try {
            val request = Request.Builder()
                .url(magentoBaseUrl() + "orders/create")
                .method("PUT", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", magentoBearerAdmin())
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                res = Gson().fromJson(s, OrderResult::class.java)
                return res
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }
}