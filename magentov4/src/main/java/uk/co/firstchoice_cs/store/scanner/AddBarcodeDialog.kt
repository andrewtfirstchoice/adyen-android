package uk.co.firstchoice_cs.store.scanner

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R


interface AddBarcodeDialogCallback {
    fun onResult(barcode: String?)
}

class AddBarcodeDialog : DialogFragment() , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private var callback: AddBarcodeDialogCallback? = null

    fun setCallback(callback: AddBarcodeDialogCallback?) {
        this.callback = callback
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        val inflater = requireActivity().layoutInflater
        @SuppressLint("InflateParams") val view = inflater.inflate(R.layout.enter_barcode_dialog, null)
        builder.setView(view)
                .setPositiveButton("Done") { _: DialogInterface?, _: Int ->
                    val barcodeText = view.findViewById<EditText>(R.id.barcodeEdit)
                    val barcode = barcodeText.text.toString()
                    if (callback != null) callback?.onResult(barcode)
                }
                .setNegativeButton("Close") { dialog: DialogInterface?, _: Int -> dialog?.cancel() }
        val dlg: Dialog = builder.create()


        //customise the buttons text colour and background
        dlg.setOnShowListener { dialog: DialogInterface ->                     //
            val positiveButton = (dialog as AlertDialog)
                    .getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setBackgroundColor(Color.TRANSPARENT)
            positiveButton.isAllCaps = false
            positiveButton.textSize = 14f
            positiveButton.setTextColor(ContextCompat.getColor(ctx,R.color.fcBlue))
            val negativeButton = dialog
                    .getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setBackgroundColor(Color.TRANSPARENT)
            negativeButton.isAllCaps = false
            negativeButton.textSize = 14f
            negativeButton.setTextColor(ContextCompat.getColor(ctx,R.color.fcRed))
        }
        dlg.setCancelable(false)
        return dlg
    }
}