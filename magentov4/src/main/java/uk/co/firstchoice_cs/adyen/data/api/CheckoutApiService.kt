package uk.co.firstchoice_cs.adyen.data.api

import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.PaymentMethodsRequest
import uk.co.firstchoice_cs.firstchoice.BuildConfig.*

interface CheckoutApiService {
    companion object {
        private const val defaultGradleUrl = "<YOUR_SERVER_URL>"

        fun isRealUrlAvailable(): Boolean {
            return MERCHANT_SERVER_URL != defaultGradleUrl
        }
    }

    @Headers("$API_KEY_HEADER_NAME:$CHECKOUT_API_KEY")
    @POST("paymentMethods")
    fun paymentMethodsAsync(@Body paymentMethodsRequest: PaymentMethodsRequest): Deferred<Response<PaymentMethodsApiResponse>>

    // There is no native support for JSONObject in either Moshi or Gson, so using RequestBody as a work around for now
    @Headers("$API_KEY_HEADER_NAME:$CHECKOUT_API_KEY")
    @POST("payments")
    fun payments(@Body paymentsRequest: RequestBody): Call<ResponseBody>

    @Headers("$API_KEY_HEADER_NAME:$CHECKOUT_API_KEY")
    @POST("payments/details")
    fun details(@Body detailsRequest: RequestBody): Call<ResponseBody>
}