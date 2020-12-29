package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.text.TextUtils
import android.util.Patterns
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import kotlinx.android.synthetic.main.fragment_contact_details.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.helpcrunch.HelpCrunch.updateHelperCrunch
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R


class ContactDetailsFragment : Fragment(R.layout.fragment_contact_details) , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    private var updateHelpCrunch = true
    interface OnFragmentInteractionListener {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }


    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments
        if(args!=null)
            updateHelpCrunch = args.getBoolean("update",true)

        initComponents()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_contact_details, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }


    private fun hasClientDetails(): Boolean {
        val pref = ctx.getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        return SafetyChecks.ensureNonNullString(pref.getString("name", "")).isNotBlank()

    }

    private fun initComponents() {
        setUpToolbar()

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)


        toolbar.setNavigationOnClickListener{
           goBack()
        }

        textInput.hint = resources.getString(R.string.contact_phone)
        partsIDPhoneEdit.hint = resources.getString(R.string.enter_your_phone_number)
        partsIDPhoneEdit.inputType = InputType.TYPE_CLASS_PHONE

        if (hasClientDetails()) button.text = "UPDATE DETAILS" else button.text = "SAVE DETAILS"

        questionButton.setOnClickListener { Toast.makeText(context, R.string.postcode_reason, Toast.LENGTH_LONG).show() }
        loadClientDetails()
        dismissKeyBoard()
        button.setOnClickListener {
            if (validate()) {
                saveClientDetails()
                Toast.makeText(context,"Saved",Toast.LENGTH_SHORT).show()
                if(updateHelpCrunch)
                   updateHelperCrunch(true)
                Handler().postDelayed({
                    if(toolbar!=null)
                        goBack()
                }, Toast.LENGTH_SHORT.toLong())
            }
        }
    }

    private fun goBack()
    {
        NavHostFragment.findNavController(this@ContactDetailsFragment).navigateUp()
    }

    private fun setUpToolbar() {
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this))
        act.setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    }

    private fun loadClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        partsIDNameEdit.setText(pref.getString("name", ""))
        partsIDCompanyEdit.setText(pref.getString("company", ""))
        partsIDEmailEdit.setText(pref.getString("email", ""))
        partsIDPostCode.setText(pref.getString("postcode", ""))
        partsIDPhoneEdit.setText(pref.getString("phone", ""))
    }

    private fun saveClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString("name", SafetyChecks.safeString(partsIDNameEdit))
        editor.putString("company", SafetyChecks.safeString(partsIDCompanyEdit))
        editor.putString("email", SafetyChecks.safeString(partsIDEmailEdit))
        editor.putString("postcode", SafetyChecks.safeString(partsIDPostCode))
        editor.putString("phone", SafetyChecks.safeString(partsIDPhoneEdit))
        editor.apply()
    }

    private fun validate(): Boolean {
        val email = SafetyChecks.ensureNonNullString(partsIDEmailEdit)
        val postcode = SafetyChecks.ensureNonNullString(partsIDPostCode)
        if (SafetyChecks.ensureNonNullString(partsIDNameEdit).isEmpty()) {
            Alerts.showAlert("We need more information", "Please provide your name", null)
            return false
        } else if (SafetyChecks.ensureNonNullString(partsIDCompanyEdit).isEmpty()) {
            Alerts.showAlert("We need more information", "Please provide your company", null)
            return false
        } else if (email.isEmpty()) {
            Alerts.showAlert("We need more information", "Please provide your email", null)
            return false
        } else if (!isValidEmail(email)) {
            Alerts.showAlert("There was an error with your details", "Please provide a valid email address", null)
            return false
        } else if (postcode.isEmpty()) {
            Alerts.showAlert("We need more information", "Please provide your company postcode", null)
            return false
        }

        return true
    }

    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private val focused: EditText?
        get() {
            if (partsIDNameEdit.isFocused) return partsIDNameEdit
            if (partsIDCompanyEdit.isFocused) return partsIDCompanyEdit
            if (partsIDEmailEdit.isFocused) return partsIDNameEdit
            if (partsIDPhoneEdit.isFocused) return partsIDPhoneEdit
            return if (partsIDPostCode.isFocused) partsIDPostCode else null
        }

    private fun dismissKeyBoard() {
        val focused: View? = focused
        if (focused != null) Helpers.hideKeyboard(focused)
    }
}