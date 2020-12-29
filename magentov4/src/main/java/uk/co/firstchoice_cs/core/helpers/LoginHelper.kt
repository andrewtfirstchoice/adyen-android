package uk.co.firstchoice_cs.core.helpers

import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.App.Companion.addresses
import uk.co.firstchoice_cs.App.Companion.customer
import uk.co.firstchoice_cs.App.Companion.hasAdminToken
import uk.co.firstchoice_cs.App.Companion.hasCustomer
import uk.co.firstchoice_cs.App.Companion.hasCustomerAPIAuthToken
import uk.co.firstchoice_cs.App.Companion.hasCustomerAddresses
import uk.co.firstchoice_cs.App.Companion.hasCustomerToken
import uk.co.firstchoice_cs.App.Companion.hasMagentoCustomerDetails
import uk.co.firstchoice_cs.App.Companion.initialLoginCompleted
import uk.co.firstchoice_cs.App.Companion.internetErrorDuringLogin
import uk.co.firstchoice_cs.App.Companion.magentoCustomer
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.SavePrefs
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.magentoAPI.MagentoAPI

object LoginHelper : KoinComponent {

    fun networkConditionsOKForLogin(): Boolean {
        return if (AppStatus.INTERNET_CONNECTED && !internetErrorDuringLogin) {
            true
        } else {
            internetErrorDuringLogin = true
            false
        }
    }

    fun clearLoginState() {

        SavePrefs.setLoggedIn(Settings.LoginState.NONE)

        magentoCustomer = null
        addresses.clear()
        internetErrorDuringLogin = false


        hasCustomerAPIAuthToken = false
        hasCustomerToken = false
        hasCustomerAddresses = false

        hasAdminToken = false
        hasCustomerToken
        hasMagentoCustomerDetails = false
        hasCustomer = false
        hasCustomerAddresses = false

        initialLoginCompleted = false
    }


    fun isLoggedIn(): Boolean {
        return if (App.loginState.value==Settings.LoginState.LOGGING_IN_GUEST) {
            hasCustomerAPIAuthToken && hasCustomer
        } else {
            hasAdminToken && hasCustomerToken && hasMagentoCustomerDetails && hasCustomerAddresses && hasCustomerAPIAuthToken&& hasCustomer
        }
    }

    fun logStatus() {
        LoggingHelper.debugMsg("LoginProcess", "Has Admin Token = $hasAdminToken")
        LoggingHelper.debugMsg("LoginProcess", "Has Customer Token Auth = $hasCustomerAPIAuthToken")
        LoggingHelper.debugMsg("LoginProcess", "Has Customer Token = $hasCustomerToken")
        LoggingHelper.debugMsg("LoginProcess", "Has Magento Customer Details = $hasMagentoCustomerDetails")
        LoggingHelper.debugMsg("LoginProcess", "Has Customer API Customer = $hasCustomer")
        LoggingHelper.debugMsg("LoginProcess", "Has Customer API Addresses = $hasCustomerAddresses")
    }

    @JvmStatic
    fun isSignedInAndNotExpired():Boolean
    {
        if(App.instance.adminExpired())
            return false
        if(App.instance.customerExpired())
            return false
        if(App.magentoAdminToken.first.isEmpty())
            return false
        if(App.magentoCustomerToken.first.isEmpty())
            return false
        return true
    }


    fun getCustomer(id:Int) {
        val customerResult = CustomerAPICalls.getCustomer(id)
        if (!customerResult?.customer.isNullOrEmpty()) {
            customer = customerResult?.customer?.get(0)
            if(customer!=null) {
                hasCustomer = true
            }
        }
    }

    fun getCustomerAddresses(id:Int) {
        val customerAddresses = CustomerAPICalls.getAddresses(id)
        if (!customerAddresses?.addresses.isNullOrEmpty()) {
            val add = customerAddresses?.addresses
            if(add!=null) {
                addresses.addCustomerAddresses(add)
                hasCustomerAddresses = true
            }
        }
    }


    fun getMagentoCustomerDetails() {
        val customerDetailsResult = MagentoAPI.getCustomerDetails()
        if (customerDetailsResult != null) {
            magentoCustomer = customerDetailsResult
            hasMagentoCustomerDetails = true
        }
    }
}


