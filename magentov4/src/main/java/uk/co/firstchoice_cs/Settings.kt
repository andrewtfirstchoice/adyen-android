package uk.co.firstchoice_cs

import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.magentoAPI.MagentoAPI
import java.util.*


object Settings {
    const val GUEST_LOGIN_FAIL_TEST = false
    const val ACCOUNT_LOGIN_FAIL_TEST = false
    const val TESTING = true
    private const val B2B_TESTING = true

    private  const val V4_API_TEST_ACCOUNT = false
    private  const val MAGENTO_API_TEST_ACCOUNT = true
    private  const val CUSTOMER_API_TEST_ACCOUNT = false
    private  const val ADYEN_API_TEST_ACCOUNT = false

    const val UNSAFE_SERVICE = "unsafeService"
    const val DEFAULT_SERVICE = "defaultService"

    const val MAX_RECURSION_S_CEDED = 5
    const val V4_BEARER = "FCCAPP_MUfOwgtH4oEnHOfGxAu+eBht92AwtwxaBDqt6rfLhouB+MSVcYsOds5pmqCufAyq"

    private const val V4_URL_LIVE = "https://api.firstchoice-cs.co.uk/V4/"
    private const val V4_URL_TEST= "https://10.0.10.200/v4Dev/"

    private const val CUSTOMER_API_TEST = "https://api.firstchoice-cs.co.uk/cadev2/"
    private const val CUSTOMER_API_LIVE = "https://api.firstchoice-cs.co.uk/ig11/"

    private const val MAGENTO_BASE_LIVE = "https://firstchoice-cs.co.uk/rest/V1/"
    private const val MAGENTO_BASE_TEST = "https://porkins.firstchoice-cs.co.uk/rest/V1/"

    private const val ADYEN_BASE_LIVE = "https://firstchoice-cs.co.uk/rest/V1/"
    private const val ADYEN_BASE_TEST = "https://checkout-test.adyen.com/v53/"

    private const val CUSTOMER_USERNAME_TEST = "FirstChoiceApp"
    private const val CUSTOMER_PASSWORD_TEST = "\$2nnwMKXqCv6*2"

    private const val CUSTOMER_USERNAME_LIVE = "FirstChoiceApp"
    private const val CUSTOMER_PASSWORD_LIVE = "0Hh7f5gGVkTek3clJx2xuvFyc1WqIeKI5LzbRRNTuKWF1!?"

    private  const val B2B_USER_NAME_LIVE = "testaccount08@firstchoice-cs.co.uk"
    private  const val B2B_PASSWORD_LIVE = "!Donk3ySw0rd!?qas"
    private const val B2C_USER_NAME_LIVE = "testaccount10@firstchoice-cs.co.uk"
    private const val B2C_PASSWORD_LIVE = "MinkyMankyMoo1234!?"

    private const val B2B_USER_NAME_TEST = "grattebr@firstchoice-cs.co.uk"
    private const val B2B_PASSWORD_TEST = "Password1313!?"
    private const val B2C_USER_NAME_TEST = "testaccount02@firstchoice-cs.co.uk"
    private const val B2C_PASSWORD_TEST = "ChocolateMouse130220!?"

    private const val MAGENTO_ADMIN_USERNAME_LIVE = "FCDevelopment"
    private const val MAGENTO_ADMIN_PASSWORD_LIVE = "Password1313!?"
    private const val MAGENTO_ADMIN_USERNAME_TEST = "FCDevelopment"
    private const val MAGENTO_ADMIN_PASSWORD_TEST = "Password1313!?"

    @JvmStatic
    fun v4Url():String
    {
        return if(V4_API_TEST_ACCOUNT) {
            V4_URL_TEST
        } else {
            V4_URL_LIVE
        }
    }


    @JvmStatic
    fun magentoAdminUsername():String
    {
        return if(MAGENTO_API_TEST_ACCOUNT) {
            MAGENTO_ADMIN_USERNAME_TEST
        } else {
            MAGENTO_ADMIN_USERNAME_LIVE
        }
    }

    @JvmStatic
    fun magentoAdminPassword():String
    {
        return if(MAGENTO_API_TEST_ACCOUNT) {
            MAGENTO_ADMIN_PASSWORD_TEST
        } else {
            MAGENTO_ADMIN_PASSWORD_LIVE
        }
    }

    @JvmStatic
    fun magentoCustomerUsername():String
    {
        return if(MAGENTO_API_TEST_ACCOUNT) {
            if(B2B_TESTING)
                B2B_USER_NAME_TEST
            else
                B2C_USER_NAME_TEST
        } else {
            if(B2B_TESTING)
                B2B_USER_NAME_LIVE
            else
                B2C_USER_NAME_LIVE
        }
    }

    @JvmStatic
    fun magentoCustomerPassword():String
    {
        return if(MAGENTO_API_TEST_ACCOUNT) {
            if(B2B_TESTING)
                B2B_PASSWORD_TEST
            else
                B2C_PASSWORD_TEST
        } else {
            if(B2B_TESTING)
                B2B_PASSWORD_LIVE
            else
                B2C_PASSWORD_LIVE
        }
    }


    @JvmStatic
    fun customerBaseUrl():String
    {
        return if(CUSTOMER_API_TEST_ACCOUNT) {
            CUSTOMER_API_TEST
        } else {
            CUSTOMER_API_LIVE
        }
    }

    @JvmStatic
    fun customerUsername():String
    {
        return if(CUSTOMER_API_TEST_ACCOUNT) {
            CUSTOMER_USERNAME_TEST
        } else {
            CUSTOMER_USERNAME_LIVE
        }
    }

    @JvmStatic
    fun customerPassword():String
    {
        return if(CUSTOMER_API_TEST_ACCOUNT) {
            CUSTOMER_PASSWORD_TEST
        } else {
            CUSTOMER_PASSWORD_LIVE
        }
    }

    @JvmStatic
    fun magentoBaseUrl():String
    {
        return if(MAGENTO_API_TEST_ACCOUNT) {
            MAGENTO_BASE_TEST
        } else {
            MAGENTO_BASE_LIVE
        }
    }

    @JvmStatic
    fun adyenBaseUrl():String
    {
        return if(ADYEN_API_TEST_ACCOUNT) {
            ADYEN_BASE_TEST
        } else {
            ADYEN_BASE_LIVE
        }
    }

    const val LOGGED_IN_PREF = "logged_in_pref"
    const val LOGGED_IN_TOKEN = "logged_in_token"
    const val USER_FULL_NAME = "user_full_name"
    const val USER_INITIAL = "user_initial"
    const val USER_JSON = "user_json"
    const val USER_EMAIL = "user_email"
    const val PHONE_NUMBER = "01543577778"
    const val MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT = 1000
    const val MY_PERMISSIONS_REQUEST_READ_WRITE = 0xd2d2
    const val CONNECTIVITY_REQUEST_CODE = 69
    const val SPEECH_REQUEST_CODE = 1001
    const val STORAGE_PERMISSIONS = 1200
    const val LATITUDE = 52.671246
    const val LONGITUDE = -2.002997
    const val MOBILE_ONLY_CONNECTION_TEST = false
    const val NEW_UPDATE_AVAILABLE_TEST = false
    const val ABOUT_BLANK_URL = "about:blank"
    const val LOGGED_IN_TIME = "logged_in_time"
    const val ADMIN_TOKEN = "admin_token"
    const val ADMIN_TIME = "admin_time"
    const val FC_USER_AGENT = "Wrapper App Android"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val CUSTOMER_DETAILS = "customer_details"
    const val IS_UPGRADED = "isUpgraded"
    const val ON_BOARDING_COMPLETE = "onboarding"
    const val CUSTOMER_API_TOKEN = "customer_api_token"
    const val CUSTOMER_API_TOKEN_TIME = "customer_api_token_time"
    const val CUSTOMER_API_TOKEN_EXPIRY = "customer_api_token_expiry"
    const val CUSTOMER_EXPIRY_LIMIT = "customer_expiry_limit"
    const val ADMIN_EXPIRY_LIMIT = "admin_expiry_limit"
    const val CUSTOMER_AUTH_EXPIRY_LIMIT = "customer_auth_expiry_limit"
    const val PRICE_EXPIRY_LIMIT = "price_expiry_limit"

    //Expiry times
    var customerExpiryMS = 10000L
    var adminExpiryMS = 10000L
    var customerAuthTokenExpiryMS = 10000L //24 hours
    var priceExpiryMS = 900000L//15 Minutes
    var brandExpiryMS = 10000L //24 hours
    var partClassesExpiryMS = 10000L //24 hours
    var topLevelExpiryMS = 10000L //24 hours

    enum class PriceStatus {
        NONE, GETTING_PRICES, FAILED_GETTING_PRICES, SUCCESS_GETTING_PRICES
    }
    enum class LoginState {
        NONE, LOGGING_IN_ACCOUNT,LOGGING_IN_GUEST,  LOGGED_IN_ACCOUNT, LOGGED_IN_AS_GUEST, DEFAULT_LOGIN
    }
    enum class CustomerOverride {
        IrelandNoVAT, //PRECIS01
        UnitedKingdom, //GRATTEBR
        UnitedStates, //PARTS001
        Jersey, //JERSEYDF
    }
    @JvmStatic
    fun v4Bearer():String
    {
        val token = V4_BEARER
        return "Bearer $token"
    }
    @JvmStatic
    fun magentoBearerAdmin():String
    {
        val token = getMagentoAdminToken()
        return "Bearer $token"
    }
    @JvmStatic
    fun magentoBearerCustomer():String
    {
        val token =  getMagentoCustomerToken()
        return "Bearer $token"
    }
    @JvmStatic
    fun bearerCustomerAuth():String
    {
        val token =  getCustomerAPIAuth()
        return "Bearer $token"
    }



    private fun getMagentoAdminToken(): String {
        val aToken = App.magentoAdminToken
        return if (aToken.first.isEmpty() || aToken.second == 0L || App.instance.adminExpired()) {
            App.magentoAdminToken = Pair("",0)
            val aTokenResult = MagentoAPI.getAdminToken()
            if (aTokenResult.isNotEmpty()) {
                App.instance.setAdminToken(aTokenResult, Date(System.currentTimeMillis()).time)
                App.hasAdminToken = true
                App.magentoAdminToken.first
            } else {
                App.hasAdminToken = false
                App.magentoAdminToken.first
            }
        }
        else {
            App.hasAdminToken = true
            App.magentoAdminToken.first
        }
    }

    private fun getMagentoCustomerToken(): String {
        val cToken = App.magentoCustomerToken
        return if (cToken.first.isEmpty() || cToken.second == 0L || App.instance.customerExpired()) {
            App.magentoCustomerToken = Pair("",0)
            val cTokenResult = MagentoAPI.getCustomerToken()
            if (cTokenResult.isNotEmpty()) {
                App.instance.setCustomerToken(cTokenResult, Date(System.currentTimeMillis()).time)
                App.hasCustomerToken = true
                App.magentoCustomerToken.first
            } else {
                App.hasCustomerToken = false
                App.magentoCustomerToken.first
            }
        }
        else {
            App.hasCustomerToken = true
            App.magentoCustomerToken.first
        }
    }

    private fun getCustomerAPIAuth():String {
        val caToken = App.customerAPIToken
        return if (caToken.first.isEmpty() || caToken.second == 0L || App.instance.customerAPIAuthExpired()) {
            App.customerAPIToken = Pair("",0) //if complete fail then the string returned empty
            val customerAuthResult = CustomerAPICalls.auth()
            if (customerAuthResult != null && !customerAuthResult.result.isNullOrBlank()) {
                App.instance.setCustomerAPIToken(customerAuthResult.result, Date(System.currentTimeMillis()).time)
                App.hasCustomerAPIAuthToken = true
                App.customerAPIToken.first //success
            }
            else {
                App.hasCustomerAPIAuthToken = false
                App.customerAPIToken.first //fail
            }
        } else {
            App.hasCustomerAPIAuthToken = true //use local
            App.customerAPIToken.first
        }
    }
}
