/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package uk.co.firstchoice_cs.adyen

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.core.log.LogUtil
import com.adyen.checkout.core.log.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import uk.co.firstchoice_cs.adyen.data.api.ResultData
import uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest.PaymentMethodsRequest
import uk.co.firstchoice_cs.adyen.data.storage.KeyValueStorage
import uk.co.firstchoice_cs.adyen.repositories.paymentMethods.PaymentsRepository
import uk.co.firstchoice_cs.store.vm.MainActivityViewModel
import kotlin.coroutines.CoroutineContext

class PaymentMethodsViewModel(
    private val paymentsRepository: PaymentsRepository,
    private val keyValueStorage: KeyValueStorage
) : ViewModel() {

    companion object {
        private val TAG = LogUtil.getTag()
    }

    private val parentJob = Job()

    private val coroutineContext: CoroutineContext get() = parentJob + Dispatchers.Main

    private val scope = CoroutineScope(coroutineContext)

    val paymentMethodResponseLiveData = MutableLiveData<PaymentMethodsApiResponse>()

    fun requestPaymentMethods() {
        scope.launch(context = Dispatchers.IO) {
            val res = paymentsRepository.getPaymentMethods(getPaymentMethodRequest())
            paymentMethodResponseLiveData.postValue(res!!)
        }
    }

    private fun getPaymentMethodRequest(): PaymentMethodsRequest {
        return PaymentMethodsRequest(
            keyValueStorage.getMerchantAccount(),
            keyValueStorage.getShopperReference(),
            keyValueStorage.getAmount(),
            keyValueStorage.getCountry(),
            keyValueStorage.getShopperLocale()
        )
    }

    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "onCleared")
        coroutineContext.cancel()
    }
}
