package uk.co.firstchoice_cs.core.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.set


class FireBaseViewModel : ViewModel(), KoinComponent {

    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    fun getShippingFlatRatesFromFireBase() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("shippingMaps")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                LoggingHelper.errorMsg("getShippingFlatRatesFromFireBase", p0.toString())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    Helpers.shippingMap.set(data.key.toString(), value = data.value as String)
                }
            }
        })
    }

    fun getUrlsFromFireBase(callback: FireBaseCompletionInterface?) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("urls")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                LoggingHelper.errorMsg("getUrlsFromFireBase", p0.toString())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (data in dataSnapshot.children) {
                    urlMap.set(data.key.toString(), value = data.value as String)
                }
                callback?.onComplete(null)
            }
        })
    }

    fun convertWords(phrase: String): MutableList<String> {

        val cleanedPhrases: MutableList<String> = ArrayList()

        val alternateWord = wordMap[phrase.toLowerCase(Locale.ROOT)]
        if (alternateWord == null) {
            if (!cleanedPhrases.contains(phrase))
                cleanedPhrases.add(phrase)
        } else {
            if (!cleanedPhrases.contains(alternateWord))
                cleanedPhrases.add(alternateWord)
        }


        return cleanedPhrases
    }




    fun loadRecognitionSuggest() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("recognitionSuggest")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    for (alternative in child.value as ArrayList<*>) {
                        try {
                            wordMap[alternative as String?] = child.key
                        } catch (ex: Exception) {
                            print(ex.message ?: "")
                        }
                    }
                }
                LoggingHelper.debugMsg("WordMap", wordMap.size.toString())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    suspend fun loginAnonymously(firebaseAuth: FirebaseAuth): AuthResult? {
        return try {
            firebaseAuth
                .signInAnonymously()
                .await()
        } catch (e: Exception) {
            null
        }
    }

    fun setupAnalytics() {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(ctx)
    }

    fun loadBrands() {
        val storage = FirebaseStorage.getInstance()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("brandLogos")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (brand in dataSnapshot.children)
                {
                    Helpers.brandsImageMap[brand.key.toString()] = storage.getReferenceFromUrl(brand.value.toString())
                }
                LoggingHelper.debugMsg("loadBrands","Brands Loaded " + Helpers.brandsImageMap.size)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    fun loadCatalogues() {
        val storage = FirebaseStorage.getInstance()
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("catalogues")
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (catalogue in dataSnapshot.children)
                {
                    Helpers.cataloguesImageMap[catalogue.key.toString()] = storage.getReferenceFromUrl(catalogue.value.toString())
                }
                LoggingHelper.debugMsg("loadCatalogues","Catalogues Loaded " + Helpers.cataloguesImageMap.size)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


    companion object {
        //these are the urls that are called in the app
        private fun getBaseMagento(): String {
            return urlMap.getValue("baseMagento") + urlMap.getValue("hostMagento")
        }

        fun getCustomerDetailsAPI(): String {
            return getBaseMagento() + urlMap.getValue("customerDetailsAPI")
        }

        fun getDashBoardUrl(): String {
            return getBaseMagento() + getDashBoardUrlSuffix()
        }

        private fun getDashBoardUrlSuffix(): String {
            return urlMap.getValue("dashBoardURL")
        }

        fun getOrderHistoryUrl(): String {
            return getBaseMagento() + getOrderHistoryUrlSuffix()
        }

        private fun getOrderHistoryUrlSuffix(): String {
            return urlMap.getValue("orderHistoryUrl")
        }

        fun getWishListUrl(): String {
            return getBaseMagento() + getWishListUrlSuffix()
        }

        private fun getWishListUrlSuffix(): String {
            return urlMap.getValue("wishListUrl")
        }

        fun getCreateCustomerAccountUrl(): String {
            return getBaseMagento() + getCreateCustomerAccountUrlSuffix()
        }

        fun getCreateCustomerAccountUrlSuffix(): String {
            return urlMap.getValue("createCustomerAccountURL")
        }

        fun getCustomerAuthUrl(): String {
            return getBaseMagento() + getCustomerAuthUrlSuffix()
        }

        private fun getCustomerAuthUrlSuffix(): String {
            return urlMap.getValue("customerAuthURL")
        }

        fun getAdminAuthUrl(): String {
            return getBaseMagento() + getAdminAuthUrlSuffix()
        }

        private fun getAdminAuthUrlSuffix(): String {
            return urlMap.getValue("adminAuthURL")
        }

        fun getLogoutUrl(): String {
            return getBaseMagento() + getLogoutUrlSuffix()
        }

        private fun getLogoutUrlSuffix(): String {
            return urlMap.getValue("logoutURL")
        }

        fun getLoginPostUrlSuffix(): String {
            return urlMap.getValue("loginPostURL")
        }

        fun getLoginPortalUrl(): String {
            return getBaseMagento() + getLoginPortalUrlSuffix()
        }

        fun getLoginPortalUrlSuffix(): String {
            return urlMap.getValue("loginPortalURL")
        }

        fun getB2BSignUpUrlSuffix(): String {
            return urlMap.getValue("b2bSignUpURL")
        }

        fun getB2BSignUpPostUrlSuffix(): String {
            return urlMap.getValue("b2bPostURL")
        }

        fun getGuestCreateAccountUrlSuffix(): String {
            return urlMap.getValue("createCustomerAccountURL")
        }

        fun getGuestPostUrlSuffix(): String {
            return urlMap.getValue("guestCreateAccountURL")
        }

        fun getSearchMagentoUrl(): String {
            return "https://www.firstchoice-cs.co.uk" + urlMap.getValue("searchMagentoURL")
        }

        fun getSearchMagentoUrlSuffix(): String {
            return urlMap.getValue("searchMagentoURL")
        }

        fun getCreateAccountUrl(): String {
            return "https://www.firstchoice-cs.co.uk" + urlMap.getValue("createCustomerAccountURL")
        }

        const val TAG = "FireBaseViewModel"

        private val urlMap = mutableMapOf(
            "hostMagento" to "firstchoice-cs.co.uk/",
            "baseMagento" to "https://",
            "customerDetailsAPI" to "/rest/V1/customers/me",
            "dashBoardURL" to "/customer/account/dashboard/",
            "orderHistoryUrl" to "/sales/order/history/",
            "wishListUrl" to "/wishlist/",
            "customerAuthURL" to "/rest/V1/integration/customer/token",
            "adminAuthURL" to "/rest/V1/integration/admin/token",
            "logoutURL" to "/customer/account/logout/",
            "loginPortalURL" to "/b2b/portal/login/",
            "loginPostURL" to "/customer/account/loginPost/",
            "b2bSignUpURL" to "/b2b/portal/register/prereg/0/",
            "b2bPostURL" to "/b2b/portal/registerpost/",
            "createCustomerAccountURL" to "/customer/account/create/",
            "guestCreateAccountURL" to "/customer/account/createpost/",
            "searchMagentoURL" to "/catalogsearch/result/?q="
        )


        val wordMap = mutableMapOf<String?, String?>()

    }
}
