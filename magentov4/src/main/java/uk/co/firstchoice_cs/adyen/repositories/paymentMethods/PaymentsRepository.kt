/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package uk.co.firstchoice_cs.adyen.repositories.paymentMethods

import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import uk.co.firstchoice_cs.adyen.data.api.CheckoutApiService
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.PaymentMethodsRequest
import uk.co.firstchoice_cs.adyen.repositories.BaseRepository
import uk.co.firstchoice_cs.firstchoice.BuildConfig.*

interface PaymentsRepository {
    suspend fun getPaymentMethods(paymentMethodsRequest: PaymentMethodsRequest): PaymentMethodsApiResponse?
    fun paymentsRequest(paymentsRequest: RequestBody): Call<ResponseBody>
    fun detailsRequest(paymentsRequest: RequestBody): Call<ResponseBody>
}

class PaymentsRepositoryImpl(private val checkoutApiService: CheckoutApiService) : PaymentsRepository, BaseRepository() {

    override suspend fun getPaymentMethods(paymentMethodsRequest: PaymentMethodsRequest): PaymentMethodsApiResponse? {

        val client = OkHttpClient().newBuilder()
            .build()
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val gson = Gson()
        val body = gson.toJson(paymentMethodsRequest).toString().toRequestBody(mediaType)

        val request = okhttp3.Request.Builder()
            .url(MERCHANT_SERVER_URL + "paymentMethods?")
            .method("POST", body)
            .addHeader("Content-Type", "application/json")
            .addHeader(
                API_KEY_HEADER_NAME,
                CHECKOUT_API_KEY
            )
            .build()
        val response: Response = client.newCall(request).execute()
        print(response)
        if (response.isSuccessful) {
            val responseBody = response.body?.string()
            val jsonObj = JSONObject(responseBody)
            return PaymentMethodsApiResponse.SERIALIZER.deserialize(jsonObj)
        }
        return null
    }


    override fun paymentsRequest(paymentsRequest: RequestBody): Call<ResponseBody> {
        return checkoutApiService.payments(paymentsRequest)
    }

    override fun detailsRequest(paymentsRequest: RequestBody): Call<ResponseBody> {
        return checkoutApiService.details(paymentsRequest)
    }
}
