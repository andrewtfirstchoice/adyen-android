package uk.co.firstchoice_cs.core.api.adyen

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.PaymentMethodsRequest
import java.io.IOException

fun callPayments(paymentRequest:PaymentMethodsRequest): Response?{
    val client = OkHttpClient().newBuilder()
        .build()
    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()


    val body =
        paymentRequest.toString()
            .toRequestBody(mediaType)


    val request: Request = Request.Builder()
        .url("https://checkout-test.adyen.com/v66/payments")
        .method("POST", body)
        .addHeader("Content-Type", "application/json")
        .addHeader(
            "x-API-key",
            "AQE0hmfuXNWTK0Qc+iSWm3Yrs8eTR4JOCLBLTGBEz3asrXZKjMJlrbYOOGqzhFhhsXbztkA5ihDBXVsNvuR83LVYjEgiTGAH-A8WeUsPrT2mOW4V69bVBH0Aos758Fhlf3+yGfU6lLqE=->HQr4CRsVMx>%:LM"
        )
        .build()
    try {
        return client.newCall(request).execute()
        Log.d("response", client.newCall(request).execute().toString())
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}