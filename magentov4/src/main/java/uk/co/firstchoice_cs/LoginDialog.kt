package uk.co.firstchoice_cs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.LoginProgressBinding

class LoginDialog: DialogFragment(R.layout.login_progress) {

    private lateinit var binding: LoginProgressBinding
    private var prLights: ArrayList<View?>? = ArrayList()

    companion object
    {
        fun newInstance(): LoginDialog {
            return LoginDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = LoginProgressBinding.inflate(LayoutInflater.from(requireContext()))

        return AlertDialog.Builder(requireActivity())
            .setView(binding.root)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    fun init()
    {
        prLights?.clear()
        prLights?.add(binding.s1)
        prLights?.add(binding.s2)
        prLights?.add(binding.s3)
        prLights?.add(binding.s4)
        prLights?.add(binding.s5)
        prLights?.add(binding.s6)
        prLights?.add(binding.s7)

        lifecycleScope.launch(context = Dispatchers.Main) {
            for (i in 0..6) {
                setIndicatorColours(i, MainActivity.LoginStates.IDLE)
            }
            binding.progress.progress = 0
        }
    }

    fun setStatusText(text: String) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            binding.statusText.text = text
        }
    }

    private fun setProgress(index: Int) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            binding.progress.progress = index
        }
    }

    fun setIndicators(i: Int, visibility: Int) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            prLights?.get(i)?.visibility = visibility
        }
    }

    fun setIndicatorColours(index: Int, colour: MainActivity.LoginStates) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            prLights?.get(index)?.setBackgroundColor(
                ContextCompat.getColor(
                    App.instance,
                    colour.colour
                )
            )
            if (colour != MainActivity.LoginStates.IDLE)
                setProgress(index)
        }
    }

    fun updateDialogStatus(status: Boolean, index: Int)
    {
        if (status)
            setIndicatorColours(index, MainActivity.LoginStates.SUCCESS)
        else
            setIndicatorColours(index, MainActivity.LoginStates.FAILED)
    }


    fun setMode() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            val visibility = if(App.loginState.value== Settings.LoginState.LOGGING_IN_GUEST) View.GONE else View.VISIBLE
            for (i in 0..3) {
                setIndicators( i,visibility)
            }
        }
    }
}