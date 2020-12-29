package uk.co.firstchoice_cs.store.scanner

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList
import uk.co.firstchoice_cs.firstchoice.databinding.ScanSectionedListDetailsBinding

internal class ItemViewHolder(val item: View) : RecyclerView.ViewHolder(item) {
    val binding: ScanSectionedListDetailsBinding =  ScanSectionedListDetailsBinding.bind(item)
    var data: PreviousScanList? = null
}