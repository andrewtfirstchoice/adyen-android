package uk.co.firstchoice_cs.store.scanner

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import uk.co.firstchoice_cs.firstchoice.databinding.ScanSectionedListItemHeaderBinding

internal class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val headerBinding = ScanSectionedListItemHeaderBinding.bind(view)
    //val tvTitle: TextView = view.findViewById(R.id.title)
   // val clearButton: Button = view.findViewById(R.id.clearButton)
}