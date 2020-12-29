/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 13/12/2019.
 */

package uk.co.firstchoice_cs.adyen.service

import com.adyen.checkout.components.model.payments.Amount
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.AdditionalData
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.Item

@Suppress("LongParameterList")
fun createPaymentRequest(
        paymentComponentData: JSONObject,
        shopperReference: String,
        amount: Amount,
        countryCode: String,
        merchantAccount: String,
        redirectUrl: String,
        additionalData: AdditionalData,
        force3DS2Challenge: Boolean = false
): JSONObject {

    val request = JSONObject(paymentComponentData.toString())

    request.put("shopperReference", shopperReference)
    request.put("amount", JSONObject(Gson().toJson(amount)))
    request.put("merchantAccount", merchantAccount)
    request.put("returnUrl", redirectUrl)
    request.put("countryCode", countryCode)
    request.put("shopperIP", "0.0.0.0")
    request.put("reference", "android-test-components_${System.currentTimeMillis()}")
    request.put("channel", "android")
    request.put("additionalData", JSONObject(Gson().toJson(additionalData)))
    request.put("lineItems", JSONArray(Gson().toJson(listOf(Item()))))

    if (force3DS2Challenge) {
        val threeDS2RequestData = JSONObject()
        threeDS2RequestData.put("deviceChannel", "app")
        threeDS2RequestData.put("challengeIndicator", "requestChallenge")
        request.put("threeDS2RequestData", threeDS2RequestData)
    }

    return request
}
