package uk.co.firstchoice_cs.core.shared

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.SavePrefs
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.TESTING
import uk.co.firstchoice_cs.Settings.magentoBearerAdmin
import uk.co.firstchoice_cs.Settings.magentoCustomerPassword
import uk.co.firstchoice_cs.Settings.magentoCustomerUsername
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private lateinit var binding: FragmentSignInBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_in, container, false)
    }
    interface OnFragmentInteractionListener {
        fun processLogin()
        fun showSnackBarMessage(message: String, duration: Int)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSignInBinding.bind(view)

        App.loginState.observe(viewLifecycleOwner, {
            if (it == Settings.LoginState.LOGGED_IN_ACCOUNT)
                loginCompleted()
        })

        GlobalScope.launch(context = Dispatchers.IO) {
            magentoBearerAdmin()
        }

        if(TESTING) {
            binding.passwordEdit.setText(magentoCustomerPassword())
            binding.emailEdit.setText(magentoCustomerUsername())
        }

        binding.passwordEdit.transformationMethod = PasswordTransformationMethod()

        binding.signInButton.setOnClickListener {
            binding.signInButton.isEnabled = false
            val email =  binding.emailEdit.text.toString()
            val password =  binding.passwordEdit.text.toString()
            if (email.isEmpty() || !isValidEmail(email)) {
                listener?.showSnackBarMessage(
                    getString(R.string.valid_email),
                    Snackbar.LENGTH_SHORT
                )
            } else if (password.isEmpty()) {
                listener?.showSnackBarMessage(
                    getString(R.string.valid_password),
                    Snackbar.LENGTH_SHORT
                )
            } else {
                if(App.loginState.value != Settings.LoginState.LOGGING_IN_ACCOUNT && App.loginState.value != Settings.LoginState.LOGGING_IN_GUEST ) {
                    SavePrefs.clearUserAndLogout()
                    SavePrefs.savePassword(binding.passwordEdit.text.toString())
                    SavePrefs.saveUsername(binding.emailEdit.text.toString())
                    lifecycleScope.launch(context = Dispatchers.IO) {
                        binding.signInButton.isEnabled = false
                        listener?.processLogin()
                    }
                }
            }
        }

        binding.showPasswordMButton.setOnClickListener {
            if ( binding.showPasswordMButton.text.toString().equals(
                    getString(R.string.show_passowrd),
                    ignoreCase = true
                )) {
                binding.showPasswordMButton.text = getString(R.string.hide_password)
                binding.passwordEdit.transformationMethod = null
            } else {
                binding.passwordEdit.transformationMethod = PasswordTransformationMethod()
                binding.showPasswordMButton.text = getString(R.string.show_passowrd)
            }
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Navigation.findNavController(view).popBackStack()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        setUpToolbar()
    }


    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
    }


    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun loginCompleted() {
        GlobalScope.launch(context = Dispatchers.Main) {
            delay(500)
            Navigation.findNavController(binding.mainView).popBackStack()
        }
    }
}