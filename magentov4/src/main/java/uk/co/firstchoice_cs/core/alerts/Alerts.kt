package uk.co.firstchoice_cs.core.alerts

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener

object Alerts : KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context


    @JvmStatic
    fun partNotFoundAlert(callback: AlertResponse?) {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle("Part Not Found")
        alert.setMessage("We couldn't locate that part on our store.  Would you like to speak to one of our advisors?")
        alert.setPositiveButton("Yes") { _: DialogInterface?, _: Int -> callback?.processPositive(null) }
        alert.setCancelable(false)
        alert.setNegativeButton("Close") { dialog: DialogInterface, _: Int ->
            callback?.processNegative(null)
            dialog.dismiss()
        }
        alert.show()
    }
    @JvmStatic
    fun showUpdateAvailableAlert(callback:AlertResponse) {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle("We have a new app update for you!")
        alert.setMessage("Tap below to be taken to the Google Play Store to download the latest version of the first choice app")
        alert.setPositiveButton("Update") { dialog: DialogInterface, _: Int ->
            callback.processPositive(null)
            dialog.dismiss()
        }
        alert.setCancelable(false)
        alert.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            callback.processNegative(null)
            dialog.dismiss()
        }
        alert.show()
    }
    @JvmStatic
    fun getUpdate( callback: AlertResponse?) {
        if (Helpers.isPlayStoreInstalled()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=uk.co.firstchoice_cs.firstchoice")
                intent.setPackage("com.android.vending")
                defaultCurrentActivityListener.currentActivity?.startActivity(intent)
                return
            } catch (ignored: ActivityNotFoundException) {
                callback?.processNegative(null)
            }
        } else {
            callback?.processNegative(null)
        }
    }

    @JvmStatic
    fun showAlert( title: String, message: String, callback: AlertResponse?) {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setPositiveButton("Okay") { _: DialogInterface?, _: Int -> callback?.processPositive(null) }
        alert.show()
    }

    @JvmStatic
    fun showNoInternetToast()
    {
        Toast.makeText(ctx, "Please connect to the internet", Toast.LENGTH_SHORT).show()
    }

    interface AlertResponse {
        fun processPositive(output: Any?)
        fun processNegative(output: Any?)
    }
}