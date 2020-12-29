/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package uk.co.firstchoice_cs.adyen.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.adyen.checkout.components.model.payments.Amount
import uk.co.firstchoice_cs.adyen.extensions.get
import uk.co.firstchoice_cs.firstchoice.R
import kotlin.math.roundToInt

interface KeyValueStorage {
    fun getShopperReference(): String
    fun getAmount(): Amount
    fun getCountry(): String
    fun getShopperLocale(): String
    fun isThreeds2Enable(): Boolean
    fun isExecuteThreeD(): Boolean
    fun getShopperEmail(): String
    fun getMerchantAccount(): String
    fun setMerchantAccount(merchantAccount: String)
}

class KeyValueStorageImpl(
    private val appContext: Context,
    private val sharedPreferences: SharedPreferences
) : KeyValueStorage {

    companion object {
        var DEFAULT_COUNTRY = "GB"
        var DEFAULT_LOCALE = "en_UK"
        var DEFAULT_VALUE = "0"
        var DEFAULT_CURRENCY = "GBP"
        var DEFAULT_THREEDS2_ENABLE = true
        var DEFAULT_EXECUTE_3D = false
        var DEFAULT_MERCHANT = "FirstChoiceCateringSparesECOM"
        var DEFAULT_EMAIL = ""
    }

    override fun getShopperReference(): String {
        return sharedPreferences.get(
            appContext,
            R.string.shopper_reference_key,
            "DEFAULTSHOPPERREF"
        )
    }

    override fun getAmount(): Amount {

        val amountValue = sharedPreferences.get(appContext, R.string.value_key, DEFAULT_VALUE)
        val amountCurrency = sharedPreferences.get(
            appContext,
            R.string.currency_key,
            DEFAULT_CURRENCY
        )

        val amount = Amount()
        amount.currency = amountCurrency
        amount.value = amountValue.toFloat().roundToInt()

        return amount
    }

    override fun getCountry(): String {
        return sharedPreferences.get(appContext, R.string.shopper_country_key, DEFAULT_COUNTRY)
    }

    override fun getShopperLocale(): String {
        return sharedPreferences.get(appContext, R.string.shopper_locale_key, DEFAULT_LOCALE)
    }

    override fun isThreeds2Enable(): Boolean {
        return sharedPreferences.get(appContext, R.string.threeds2_key, DEFAULT_THREEDS2_ENABLE)
    }

    override fun isExecuteThreeD(): Boolean {
        return sharedPreferences.get(appContext, R.string.execute3D_key, DEFAULT_EXECUTE_3D)
    }

    override fun getShopperEmail(): String {
        return sharedPreferences.get(appContext, R.string.shopper_email_key, DEFAULT_EMAIL)
    }

    override fun getMerchantAccount(): String {
        return sharedPreferences.get(
            appContext,
            R.string.merchant_account_key,
            DEFAULT_MERCHANT
        )
    }

    override fun setMerchantAccount(merchantAccount: String) {
        val editor = sharedPreferences.edit()
        editor.putString(R.string.merchant_account_key.toString(), merchantAccount)
        editor.commit()
    }
}
