package uk.co.firstchoice_cs.more

import android.content.Context
import android.os.Bundle
import android.text.InputType
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
import kotlinx.android.synthetic.main.fragment_suggest_a_feature.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.helpcrunch.HelpCrunch.updateHelperCrunch
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*


class SuggestAFeature : Fragment(R.layout.fragment_suggest_a_feature) , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    private var sending = false

    interface OnFragmentInteractionListener {
        fun restoreFabState()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_suggest_feature, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }


    override fun onDetach() {
        super.onDetach()
        mListener = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mListener?.restoreFabState()
        initComponents()
    }

    private fun initComponents() {

        setUpToolbar()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@SuggestAFeature).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        toolbar.setNavigationOnClickListener {
            NavHostFragment.findNavController(this@SuggestAFeature).navigateUp()
        }

        textInput.hint = "Suggest a feature below"
        suggestionEt.inputType = InputType.TYPE_CLASS_TEXT
        suggestionEt.setText("")

        loadClientDetails()
        dismissKeyBoard()
        button.setOnClickListener {
            if (validate()) {
                saveClientDetails()
                updateHelperCrunch(false)
                sendFeatureRequest(act, suggestionEt.text.toString())
            }
        }

    }

    private fun setUpToolbar() {
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this))
        act.setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    }

    private fun sendFeatureRequest(context: Context, feature: String) {
        if (sending)
            return
        sending = true
        if(AppStatus.INTERNET_CONNECTED) {
            val pref = context.getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
            val name = pref.getString("name", "")
            val company = pref.getString("company", "")
            val email = pref.getString("email", "")
            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("featureRequests")
            val ref = myRef.push()
            val dict: MutableMap<String, Any> = HashMap()
            dict["contactCompany"] = SafetyChecks.ensureNonNullString(company)
            dict["contactEmail"] = SafetyChecks.ensureNonNullString(email)
            dict["contactName"] = SafetyChecks.ensureNonNullString(name)
            dict["feedback"] = feature
            dict["platform"] = "Android"
            //dict.put("testing",TestingConstants.SUGGESTION_TEST_ENABLED);
            ref.setValue(dict)
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
        suggestionEt.setText("")
        if(AppStatus.INTERNET_CONNECTED)
            Alerts.showAlert("Suggestion Submitted", "Thank you! Your feature suggestion will help us plan brand new features for the app", null)
        else
            Alerts.showAlert("Feature Queued", "When the internet is connected your feature request will automatically be sent", null)
    }

    private fun showError()
    {
        sending = false
        Alerts.showAlert("Error Submitting Suggestion", "Please check your internet connection and retry", null)
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
            if (suggestionEt.length() == 0) {
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
            return if (suggestionEt.isFocused) return suggestionEt else null
        }

    private fun dismissKeyBoard() {
        val focused: View? = focused
        if (focused != null) Helpers.hideKeyboard(focused)
    }
}