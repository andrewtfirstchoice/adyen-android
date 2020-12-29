package uk.co.firstchoice_cs.more

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.multidex.BuildConfig
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.contact_us_fragment.*
import kotlinx.android.synthetic.main.logged_in_layout.*
import kotlinx.android.synthetic.main.login_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.App.Companion.instance
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.SavePrefs.clearUserAndLogout
import uk.co.firstchoice_cs.SavePrefs.userEmail
import uk.co.firstchoice_cs.SavePrefs.userFullName
import uk.co.firstchoice_cs.SavePrefs.userInitials
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.alerts.Alerts.showNoInternetToast
import uk.co.firstchoice_cs.core.helpcrunch.HelpCrunch
import uk.co.firstchoice_cs.core.helpers.Helpers.isPlayStoreInstalled
import uk.co.firstchoice_cs.core.helpers.Helpers.launchEmail
import uk.co.firstchoice_cs.core.helpers.Helpers.launchPhone
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel.Companion.getCreateAccountUrl
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*

class ContactUsFragment : Fragment(R.layout.contact_us_fragment),KoinComponent, OnMapReadyCallback {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity
    private lateinit var mAnalyticsViewModel: AnalyticsViewModel
    private var mListener: OnFragmentInteractionListener? = null
    private val sectionAdapter = SectionedRecyclerViewAdapter()
    private var mMap: GoogleMap? = null
    private var mapView: MapView? = null

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        if(mapView!=null)
            mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onResume() {
        mapView?.onResume()
        processLoggedInLoggedOutStates()
        super.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mListener?.restoreFabState()
        mListener?.setSlider(4)

        recycler.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })

        createAccountButton.setOnClickListener { createCustomerAccount() }
        loginButton.setOnClickListener { NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_more_fragment_to_signInFragment) }

        recycler.layoutManager = LinearLayoutManager(this@ContactUsFragment.context)
        recycler.adapter = sectionAdapter
        recycler.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        setUpToolbar()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@ContactUsFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)



        var mapViewBundle: Bundle? = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }
        mapView = MapView(ctx)
        mapView?.onCreate(mapViewBundle)
        mapView?.getMapAsync(this)

        App.loginState.observe(viewLifecycleOwner, {
            lifecycleScope.launch(context = Dispatchers.Main) {
                if(recycler!=null)
                    processLoggedInLoggedOutStates()
            }
        })
    }


    private fun createCustomerAccount() {
        showWeb(getCreateAccountUrl(), "Create Account")
        logAnalytic("CreateAccountSelected")
    }

    private fun logout() {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle("Confirmation")
        alert.setCancelable(false)
        alert.setMessage("Are you sure you want to logout?")
        alert.setPositiveButton("Yes") { dialog: DialogInterface, _: Int ->
            clearUserAndLogout()
            dialog.dismiss()
            mListener?.showSnackBarMessage("Logged Out", Toast.LENGTH_SHORT)
            mListener?.loginAsGuest()
        }
        alert.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }
        alert.show()
    }

    private fun isUserLoggedIn():Boolean
    {
        return App.loginState.value == Settings.LoginState.LOGGED_IN_ACCOUNT
    }

    private fun processLoggedInLoggedOutStates() {
        val isExpired = instance.customerExpired()
        if (isUserLoggedIn() && !isExpired) {
            loginWrapper?.visibility = View.GONE
            loggedInWrapper?.visibility = View.VISIBLE
            setID(userInitials, userFullName, userEmail)
        } else {
            loginWrapper?.visibility = View.VISIBLE
            loggedInWrapper?.visibility = View.GONE
        }
        renderLists(sectionAdapter, isUserLoggedIn())
    }

    private fun setUpToolbar() {

        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        mAnalyticsViewModel = ViewModelProvider(act as FragmentActivity).get(AnalyticsViewModel::class.java)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.action_menu, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun setID(id: String?, name: String?, email: String?) {
        idField.text = id
        nameText.text = name
        emailText.text = email
    }

    private fun clearLists() {
        sectionAdapter.removeAllSections()
    }

    private fun renderLists(sectionAdapter: SectionedRecyclerViewAdapter, loggedIn: Boolean) {
        clearLists()
        val accountItems = ArrayList<MoreItem>()
        if (loggedIn) {
            accountItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_orders, "My Orders"))
            accountItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_contacts_black_24dp, "My Addresses"))
            accountItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_account_box_black_24dp, "Account Information"))
            accountItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_chat_bubble_24px, "Account Help"))
            accountItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.sign_out_icon, "Logout"))
            sectionAdapter.addSection(MoreSectionItem("MY ACCOUNT", accountItems))
        }
        val helpItems = ArrayList<MoreItem>()
        helpItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_help, "Get Help"))
        helpItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_feedback, "Send Us Feedback"))
        helpItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_suggest, "Suggest a Feature"))
        sectionAdapter.addSection(MoreSectionItem("HELP & SUPPORT", helpItems))
        val contactItems = ArrayList<MoreItem>()
        contactItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_phone, "Call Us"))
        contactItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_email, "Email Us"))
        contactItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_marker, "Map"))
        contactItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.icon_marker, "Find Us"))
        sectionAdapter.addSection(MoreSectionItem(getString(R.string.more_contact_us_section), contactItems))
        val aboutItems = ArrayList<MoreItem>()
        aboutItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.more_facebook_tint), R.drawable.ic_rss_feed_black_24dp, "Company News"))
        aboutItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.more_facebook_tint), R.drawable.icon_facebook, "Follow us on Facebook"))
        aboutItems.add(MoreItem(NO_TINT, R.drawable.icon_instagram, "Follow us on Instagram"))
        aboutItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.more_twitter_tint), R.drawable.icon_twitter, "Follow us on Twitter"))
        aboutItems.add(MoreItem(NO_TINT, R.drawable.icon_fc, "View Our Website"))
        sectionAdapter.addSection(MoreSectionItem(getString(R.string.more_about), aboutItems))
        val policyItems = ArrayList<MoreItem>()
        policyItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.more_terms, getString(R.string.Terms)))
        policyItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.more_privacy, getString(R.string.Privacy)))
        sectionAdapter.addSection(MoreSectionItem(getString(R.string.more_policies), policyItems))
        val versionItems = ArrayList<MoreItem>()
        versionItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.preferences, "Preferences"))
        versionItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_star_black_24dp, getString(R.string.Rate)))
        versionItems.add(MoreItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_new_releases_black_24dp, getString(R.string.WhatsNew)))
        sectionAdapter.addSection(MoreSectionItem(getString(R.string.more_version) + " " + BuildConfig.VERSION_NAME, versionItems))
        sectionAdapter.notifyDataSetChanged()
    }

    private fun email() {
        launchEmail("enquiries@firstchoice-cs.co.uk", "", "Android Mobile App Enquiry")
    }

    private fun call() {
        launchPhone(act as AppCompatActivity, Settings.PHONE_NUMBER)
    }

    private fun showWeb(url: String, title: String) {
        val bundle = Bundle()
        bundle.putString(WebFragment.ARG_PARAM_URL, url)
        bundle.putString(WebFragment.ARG_PARAM_TITLE, title)
        NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_web_fragment, bundle)
    }

    private val help: Unit
        get() {
            showWeb("https://www.firstchoice-cs.co.uk/solutions/app", "Get Help")
            if (isAdded) mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("GetHelpSelected", Bundle())
        }


    private fun logAnalytic(msg: String) {
        if (isAdded) mAnalyticsViewModel.mFirebaseAnalytics?.logEvent(msg, Bundle())
    }

    private fun sendFeedback() {

        logAnalytic("SendFeedbackSelected")
        if(!AppStatus.INTERNET_CONNECTED) showNoInternetToast() else NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_fragment_to_feedback)
    }

    private fun suggestFeature() {

        logAnalytic("SuggestFeatureSelected")
        if(!AppStatus.INTERNET_CONNECTED) showNoInternetToast() else NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_fragment_to_suggestAFeature)
    }


    private fun callUs() {
        call()
        logAnalytic("CallUsSelected")
    }

    private fun emailUs() {
        email()
        logAnalytic("EmailUsSelected")
    }

    private fun ourLocation() {
        NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_to_our_location_fragment)
        logAnalytic("OurLocationSelected")
    }

    private fun companyNews() {
        showWeb("https://www.firstchoice-cs.co.uk/blog/", "CompanyNews")
        logAnalytic("CompanyNewsSelected")
    }

    private fun followOnFacebook() {
        showWeb("http://www.facebook.com/FirstChoiceCS", "Our Facebook")
        logAnalytic("FacebookSelected")
    }

    private fun followOnInstagram() {
        showWeb("https://www.instagram.com/firstchoicegroup", "Our Instagram")
        logAnalytic("InstagramSelected")
    }

    private fun followOnTwitter() {
        showWeb("https://twitter.com/firstchoice_cs", "Our Twitter")
        logAnalytic("TwitterSelected")
    }

    private fun followOnWebSite() {
        showWeb("https://www.firstchoice-cs.co.uk/app/", " Our Website")
        logAnalytic("WebsiteSelected")
    }

    private fun termsAndConditions() {
        showWeb("https://www.firstchoice-cs.co.uk/terms-conditions/", "Terms & Conditions")
        logAnalytic("TermsConditionsSelected")
    }

    private fun privacyPolicy() {
        showWeb("https://www.firstchoice-cs.co.uk/privacy-policy-cookie-restriction-mode/", "Privacy Policy")
        logAnalytic("PrivacyPolicySelected")
    }

    private fun rateUs() {
        if (isPlayStoreInstalled()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=uk.co.firstchoice_cs.firstchoice")
                intent.setPackage("com.android.vending")
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                showWeb("https://play.google.com/store/apps/details?id=uk.co.firstchoice_cs.firstchoice", "Rate Us")
            }
        } else {
            showWeb("https://play.google.com/store/apps/details?id=uk.co.firstchoice_cs.firstchoice", "Rate Us")
        }
        mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("RateOnStoreSelected", Bundle())
    }

    private fun whatsNew() {
        logAnalytic("WhatsNewSelected")
        NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_to_whats_new_fragment)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }
    private fun findUs() {
        showWeb("https://www.firstchoice-cs.co.uk/contact/", "Contact Us")

        logAnalytic("OurLocationSelected")
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun onScrollUp()
        fun restoreFabState()
        fun setSlider(pos: Int)
        fun showSnackBarMessage(message: String, duration: Int)
        fun loginAsGuest()
    }

    class MoreItem internal constructor(var tint: Int, var drawable: Int, var text: String)


    internal inner class MoreSectionItem(private val title: String, private val list: List<MoreItem>) : Section(SectionParameters.builder()
            .itemResourceId(R.layout.more_sectioned_list_details)
            .headerResourceId(R.layout.sectioned_list_item_header)
            .build()) {
        override fun getContentItemsTotal(): Int {
            return list.size
        }

        override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
            return ItemViewHolder(view)
        }

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemHolder = holder as ItemViewHolder
            itemHolder.text.text = list[position].text
            itemHolder.icon.setImageResource(list[position].drawable)
            val tint = list[position].tint
            if (tint != NO_TINT) itemHolder.icon.setColorFilter(tint)
            val listener = View.OnClickListener {
                when {
                    title.equals(getString(R.string.more_help_section), ignoreCase = true) -> {
                        when (position) {
                            HELP -> help
                            FEEDBACK -> sendFeedback()
                            FEATURE -> suggestFeature()
                        }
                    }
                    title.equals(getString(R.string.more_contact_us_section), ignoreCase = true) -> {
                        when (position) {
                            CALL_US -> callUs()
                            EMAIL -> emailUs()
                            LOCATION -> ourLocation()
                            MAP -> findUs()
                        }
                    }
                    title.equals(getString(R.string.more_about), ignoreCase = true) -> {
                        when (position) {
                            COMPANY_NEWS -> companyNews()
                            FACEBOOK -> followOnFacebook()
                            INSTAGRAM -> followOnInstagram()
                            TWITTER -> followOnTwitter()
                            WEBSITE -> followOnWebSite()
                        }
                    }
                    title.equals(getString(R.string.more_policies), ignoreCase = true) -> {
                        when (position) {
                            TERMS_AND_CONDITIONS -> termsAndConditions()
                            PRIVACY_POLICY -> privacyPolicy()
                        }
                    }
                    title.equals(getString(R.string.my_account), ignoreCase = true) -> {
                        when (position) {
                            MY_ORDERS -> NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_more_fragment_to_accountOrdersFragment)
                            MY_ADDRESSES -> NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_more_fragment_to_deliveryAddressFragment)
                            ACCOUNT_INFORMATION -> NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_more_fragment_to_accountFragment)
                            ACCOUNT_HELP -> HelpCrunch.onChat()
                            SIGN_OUT -> logout()
                        }
                    }
                }
                if (title.contains(getString(R.string.more_version))) {
                    when (position) {
                        PREFERENCES -> NavHostFragment.findNavController(this@ContactUsFragment).navigate(R.id.action_contact_us_to_preferences_fragment)
                        RATE -> rateUs()
                        WHATS_NEW -> whatsNew()
                    }
                }
            }
            itemHolder.nav.setOnClickListener(listener)
            itemHolder.rootView.setOnClickListener(listener)
        }

        override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
            return HeaderViewHolder(view)
        }

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
            val headerHolder = holder as HeaderViewHolder
            if (headerHolder.headerTitle != null) headerHolder.headerTitle.text = title
            if (title.equals("MY ACCOUNT", ignoreCase = true)) {
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0)
            }
        }

        private inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val headerTitle: TextView? = view.findViewById(R.id.moreHeaderText)

        }

        internal inner class ItemViewHolder(val rootView: View) : RecyclerView.ViewHolder(rootView) {
            val text: TextView = rootView.findViewById(R.id.text)
            val icon: ImageView = rootView.findViewById(R.id.icon)
            val nav: View = rootView.findViewById(R.id.nav)

        }

    }

    companion object {
        private const val MAP_VIEW_BUNDLE_KEY = "AIzaSyAE__q9Wb74fHR6Hcs1qxSiG5uQ1Sizgak"
        private const val NO_TINT = -1

        //ACCOUNT
        private const val MY_ORDERS = 0
        private const val MY_ADDRESSES = 1
        private const val ACCOUNT_INFORMATION = 2
        private const val ACCOUNT_HELP = 3
        private const val SIGN_OUT = 4

        //POLICIES
        private const val TERMS_AND_CONDITIONS = 0
        private const val PRIVACY_POLICY = 1
        //ABOUT
        private const val COMPANY_NEWS = 0
        private const val FACEBOOK = 1
        private const val INSTAGRAM = 2
        private const val TWITTER = 3
        private const val WEBSITE = 4

        //LOCATION
        private const val LOCATION = 2
        private const val MAP = 3
        private const val EMAIL = 1
        private const val CALL_US = 0
        //HELP & SUPPORT
        private const val HELP = 0
        private const val FEEDBACK = 1
        private const val FEATURE = 2
        //PREFERENCES
        private const val PREFERENCES = 0
        private const val RATE = 1
        private const val WHATS_NEW = 2
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap
        val fc = LatLng(Settings.LATITUDE, Settings.LONGITUDE)
        mMap?.addMarker(MarkerOptions().position(fc).title("First Choice Catering Ltd"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLng(fc))
        mMap?.setMinZoomPreference(15f)
    }
}