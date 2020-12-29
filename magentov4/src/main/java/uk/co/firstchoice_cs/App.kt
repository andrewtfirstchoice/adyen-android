package uk.co.firstchoice_cs

import android.app.Application
import android.os.Build
import android.os.Environment
import androidx.lifecycle.MutableLiveData
import com.helpcrunch.library.core.HelpCrunch
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.KoinComponent
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import uk.co.firstchoice_cs.adyen.data.api.ResultData
import uk.co.firstchoice_cs.adyen.di.repositoryModule
import uk.co.firstchoice_cs.adyen.di.storageManager
import uk.co.firstchoice_cs.adyen.di.viewModelModule
import uk.co.firstchoice_cs.basket.MagentoOrder
import uk.co.firstchoice_cs.core.api.AddressManager
import uk.co.firstchoice_cs.core.api.magentoAPI.Customer
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.modules.*
import java.util.*

class App : Application(),KoinComponent {

    var paymentCallResult: String? = null
    var paymentResult: ResultData? = null
    var awaitingPaymentResult = false
    var magentoOrder: MagentoOrder? = null
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    override fun onCreate() {
        super.onCreate()
        instance = this
        globalData = GlobalAppData()

       // SavePrefs.clearUserAndLogout()

        LoggingHelper.debugMsg("BuildVersion", "version = " + Build.VERSION.SDK_INT)
        //todo this needs understanding and dealing with
        docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/FirstChoice"
        thumbnailsDirectory = "$docDir/thumbnails/"

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(listOf(appModule, storageManager, repositoryModule, viewModelModule))
           // modules(listOf(appModule))
        }

        registerActivityLifecycleCallbacks(defaultCurrentActivityListener)

        initTokensFromPreferences()

        httpClient = OkHttpClient().newBuilder().build()

        globalData = GlobalAppData()

        setupHelpCrunch()
    }

    private fun setupHelpCrunch() {
        try {
            HelpCrunch.initialize(ORGANIZATION, APP_ID, SECRET)
        } catch (ex: Exception) {
            LoggingHelper.errorMsg("setupHelpCrunch",ex.toString())
        }
    }

    private fun initTokensFromPreferences()
    {
        val adminToken = SavePrefs.adminToken
        val customerToken = SavePrefs.customerToken
        val customerApiToken = SavePrefs.customerAPIToken
        val adminTime = SavePrefs.adminTime
        val customerTime = SavePrefs.customerTime
        val customerAPITokenTime = SavePrefs.customerAPITokenTime

        //initialise the expiry times from prefs - if first load prefs return defaults
        Settings.customerAuthTokenExpiryMS = SavePrefs.customerAuthExpiryLimit
        Settings.customerExpiryMS = SavePrefs.customerExpiryLimit
        Settings.adminExpiryMS = SavePrefs.adminExpiryLimit
        Settings.priceExpiryMS = SavePrefs.priceExpiryLimit
        //initialise the tokens from the ones saved in preferences - no need to save as they have just been loaded
        setAdminToken(adminToken,adminTime,save = false)
        setCustomerToken(customerToken,customerTime,save = false)
        setCustomerAPIToken(customerApiToken,customerAPITokenTime,save = false)
    }

    fun setAdminToken(token:String, time:Long, save:Boolean = true)
    {
        magentoAdminToken = Pair(token,time)
        if(save)
            SavePrefs.saveAdminToken(token,time)
    }

    fun setCustomerToken(token:String, time:Long, save:Boolean = true)
    {
        magentoCustomerToken = Pair(token,time)
        if(save)
            SavePrefs.saveCustomerToken(token,time)
    }

    fun setCustomerAPIToken(token:String, time:Long, save:Boolean = true)
    {
        customerAPIToken = Pair(token,time)
        if(save)
            SavePrefs.saveCustomerAPIToken(token,time)
    }

    fun customerAPIAuthExpired(): Boolean {
        val now = Date(System.currentTimeMillis()).time
        return now - customerAPIToken.second > Settings.customerAuthTokenExpiryMS
    }

    fun adminExpired(): Boolean {
        val now = Date(System.currentTimeMillis()).time
        return now - magentoAdminToken.second > Settings.adminExpiryMS
    }

    fun customerExpired(): Boolean {
        val now = Date(System.currentTimeMillis()).time
        return now - magentoCustomerToken.second > Settings.customerExpiryMS
    }


    fun clearTokens()
    {
        instance.setCustomerToken("",0L)
        instance.setAdminToken("",0L)
        instance.setCustomerAPIToken("",0L)
        magentoAdminToken = Pair("",0)
        magentoCustomerToken = Pair("",0)
        customerAPIToken = Pair("",0)
    }



    companion object {

        //var isGuest: Boolean = false
      //  var loggingIn = false
        var initialLoginCompleted = false
        var internetErrorDuringLogin = false
        var hasCustomerToken = false
        var hasAdminToken = false
        var hasMagentoCustomerDetails = false
        var hasCustomer = false
        var hasCustomerAddresses = false
        var hasCustomerAPIAuthToken = false
        var selectedPaymentType = ""
        var magentoCustomer: Customer?=null
        var customer: uk.co.firstchoice_cs.core.api.customerAPI.CustomerX?=null
        var magentoCustomerToken = Pair("", 0L)
        var magentoAdminToken = Pair("", 0L)
        var customerAPIToken = Pair("", 0L)
        var addresses = AddressManager()

        val internetConnected: MutableLiveData<Boolean> by lazy {
            MutableLiveData<Boolean>()
        }

        val loginState: MutableLiveData<Settings.LoginState> by lazy {
            MutableLiveData<Settings.LoginState>()
        }

        val chatChanged: MutableLiveData<Int> by lazy {
            MutableLiveData<Int>()
        }

        val manualsCount: MutableLiveData<Int> by lazy {
            MutableLiveData<Int>()
        }

        lateinit var instance: App
        lateinit var docDir: String
        lateinit var thumbnailsDirectory: String
        lateinit var httpClient: OkHttpClient
        lateinit var globalData: GlobalAppData
        var shopperLocale:Locale = Locale.ENGLISH
        private const val APP_ID = 9
        private const val SECRET =  "87f1VCplTbx6TZqtc1NIllL6azLqE4B3zug/EIPenHpxub031wwLytz/NiP2vyWDxZsq5WUBIy7NdAAuAMUrEQ=="
        private const val ORGANIZATION = "firstchoice"
    }
 }
