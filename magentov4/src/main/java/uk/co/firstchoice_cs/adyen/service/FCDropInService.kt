package uk.co.firstchoice_cs.adyen.service

import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.core.model.JsonUtils
import com.adyen.checkout.dropin.service.SimplifiedDropInService
import com.adyen.checkout.redirect.RedirectComponent
import com.google.gson.Gson
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import org.koin.android.ext.android.inject
import retrofit2.Call
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.adyen.data.api.ResultData
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.AdditionalData
import uk.co.firstchoice_cs.adyen.data.storage.KeyValueStorage
import uk.co.firstchoice_cs.adyen.repositories.paymentMethods.PaymentsRepository

class FCDropInService : SimplifiedDropInService() {

    companion object {
        private val TAG = LogUtil.getTag()
        private val CONTENT_TYPE: MediaType = "application/json".toMediaType()
    }
    private val paymentsRepository: PaymentsRepository by inject()
    private val keyValueStorage: KeyValueStorage by inject()

    override fun makePaymentsCallOrFail(paymentComponentData: JSONObject): JSONObject? {
        Logger.d(TAG, "makePaymentsCallOrFail")
        Logger.v(TAG, "paymentComponentData - ${JsonUtils.indent(paymentComponentData)}")


        val shopperRef =   keyValueStorage.getShopperReference()
        val amount = keyValueStorage.getAmount()
        val country =  keyValueStorage.getCountry()
        val merchant = keyValueStorage.getMerchantAccount()
        // Check out the documentation of this method on the parent DropInService class
        val paymentRequest = createPaymentRequest(
            paymentComponentData,
            shopperRef,
            amount,
            country,
            merchant,
            RedirectComponent.getReturnUrl(applicationContext),
            AdditionalData(
                allow3DS2 = keyValueStorage.isThreeds2Enable().toString(),
                executeThreeD = keyValueStorage.isExecuteThreeD().toString()
            )
        )

        val requestBody = paymentRequest.toString().toRequestBody(CONTENT_TYPE)
        val call = paymentsRepository.paymentsRequest(requestBody)

        return makeCall(call)
    }

    override fun makeDetailsCallOrFail(actionComponentData: JSONObject): JSONObject? {
        Logger.d(TAG, "makeDetailsCallOrFail")
        Logger.v(TAG, "actionComponentData - ${JsonUtils.indent(actionComponentData)}")

        val requestBody = actionComponentData.toString().toRequestBody(CONTENT_TYPE)
        val call = paymentsRepository.detailsRequest(requestBody)

        return makeCall(call)
    }

    private fun makeCall(call: Call<ResponseBody>): JSONObject? {
        val response = call.execute()

        val byteArray = response.errorBody()?.bytes()
        if (byteArray != null) {
            Logger.e(TAG, "errorBody - ${String(byteArray)}")
        }

        if (response.isSuccessful) {
            val res = response.body()?.string()
            App.instance.paymentResult = Gson().fromJson(res, ResultData::class.java)
            return JSONObject(res)
        }

        return null
    }
}
