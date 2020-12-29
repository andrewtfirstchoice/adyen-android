package uk.co.firstchoice_cs.core.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticsViewModel : ViewModel() {
    @JvmField
    var mFirebaseAnalytics: FirebaseAnalytics? = null

    companion object {
        const val TAG = "AnalyticsViewModel"
    }
}