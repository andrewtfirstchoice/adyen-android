package uk.co.firstchoice_cs

import android.content.Context
import com.google.gson.Gson
import de.adorsys.android.securestoragelibrary.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.co.firstchoice_cs.Constants.PAYMENT_TYPE_CARD
import uk.co.firstchoice_cs.Settings.ADMIN_EXPIRY_LIMIT
import uk.co.firstchoice_cs.Settings.ADMIN_TIME
import uk.co.firstchoice_cs.Settings.ADMIN_TOKEN
import uk.co.firstchoice_cs.Settings.CUSTOMER_API_TOKEN
import uk.co.firstchoice_cs.Settings.CUSTOMER_API_TOKEN_TIME
import uk.co.firstchoice_cs.Settings.CUSTOMER_AUTH_EXPIRY_LIMIT
import uk.co.firstchoice_cs.Settings.CUSTOMER_DETAILS
import uk.co.firstchoice_cs.Settings.CUSTOMER_EXPIRY_LIMIT
import uk.co.firstchoice_cs.Settings.IS_UPGRADED
import uk.co.firstchoice_cs.Settings.LOGGED_IN_PREF
import uk.co.firstchoice_cs.Settings.LOGGED_IN_TIME
import uk.co.firstchoice_cs.Settings.LOGGED_IN_TOKEN
import uk.co.firstchoice_cs.Settings.ON_BOARDING_COMPLETE
import uk.co.firstchoice_cs.Settings.PASSWORD
import uk.co.firstchoice_cs.Settings.PRICE_EXPIRY_LIMIT
import uk.co.firstchoice_cs.Settings.USERNAME
import uk.co.firstchoice_cs.Settings.USER_EMAIL
import uk.co.firstchoice_cs.Settings.USER_FULL_NAME
import uk.co.firstchoice_cs.Settings.USER_INITIAL
import uk.co.firstchoice_cs.core.api.magentoAPI.Customer
import uk.co.firstchoice_cs.core.helpers.CartHelper.customerInitials
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import java.util.*


object SavePrefs {

    fun saveCustomerExpiryLimit(millis:Long) {
        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putLong(CUSTOMER_EXPIRY_LIMIT , millis)
        editor.apply()
    }

    fun saveAdminExpiryLimit(millis:Long) {
        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putLong(ADMIN_EXPIRY_LIMIT , millis)
        editor.apply()
    }

    fun saveCustomerAuthExpiryLimit(millis:Long) {
        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putLong(CUSTOMER_AUTH_EXPIRY_LIMIT , millis)
        editor.apply()
    }

    fun savePriceExpiryLimit(millis:Long) {
        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putLong(PRICE_EXPIRY_LIMIT , millis)
        editor.apply()
    }

    val customerExpiryLimit: Long
        get() = App.instance.getSharedPreferences("Settings", 0).getLong(CUSTOMER_EXPIRY_LIMIT, 3600000L)

    val adminExpiryLimit: Long
        get() = App.instance.getSharedPreferences("Settings", 0).getLong(ADMIN_EXPIRY_LIMIT, 14400000L)

    val customerAuthExpiryLimit: Long
        get() = App.instance.getSharedPreferences("Settings", 0).getLong(CUSTOMER_AUTH_EXPIRY_LIMIT, 86400000L)

    val priceExpiryLimit: Long
        get() = App.instance.getSharedPreferences("Settings", 0).getLong(PRICE_EXPIRY_LIMIT, 14400000L)


    val customerAPIToken: String
        get() = App.instance.getSharedPreferences("Settings", 0).getString(CUSTOMER_API_TOKEN, "")?:""

    val customerAPITokenTime: Long
        get() = App.instance.getSharedPreferences("Settings", 0).getLong(Settings.CUSTOMER_API_TOKEN_EXPIRY, 0L)

    val adminToken: String
        get() = SecurePreferences.getStringValue(App.instance, ADMIN_TOKEN, "").toString()

    val customerToken: String
        get() = SecurePreferences.getStringValue(App.instance, LOGGED_IN_TOKEN, "").toString()

    val adminTime: Long
        get() = SecurePreferences.getLongValue(App.instance, ADMIN_TIME, 0)

    val customerTime: Long
        get() = SecurePreferences.getLongValue(App.instance, LOGGED_IN_TIME, 0)

    val password: String?
        get() = SecurePreferences.getStringValue(App.instance, PASSWORD, "")

    val username: String?
        get() = SecurePreferences.getStringValue(App.instance, USERNAME, "")

    val loggedStatus: Settings.LoginState
        get() =  Settings.LoginState.valueOf(SecurePreferences.getStringValue(App.instance,LOGGED_IN_PREF,
            Settings.LoginState.NONE.name)?:Settings.LoginState.NONE.name)

    val userEmail: String?
        get() = SecurePreferences.getStringValue(App.instance, USER_EMAIL, "")


    val userFullName: String?
        get() = SecurePreferences.getStringValue(App.instance, USER_FULL_NAME, "")

    val userInitials: String?
        get() = SecurePreferences.getStringValue(App.instance, USER_INITIAL, "")

    val onBoarding: Boolean
        get() = SecurePreferences.getBooleanValue(App.instance, ON_BOARDING_COMPLETE, false)

    val isUpgraded: Boolean
        get() = SecurePreferences.getBooleanValue(App.instance, IS_UPGRADED, false)

    fun saveCustomerAPIToken(token: String,millis:Long) {
        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putString(CUSTOMER_API_TOKEN , token)
        editor.putLong(CUSTOMER_API_TOKEN_TIME , millis)
        editor.apply()
    }


    fun saveAdminToken(token: String,millis:Long) {
        SecurePreferences.setValue(App.instance, ADMIN_TOKEN, token)
        SecurePreferences.setValue(App.instance, ADMIN_TIME, millis)
    }

    fun saveCustomerToken(token: String,millis:Long) {
        SecurePreferences.setValue(App.instance, LOGGED_IN_TOKEN, token)
        SecurePreferences.setValue(App.instance, LOGGED_IN_TIME, millis)
    }

    fun savePassword(pwd: String) {
        SecurePreferences.setValue(App.instance, PASSWORD, pwd)
    }

    fun saveUsername(uName: String) {
        SecurePreferences.setValue(App.instance, USERNAME, uName)
    }

    fun setIsUpgraded(upgraded: Boolean) {
        SecurePreferences.setValue(App.instance, IS_UPGRADED, upgraded)
    }

    fun setLoggedIn(loginState: Settings.LoginState) {
        SecurePreferences.setValue(App.instance, LOGGED_IN_PREF, loginState.name )

        GlobalScope.launch(context = Dispatchers.Main) {
            App.loginState.value = loginState
        }
    }

    fun clearUserAndLogout() {

        val prefs = App.instance.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.remove(CUSTOMER_API_TOKEN)
        editor.remove(CUSTOMER_API_TOKEN_TIME)
        editor.apply()
        // clear some of secure prefs
        SecurePreferences.removeValue(App.instance, ADMIN_TOKEN)
        SecurePreferences.removeValue(App.instance, ADMIN_TIME)
        SecurePreferences.removeValue(App.instance, USER_FULL_NAME)
        SecurePreferences.removeValue(App.instance, USER_INITIAL)
        SecurePreferences.removeValue(App.instance, CUSTOMER_DETAILS)
        SecurePreferences.removeValue(App.instance, USERNAME)
        SecurePreferences.removeValue(App.instance, PASSWORD)
        SecurePreferences.removeValue(App.instance, LOGGED_IN_TOKEN)
        SecurePreferences.removeValue(App.instance, LOGGED_IN_TIME)

        App.instance.clearTokens()
        setLoggedIn(Settings.LoginState.NONE)
        App.selectedPaymentType = PAYMENT_TYPE_CARD
    }


    fun onBoardingComplete() {
        SecurePreferences.setValue(App.instance, ON_BOARDING_COMPLETE, true)
    }


    @JvmStatic
    fun saveCustomerJSON(json: String, mContext: Context) {
        val prefs = mContext.getSharedPreferences("Settings", 0)
        val editor = prefs.edit()
        editor.putString(Settings.USER_JSON , json)
        editor.apply()
    }

    @JvmStatic
    fun setArrayPrefs(arrayName: String, array: ArrayList<String>, mContext: Context) {
        val prefs = mContext.getSharedPreferences(arrayName, 0)
        val editor = prefs.edit()
        editor.putInt(arrayName + "_size", array.size)
        for (i in array.indices) editor.putString(arrayName + "_" + i, array[i])
        editor.apply()
    }

    @JvmStatic
    fun getArrayPrefs(arrayName: String, mContext: Context?): ArrayList<String> {
        val prefs = mContext?.getSharedPreferences(arrayName, 0)
        val size = prefs?.getInt(arrayName + "_size", 0)
        if (size != null) {
            val array = ArrayList<String>(size)
            for (i in 0 until size) prefs.getString(arrayName + "_" + i, null)?.let { array.add(it) }
            return array
        }
        return ArrayList(0)
    }

    @JvmStatic
    fun saveUserDetails(json: String?): Customer {
        val customerDetails = Gson().fromJson(json, Customer::class.java)
        try {
            val email = customerDetails.email
            val firstName = customerDetails.firstname
            val lastName = customerDetails.lastname
            val initial = customerInitials(customerDetails)
            SecurePreferences.setValue(App.instance, USER_EMAIL, email)
            SecurePreferences.setValue(App.instance, USER_FULL_NAME, "$firstName $lastName")
            SecurePreferences.setValue(App.instance, USER_INITIAL, initial)
            if (json != null) {
                saveCustomerJSON(json, App.instance.applicationContext)
            }
            return customerDetails
        }
        catch (ex:Exception)
        {
            LoggingHelper.errorMsg("saveUserDetails",ex.toString())
            return customerDetails
        }
    }
}


