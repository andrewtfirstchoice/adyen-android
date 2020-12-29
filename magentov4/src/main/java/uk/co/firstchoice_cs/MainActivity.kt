package uk.co.firstchoice_cs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.speech.RecognizerIntent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.adyen.checkout.dropin.DropIn
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.novoda.merlin.*
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.login_progress.*
import kotlinx.coroutines.*
import me.msfjarvis.apprate.AppRate
import org.koin.androidx.viewmodel.ext.android.viewModel
import uk.co.firstchoice_cs.App.Companion.hasCustomerAPIAuthToken
import uk.co.firstchoice_cs.App.Companion.instance
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED_MOBILE
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED_WIFI
import uk.co.firstchoice_cs.Settings.ACCOUNT_LOGIN_FAIL_TEST
import uk.co.firstchoice_cs.Settings.CONNECTIVITY_REQUEST_CODE
import uk.co.firstchoice_cs.Settings.GUEST_LOGIN_FAIL_TEST
import uk.co.firstchoice_cs.Settings.NEW_UPDATE_AVAILABLE_TEST
import uk.co.firstchoice_cs.Settings.SPEECH_REQUEST_CODE
import uk.co.firstchoice_cs.Settings.STORAGE_PERMISSIONS
import uk.co.firstchoice_cs.Settings.adminExpiryMS
import uk.co.firstchoice_cs.Settings.bearerCustomerAuth
import uk.co.firstchoice_cs.Settings.customerAuthTokenExpiryMS
import uk.co.firstchoice_cs.Settings.customerExpiryMS
import uk.co.firstchoice_cs.Settings.magentoBearerAdmin
import uk.co.firstchoice_cs.Settings.magentoBearerCustomer
import uk.co.firstchoice_cs.Settings.priceExpiryMS
import uk.co.firstchoice_cs.adyen.PaymentMethodsViewModel
import uk.co.firstchoice_cs.basket.BasketFragment
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls.searchDocument
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.cart.CartItem
import uk.co.firstchoice_cs.core.database.cart.CartViewModel
import uk.co.firstchoice_cs.core.helpcrunch.HelpCrunch
import uk.co.firstchoice_cs.core.helpers.CartHelper
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.core.helpers.LoginHelper
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.navigation.setupWithNavController
import uk.co.firstchoice_cs.core.onboarding.OnBoardingFragment
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.shared.*
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.FireBaseCompletionInterface
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import uk.co.firstchoice_cs.firstchoice.BuildConfig.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.ActivityMainBinding
import uk.co.firstchoice_cs.more.ContactUsFragment
import uk.co.firstchoice_cs.more.PreferencesFragment
import uk.co.firstchoice_cs.more.SuggestAFeature
import uk.co.firstchoice_cs.more.WebFragment.Companion.WEB_FRAGMENT_SHOWING
import uk.co.firstchoice_cs.partsid.PartsIDFragment
import uk.co.firstchoice_cs.store.fragments.ScannerFragment
import uk.co.firstchoice_cs.store.fragments.ShopSearchFragment
import uk.co.firstchoice_cs.store.vm.MainActivityViewModel
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),
        Connectable,
        Disconnectable,
        Bindable,
        SignInFragment.OnFragmentInteractionListener,
        SuggestAFeature.OnFragmentInteractionListener,
        ManualsFragment.OnFragmentInteractionListener,
        ManualsSearchResultsFragment.OnFragmentInteractionListener,
        PartsIDFragment.OnFragmentInteractionListener,
        CollectionFragment.OnFragmentInteractionListener,
        ItemInCollectionsFragment.OnFragmentInteractionListener,
        ManualDetailsFragment.OnFragmentInteractionListener,
        ShopSearchFragment.OnFragmentInteractionListener,
        ScannerFragment.OnFragmentInteractionListener,
        OnBoardingFragment.OnFragmentInteractionListener,
        ContactUsFragment.OnFragmentInteractionListener,
        PreferencesFragment.OnFragmentInteractionListener,
        BasketFragment.OnFragmentInteractionListener,
        AddPartFragment.OnFragmentInteractionListener,
        ScrollAwareInterface{



    private var userAttemptedToLoginThroughAccount: Boolean = false
    private lateinit var mainViewModel: MainActivityViewModel
    private lateinit var binding: ActivityMainBinding

    private var cartItems: List<CartItem> = ArrayList()
    private var loginDialog: LoginDialog? = null
    private var navigateToCheckout = false
    var TAG = "MainActivity"

    private val paymentMethodsViewModel: PaymentMethodsViewModel by viewModel()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        if (intent.hasExtra(DropIn.RESULT_KEY)) {
            instance.paymentCallResult = intent.getStringExtra(DropIn.RESULT_KEY)
            navigateToCheckout = true
            print(instance.awaitingPaymentResult)

       }

        initObservables()

        setupViewModels()

        setupBottomNavigationBar()

        binding.fabChat.setOnClickListener { onChat() }

        setupConnectible()

        setConnectivityValues()

        upgradeData()

        checkOnBoarding()

        initManualsCount()


    }



    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Logger.d(TAG, "onNewIntent")
        if (intent?.hasExtra(DropIn.RESULT_KEY) == true) {
            Toast.makeText(this, intent.getStringExtra(DropIn.RESULT_KEY), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onConnect() {
        dismissed = false
        setConnectivityValues()

        lifecycleScope.launch(context = Dispatchers.Main) {
            binding.matbanner.dismiss()
            if (beard.isConnected && !internetBasedInitCompleted && storagePermissionsChecked) {
                internetBasedInit()
            }
        }
    }

    override fun onDisconnect() {
        setConnectivityValues()
        lifecycleScope.launch(context = Dispatchers.Main) {
            binding.matbanner.show()
        }
    }

    private fun setConnectivityValues() {
        INTERNET_CONNECTED = beard.isConnected
        INTERNET_CONNECTED_WIFI = beard.isConnectedToWifi
        INTERNET_CONNECTED_MOBILE = beard.isConnectedToMobileNetwork

        lifecycleScope.launch(context = Dispatchers.Main) {
            App.internetConnected.value = INTERNET_CONNECTED
        }
    }

    private fun internetStatusChanged() {
        mainViewModel.internetStatusChanged.postValue(App.internetConnected.value)
        
        if (INTERNET_CONNECTED)
            hideBanner()
        else
            showBanner()
    }



    override fun onBind(networkStatus: NetworkStatus?) {

    }

    override fun setSlider(pos: Int) {
        binding.slidingIndicator.getTabAt(pos)?.select()
        binding.slidingIndicator.visibility = View.VISIBLE
    }

    override fun onChat() {
        binding.fabChat.setOnClickListener { HelpCrunch.onChat() }
    }

    override fun hideNavBar() {
        binding.bottomNav.visibility = GONE
    }

    override fun showNavBar() {
        binding.bottomNav.visibility = View.VISIBLE
    }

    override fun onScrollDown() {
        restoreFabState()
    }

    override fun onScrollUp() {
        binding.fabChat.hide()
    }

    @Suppress("DEPRECATION")
    override fun vibrate() {
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(80)
        }
    }

    override fun restoreFabState() {
        binding.fabChat.show()
    }


    private fun initObservables() {

        val cartViewModel = ViewModelProvider(this).get(CartViewModel::class.java)

        cartViewModel.allCartItems.observe(this, {
            cartItems = it
            if (cartItems.isEmpty()) {
                binding.bottomNav.removeBadge(R.id.nav_basket)
            } else {
                binding.bottomNav.getOrCreateBadge(R.id.nav_basket).number = cartItems.size
                binding.bottomNav.getBadge(R.id.nav_basket)?.backgroundColor =
                    ContextCompat.getColor(
                        instance,
                        R.color.fcRed
                    )
            }
        })

        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.NONE)

        GlobalScope.launch(context = Dispatchers.Main) {
            App.chatChanged.value = 0
            App.manualsCount.value = 0
        }

        App.loginState.observe(this, {
            internetStatusChanged()
        })

        App.manualsCount.observe(this, {
            if (it == 0) {
                binding.bottomNav.removeBadge(R.id.nav_manuals)
            } else {
                binding.bottomNav.getOrCreateBadge(R.id.nav_manuals).number = it
                binding.bottomNav.getOrCreateBadge(R.id.nav_manuals).maxCharacterCount =
                    Constants.BADGE_MAX_CHARACTERS
                binding.bottomNav.getBadge(R.id.nav_manuals)?.backgroundColor =
                    ContextCompat.getColor(
                        instance,
                        R.color.fcRed
                    )
            }
        })

        App.chatChanged.observe(this, {
            binding.fabChat.count = it
        })
    }

    private fun initManualsCount()
    {
        val allList = DocumentManager.instance?.allDocuments
        App.manualsCount.value = allList?.size?:0
    }

    private fun checkOnBoarding() {
        if (!SavePrefs.onBoarding || TestingConstants.ON_BOARDING_ON_TEST) {
            showOnBoarding()
        } else {
            hideOnBoarding()
        }
    }

    override fun onPause() {
        merlin.unbind()
        super.onPause()
    }

    private fun setupViewModels() {
        mDBViewModel = ViewModelProvider(this).get(DBViewModel::class.java)
        mFireBaseViewModel = ViewModelProvider(this).get(FireBaseViewModel::class.java)
        mAnalyticsViewModel = ViewModelProvider(this).get(AnalyticsViewModel::class.java)
        mAnalyticsViewModel.mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mainViewModel = ViewModelProvider(this).get(MainActivityViewModel::class.java)
    }

    private fun setupConnectible() {

        merlin = createMerlin()
        beard = createBeard()

        binding.matbanner.setLeftButtonListener {
            dismissed = true
            binding.matbanner.dismiss()
        }
        binding.matbanner.setRightButtonListener {
            dismissed = false
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    override fun onStart() {
        super.onStart()
        merlin.bind()
    }

    override fun onStop() {
        super.onStop()
        merlin.unbind()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onResume() {
        super.onResume()
        registerConnectable(this)
        registerDisconnectable(this)
        registerBindable(this)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        if (!storagePermissionsChecked) {
            checkPermission()
        }
    }

    private fun checkPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )) {
                LoggingHelper.debugMsg("Permissions", "show explanation to user")
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSIONS
                )
            }
        } else {
            storagePermissionsChecked = true
            storageChecked()
        }
    }

    private fun upgradeData() {
        if (!SavePrefs.isUpgraded) {
            SavePrefs.setIsUpgraded(true)
            val documentMngr = DocumentManager.instance ?: return
            val docs = documentMngr.allDocuments
            lifecycleScope.launch(Dispatchers.IO) {
                for (documentEntry in docs) {
                    if (documentEntry.linkedTo == null && !documentEntry.upgraded) {
                        documentEntry.updatedIndex++
                        val fileName = documentEntry.fileName.removeSuffix(".pdf")
                        val documentV4 = searchDocument(fileName)
                        documentEntry.upgraded = true
                        if (documentV4 != null) {
                            documentEntry.linkedTo = documentV4.document?.get(0)?.linkedTo?.get(0)
                        }
                    }
                }
                documentMngr.updateAllDocuments()
            }
        }
    }


    private fun checkForNewExpiryTimes() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("android/general/expiry")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    if (child.key.equals("customerExpiryMS", ignoreCase = true)) {
                        customerExpiryMS = child.value as Long
                        SavePrefs.saveCustomerExpiryLimit(customerAuthTokenExpiryMS)
                    }
                    if (child.key.equals("adminExpiryMS", ignoreCase = true)) {
                        adminExpiryMS = child.value as Long
                        SavePrefs.saveAdminExpiryLimit(adminExpiryMS)
                    }
                    if (child.key.equals("customerApiTokenExpiryMS", ignoreCase = true)) {
                        customerAuthTokenExpiryMS = child.value as Long
                        SavePrefs.saveCustomerAuthExpiryLimit(customerAuthTokenExpiryMS)
                    }
                    if (child.key.equals("priceExpiryMS", ignoreCase = true)) {
                        priceExpiryMS = child.value as Long
                        SavePrefs.savePriceExpiryLimit(priceExpiryMS)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun checkForUpdates() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("android/general/latestVersion")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var build: Long = 0
                var live = false
                for (child in dataSnapshot.children) {


                    if (child.key.equals("build", ignoreCase = true)) build = child.value as Long
                    if (child.key.equals("live", ignoreCase = true)) live = child.value as Boolean
                }
                val typedValue = TypedValue()
                instance.resources.getValue(R.dimen.version, typedValue, true)
                val versionCode = BuildConfig.VERSION_CODE
                if (versionCode < build || NEW_UPDATE_AVAILABLE_TEST) {
                    if (live) {
                        Alerts.showUpdateAvailableAlert(object : Alerts.AlertResponse {
                            override fun processPositive(output: Any?) {
                                Alerts.getUpdate(object : Alerts.AlertResponse {
                                    override fun processPositive(output: Any?) {

                                    }

                                    override fun processNegative(output: Any?) {
                                    }
                                })
                            }

                            override fun processNegative(output: Any?) {}
                        })
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun registerConnectable(c: Connectable) {
        merlin.registerConnectable(c)
    }

    private fun registerDisconnectable(d: Disconnectable) {
        merlin.registerDisconnectable(d)
    }

    private fun registerBindable(bind: Bindable) {
        merlin.registerBindable(bind)
    }

    private fun internetBasedInit() {
        if (!App.initialLoginCompleted &&  binding.onBoardingFragment.visibility == GONE) {
            lifecycleScope.launch(context = Dispatchers.IO) {
                processLogin()
            }
        }

        if (!internetBasedInitCompleted) {

            configureRatings()

            lifecycleScope.launch(context = Dispatchers.IO) {
                val resultLogin = mFireBaseViewModel.loginAnonymously(FirebaseAuth.getInstance())
                if (resultLogin == null) {
                    internetBasedInitCompleted = false
                    return@launch
                } else {
                    mFireBaseViewModel.setupAnalytics()
                    mFireBaseViewModel.loadRecognitionSuggest()
                    mFireBaseViewModel.loadBrands()
                    mFireBaseViewModel.loadCatalogues()
                    mFireBaseViewModel.getShippingFlatRatesFromFireBase()
                    mFireBaseViewModel.getUrlsFromFireBase(object : FireBaseCompletionInterface {
                        override fun onComplete(obj: Any?) {

                            internetBasedInitCompleted = true
                            return
                        }

                        override fun onError(obj: Any?) {
                            internetBasedInitCompleted = false
                            return
                        }
                    })
                    return@launch
                }
            }

            internetBasedInitCompleted = false
        }
    }






    //////////////////////////////////////////////////////////////////////////////
    // When this app launches it gets a fresh token for customer and admin
    // The customer details, cart and shipping methods are collected at the start
    // and are held in the App instance
    //////////////////////////////////////////////////////////////////////////////

    private fun canLogin():Boolean
    {
        return App.loginState.value!= uk.co.firstchoice_cs.Settings.LoginState.LOGGING_IN_ACCOUNT
                && App.loginState.value!= uk.co.firstchoice_cs.Settings.LoginState.LOGGING_IN_GUEST
                && binding.onBoardingFragment.visibility == GONE && LoginHelper.networkConditionsOKForLogin()
    }

    override fun processLogin() {
        if (canLogin()) {
            lifecycleScope.launch(context = Dispatchers.IO) {
                withContext(Dispatchers.Main)
                {
                    showLoginProgressDialog()
                    delay(200) //this is to ensure the login dialog is fully initialised
                }

                if (!SavePrefs.password.isNullOrBlank() && !SavePrefs.username.isNullOrBlank()) {
                    userAttemptedToLoginThroughAccount = true
                    processLoginAccount()
                } else {
                    processLoginGuest()
                }
            }
        }
    }

    private fun processLoginAccount()
    {
        LoginHelper.clearLoginState()
        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.LOGGING_IN_ACCOUNT)
        loginDialog?.init()
        loginDialog?.setMode()
        loginMagento()
        loginCustomer()
        LoginHelper.logStatus()
        loginCompleteAccount()
    }

    private fun processLoginGuest()
    {
        LoginHelper.clearLoginState()
        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.LOGGING_IN_GUEST)
        loginDialog?.init()
        loginDialog?.setMode()
        loginCustomer()
        LoginHelper.logStatus()
        loginCompleteGuest()
    }

    private fun loginCompleteAccount() {
        lifecycleScope.launch(context = Dispatchers.Main) {

            val isLoggedIn = LoginHelper.isLoggedIn()
            if(isLoggedIn && !ACCOUNT_LOGIN_FAIL_TEST)
            {
                loginDialog?.dismiss()
                loginSuccessAccount()
            }
            else
            {
                processLoginGuest()
            }
        }
    }

    private fun loginCompleteGuest() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            val isLoggedIn = LoginHelper.isLoggedIn()
            if(isLoggedIn && !GUEST_LOGIN_FAIL_TEST)
            {
                loginDialog?.dismiss()
                loginSuccessGuest()
            }
            else
            {
                defaultLogin()
            }
        }
    }

    private fun loginSuccessAccount() {
        App.initialLoginCompleted = true
        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.LOGGED_IN_ACCOUNT)
        val mCustomer = App.magentoCustomer
        val fullName = if (mCustomer != null) CartHelper.customerFullName(mCustomer) else ""
        val message = "$fullName ${getString(R.string.signed_in)}"
        showSnackBarMessage(message, Snackbar.LENGTH_SHORT)
    }

    private fun loginSuccessGuest() {
        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.LOGGED_IN_AS_GUEST)
        val message = getString(R.string.logged_in_as_guest)
        if(!userAttemptedToLoginThroughAccount) {
           showLoginWarning()
        }
        showSnackBarMessage(message, Snackbar.LENGTH_SHORT)
    }

    private fun showLoginWarning() {
        val message = "Failed to login to account : logged in as guest, using retail prices"
        showSnackBarMessage(message, Snackbar.LENGTH_SHORT)
    }


    override fun loginAsGuest() {
        processLogin()
    }

    private fun defaultLogin() {
        SavePrefs.setLoggedIn(uk.co.firstchoice_cs.Settings.LoginState.DEFAULT_LOGIN)
        val message = "Logged in as guest, using retail prices"
        showSnackBarMessage(message, Snackbar.LENGTH_SHORT)
        loginDialog?.dismiss()
        showRetryDialog()
    }



    private fun loginMagento()
    {
        if (LoginHelper.networkConditionsOKForLogin()) {
            loginDialog?.setStatusText("Get Magento Customer Token.")
            loginDialog?.setIndicatorColours(0, LoginStates.BUSY)
            magentoBearerCustomer()
        }

        loginDialog?.updateDialogStatus(App.hasCustomerToken, 0)

        if(!App.hasCustomerToken)
            return

        if (LoginHelper.networkConditionsOKForLogin()) {
            loginDialog?.setStatusText("Get Magento Admin Token..")
            loginDialog?.setIndicatorColours(1, LoginStates.BUSY)
            magentoBearerAdmin()
        }

        loginDialog?.updateDialogStatus(App.hasAdminToken, 1)

        if(!App.hasAdminToken)
            return

        if (LoginHelper.networkConditionsOKForLogin()) {
            loginDialog?.setStatusText("Getting Magento Customer.")
            loginDialog?.setIndicatorColours(2, LoginStates.BUSY)
            LoginHelper.getMagentoCustomerDetails()
            App.magentoCustomer?.addresses?.let { App.addresses.addMagentoAddresses(it) }
        }

        loginDialog?.updateDialogStatus(App.hasMagentoCustomerDetails, 2)

        if(!App.hasMagentoCustomerDetails)
            return
    }


    private fun loginCustomer()
    {
        if (LoginHelper.networkConditionsOKForLogin()) {
            loginDialog?.setStatusText("Get Customer API Auth Token.")
            loginDialog?.setIndicatorColours(3, LoginStates.BUSY)
            val token = bearerCustomerAuth()
            if(token.isNotBlank())
                hasCustomerAPIAuthToken = true
        }

        loginDialog?.updateDialogStatus(hasCustomerAPIAuthToken, 3)

        if(!hasCustomerAPIAuthToken)
            return

        if (LoginHelper.networkConditionsOKForLogin()) {
            val magentoCustomerId = GlobalAppData().customerNumber
            loginDialog?.setStatusText("Getting Customer.")
            loginDialog?.setIndicatorColours(4, LoginStates.BUSY)
            LoginHelper.getCustomer(magentoCustomerId)
        }

        loginDialog?.updateDialogStatus(App.hasCustomer, 4)

        if(!App.hasCustomer)
            return

        if(App.loginState.value != uk.co.firstchoice_cs.Settings.LoginState.LOGGING_IN_GUEST) {
            if (LoginHelper.networkConditionsOKForLogin()) {
                val magentoCustomerId = GlobalAppData().customerNumber
                loginDialog?.setStatusText("Getting Customer Addresses")
                loginDialog?.setIndicatorColours(4, LoginStates.BUSY)
                LoginHelper.getCustomerAddresses(magentoCustomerId)
            }

            loginDialog?.updateDialogStatus(App.hasCustomerAddresses, 4)

            if (!App.hasCustomerAddresses) {
                return
            }
        }
    }


    private fun configureRatings() {
        AppRate(this)
                .setMinDaysUntilPrompt(7)
                .setMinLaunchesUntilPrompt(20)
                .setShowIfAppHasCrashed(false)
                .init()
    }


    private fun setupBottomNavigationBar() {
        val navIDs = ArrayList<Int>()
        navIDs.add(R.navigation.nav_store)
        navIDs.add(R.navigation.nav_manuals)
        navIDs.add(R.navigation.nav_parts_id)
        navIDs.add(R.navigation.nav_basket)
        navIDs.add(R.navigation.nav_more)

        val currentNavController =  binding.bottomNav.setupWithNavController(
            navIDs,
            supportFragmentManager,
            R.id.fragment_nav_host,
            intent
        )
        currentNavController.observe(this) { navController ->
            val id = navController.currentDestination?.id
            if (id != null) {
                when (id) {
                    R.id.shopRootFragment -> setSlider(0)
                    R.id.shopSearchFragment -> setSlider(0)
                    R.id.manufacturerSearchFragment -> setSlider(0)
                    R.id.partClassesSearchFragment -> setSlider(0)
                    R.id.equipmentSearchFragment -> setSlider(0)
                    R.id.shopPartsResultFragment -> setSlider(0)
                    R.id.scannerFragment -> setSlider(0)
                    R.id.manuals_details_fragment_store -> setSlider(0)
                    R.id.orderFragment -> setSlider(0)
                    R.id.basketFragment -> setSlider(0)
                    R.id.addPartFragmentStore -> setSlider(0)
                    R.id.threeSixtyFragmentStore -> setSlider(0)

                    R.id.manuals_fragment -> setSlider(1)
                    R.id.item_in_collections_fragment -> setSlider(1)
                    R.id.collection_fragment -> setSlider(1)
                    R.id.manuals_details_fragment_manuals -> setSlider(1)
                    R.id.add_edit_collections_fragment -> setSlider(1)
                    R.id.addPartFragmentManuals -> setSlider(1)
                    R.id.threeSixtyFragmentManuals -> setSlider(1)

                    R.id.basket_fragment -> setSlider(2)
                    R.id.checkoutFragment -> setSlider(2)
                    R.id.addNewAddressFragment -> setSlider(2)
                    R.id.deliveryMethodsFragment -> setSlider(2)
                    R.id.orderConfirmationFragment -> setSlider(2)
                    R.id.deliveryAddressFragmentMore -> setSlider(2)

                    R.id.parts_id_fragment -> setSlider(3)
                    R.id.contact_us_fragment -> setSlider(4)
                    R.id.signInFragment -> setSlider(4)
                    R.id.our_location_fragment -> setSlider(4)
                    R.id.preferences_fragment -> setSlider(4)
                    R.id.whats_new_fragment -> setSlider(4)
                    R.id.accountOrdersFragment -> setSlider(4)
                    R.id.accountOrderDetailsFragment -> setSlider(4)
                    R.id.addNewAddressFragmentMore -> setSlider(4)
                    R.id.accountFragmentMore -> setSlider(4)

                }
            }
        }

        if(navigateToCheckout)
        {
            binding.bottomNav.selectedItemId = R.id.nav_basket      
            navigateToCheckout = false
        }





        binding.bottomNav.setOnNavigationItemReselectedListener { item ->
         //   throw RuntimeException("Test Crash V4")
            when (item.itemId) {
                R.id.nav_store -> {
                    val collapseAll = NavOptions.Builder()
                        .setEnterAnim(R.anim.nav_default_enter_anim)
                        .setExitAnim(R.anim.nav_default_exit_anim)
                        .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
                        .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
                        .setPopUpTo(R.id.nav_store, true)
                        .build()
                    currentNavController.value?.navigate(R.id.nav_store, null, collapseAll)
                }
                R.id.nav_manuals -> currentNavController.value?.navigate(R.id.nav_manuals)
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        return  binding.fragmentNavHost.findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    private fun createMerlin(): Merlin {
        return Merlin.Builder()
                .withConnectableCallbacks()
                .withDisconnectableCallbacks()
                .withBindableCallbacks()
                .build(this)
    }

    private fun createBeard(): MerlinsBeard {
        return MerlinsBeard.Builder().build(this)
    }


    override fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
        startActivityForResult(intent, SPEECH_REQUEST_CODE)
    }

    // Function to initiate after permissions are given by user
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSIONS -> if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                storagePermissionsChecked = true
                storageChecked()
            }
        }
    }

    private fun storageChecked() {
        checkForUpdates()
        checkForNewExpiryTimes()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONNECTIVITY_REQUEST_CODE) {
            setConnectivityValues()
        }
        else if(requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE)
        {
            mainViewModel.setPartsImageResult(requestCode, resultCode, data)
        }

        else if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            mainViewModel.setSpeechData(data)
        }
    }


    companion object {
        private lateinit var mFireBaseViewModel: FireBaseViewModel
        private lateinit var mAnalyticsViewModel: AnalyticsViewModel
        private lateinit var mDBViewModel: DBViewModel
        private lateinit var merlin: Merlin
        private lateinit var beard: MerlinsBeard
        private var internetBasedInitCompleted = false
        private var dismissed = false
        private var storagePermissionsChecked = false
    }


    private fun hideOnBoarding() {
        binding.onBoardingFragment.visibility = GONE
    }

    private fun showOnBoarding() {
        binding.onBoardingFragment.visibility = View.VISIBLE
    }

    override fun onBoardingComplete(complete: Boolean) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            hideOnBoarding()
        }

        lifecycleScope.launch(context = Dispatchers.IO) {
            delay(1000)
            processLogin()
        }
    }


    enum class LoginStates(val colour: Int) {
        IDLE(R.color.light_background),
        BUSY(R.color.fcBlue),
        FAILED(R.color.fcRed),
        SUCCESS(R.color.green)
    }


    private fun showLoginProgressDialog() {

        val fm = supportFragmentManager
        loginDialog = LoginDialog.newInstance()
        loginDialog?.show(fm, "")
    }



    private fun showRetryDialog() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            val alert = MaterialAlertDialogBuilder(this@MainActivity)
            alert.setTitle("Failed to login")
            alert.setMessage("Check your internet connection and click retry")
            alert.setCancelable(false)
            alert.setPositiveButton("Retry") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                processLogin()
            }
            alert.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            alert.show()
        }
    }

    override fun showSnackBarMessage(message: String, duration: Int) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            Snackbar.make(binding.mainCoordinator, message, duration).setBackgroundTint(
                ContextCompat.getColor(
                    this@MainActivity,
                    R.color.dark_purple
                )
            ).show()
        }
    }

    private fun showBanner() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (!WEB_FRAGMENT_SHOWING) {
                binding.matbanner.show()
            }
        }
    }

    private fun hideBanner() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.matbanner.dismiss()
        }
    }
}
