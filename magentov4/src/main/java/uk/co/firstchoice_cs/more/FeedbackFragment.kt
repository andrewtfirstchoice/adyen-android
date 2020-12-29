package uk.co.firstchoice_cs.more

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.fragment_feedback.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*


class FeedbackFragment : Fragment(R.layout.fragment_feedback), KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    var sending = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initComponents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_feedback, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun setUpToolbar() {
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this))
        act.setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    }


    private fun initComponents() {

        setUpToolbar()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@FeedbackFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        toolbar.setNavigationOnClickListener{
            NavHostFragment.findNavController(this@FeedbackFragment).navigateUp()
        }

        feedbackEt.setText("")
        loadClientDetails()
        dismissKeyBoard()

        button.setOnClickListener {
            if (validate()) {
                saveClientDetails()
                sendFeedbackRequest(act, feedbackEt.text.toString())

            }
        }

    }


    private fun sendFeedbackRequest(context: Context, feedback: String) {
        if (sending)
            return
        sending = true
        if(AppStatus.INTERNET_CONNECTED) {
            val pref = context.getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
            val name = pref.getString("name", "")
            val company = pref.getString("company", "")
            val email = pref.getString("email", "")
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("feedback")
            val ref = myRef.push()
            val dict: MutableMap<String, Any> = HashMap()
            dict["contactCompany"] = SafetyChecks.ensureNonNullString(company)
            dict["contactEmail"] = SafetyChecks.ensureNonNullString(email)
            dict["contactName"] = SafetyChecks.ensureNonNullString(name)
            dict["feedback"] = feedback
            dict["platform"] = "Android"
            ref.setValue(dict).addOnSuccessListener {
                showSuccess()
            }.addOnFailureListener {
                showError()
            }
        }
        else
        {
            showError()
        }
    }
    private fun showSuccess()
    {
        sending = false
        feedbackEt.setText("")
        if(AppStatus.INTERNET_CONNECTED)
            Alerts.showAlert("Feedback Submitted", "Thank you! Your feedback will help us make future improvements to the app", null)
        else
            Alerts.showAlert("Feedback Queued", "When the internet is connected your feedback will automatically be sent", null)
    }

    private fun showError()
    {
        sending = false
        Alerts.showAlert("Error Submitting Feedback", "Please check your internet connection and retry", null)
    }

    private fun loadClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        partsIDNameEdit.setText(pref.getString("name", ""))
        partsIDCompanyEdit.setText(pref.getString("company", ""))
        partsIDEmailEdit.setText(pref.getString("email", ""))
    }

    private fun saveClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString("name", SafetyChecks.safeString(partsIDNameEdit))
        editor.putString("company", SafetyChecks.safeString(partsIDCompanyEdit))
        editor.putString("email", SafetyChecks.safeString(partsIDEmailEdit))
        editor.apply()
    }

    private fun validate(): Boolean {
            if (feedbackEt.length() == 0) {
                Alerts.showAlert("We need more information", "Please provide your feature request", null)
                return false
            }
        return true
    }

    private val focused: EditText?
        get() {
            if (partsIDNameEdit.isFocused) return partsIDNameEdit
            if (partsIDCompanyEdit.isFocused) return partsIDCompanyEdit
            if (partsIDEmailEdit.isFocused) return partsIDNameEdit
            return if (feedbackEt.isFocused) return feedbackEt else null
        }

    private fun dismissKeyBoard() {
        val focused: View? = focused
        if (focused != null) Helpers.hideKeyboard(focused)
    }
}