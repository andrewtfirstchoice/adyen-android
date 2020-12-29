package uk.co.firstchoice_cs.store.bottom_sheets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.download_bottom_sheet.view.*
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.DownloadBottomSheetBinding

class DownloadBottomSheet : ConstraintLayout {
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    lateinit var binding:DownloadBottomSheetBinding

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        val view = View.inflate(context, R.layout.download_bottom_sheet,this)
        binding = DownloadBottomSheetBinding.bind(view)
    }

    fun setSheetBehavior(sheetBehavior: BottomSheetBehavior<*>?) {
        this.sheetBehavior = sheetBehavior
    }

    fun expand() {
        wrapper.requestFocus()
        sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun hide() {
        wrapper.requestFocus()
        sheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    fun setDocumentDownloadProgress(docEntry: DocumentEntry) {
        val bytesDownloaded = Helpers.bytesIntoHumanReadable(docEntry.bytesDownloaded)
        val totalBytes = Helpers.bytesIntoHumanReadable(docEntry.totalBytes)
        binding.progressTxt.text = context.getString(R.string.downloaded_message,bytesDownloaded,totalBytes)
        binding.contentLoadingBar.max = docEntry.totalBytes.toInt()
        binding.contentLoadingBar.progress = docEntry.bytesDownloaded.toInt()
        if (docEntry.bytesDownloaded - docEntry.lastDownloadedBytes > 100) {
            docEntry.lastDownloadedBytes = docEntry.bytesDownloaded
        }
    }

    fun setComplete() {
        binding.statusTxt.text = context.getString(R.string.generated)
        binding.progressTxt.text = context.getString(R.string.download_complete)
    }

    fun setError() { 
        binding.statusTxt.text = context.getString(R.string.failed_to_download)
        binding.progressTxt.text = context.getString(R.string.error_downloading)
    }

    fun startDownload(docEntry: DocumentEntry) {
        binding.contentLoadingBar.max = docEntry.totalBytes.toInt()
        binding.statusTxt.text = context.getString(R.string.downloading)
        docEntry.downloading = true
    }
}