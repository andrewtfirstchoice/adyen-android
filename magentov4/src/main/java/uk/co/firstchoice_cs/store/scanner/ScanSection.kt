package uk.co.firstchoice_cs.store.scanner

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.api.v4API.Category
import uk.co.firstchoice_cs.core.api.v4API.Image
import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*


internal class ScanSection(private val title: String, var list: List<PreviousScanList>,
                           private val itemInterface: ScanSectionItemInterface,
                           private val headerItemInterface: ScanSectionHeaderItemInterface) : Section(SectionParameters.builder()
        .itemResourceId(R.layout.scan_sectioned_list_details)
        .headerResourceId(R.layout.scan_sectioned_list_item_header)
        .build()) , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    override fun getContentItemsTotal(): Int {
        return list.size
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return ItemViewHolder(view)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemHolder = holder as ItemViewHolder
        val data =  list[position]
        itemHolder.data = data
            if (data.flash) {
                data.flash = false
                val flashOn = ObjectAnimator.ofObject(holder.itemView,
                        "backgroundColor", ArgbEvaluator(), -0x1, -0xffaa51)
                flashOn.duration = 500
                val flashOff = ObjectAnimator.ofObject(holder.itemView,
                        "backgroundColor", ArgbEvaluator(), -0xffaa51, -0x1)
                flashOff.duration = 500
                val animatorSet = AnimatorSet()
                animatorSet.play(flashOn).before(flashOff)
                animatorSet.start()
            }
            val is360 = data.image.contains("360Spin",ignoreCase = true)
            itemHolder.item.setOnClickListener {
                if (data.partnumber.equals("No Results", ignoreCase = true)) {
                    itemInterface.talkToExpert()
                } else {
                    val productDetails = Product(
                            manufacturer = data.manufacturer,
                            partNum = data.fccPartnumber,
                            partDescription= data.partname,
                            obsolete = data.obsolete,
                            stock = data.stock,
                            fccPart = data.fccPartnumber,
                            images = listOf(Image("",data.image)),
                            barcode = data.barcode,
                            categories = ArrayList<Category>(),
                            classDescription = "",
                            classId = "",
                            documents = null,
                            fitsModel = null,
                            haz_ClassNumber = "",
                            haz_GovernmentId = "",
                            haz_SubRisk = "",
                            haz_TechnicalName = "",
                            hazardous = false,
                            height = 0.0,
                            weight = 0.0,
                            length = 0.0,
                            width = 0.0,
                            isModel = 0,
                            isPart = 0,
                            leadTime = "",
                            linkedParts = ArrayList<LinkedPart>(),
                            prodCode = "",
                            preferred = false,
                            preferredFccPart =  "",
                            supersededFccPart = data.fccSuperseded,
                            preferredPartNum = "",
                            soldInMultiples = 0,
                            superseded = !data.superseded.isNullOrBlank(),
                            supersededPartNum = data.superseded)

                    itemInterface.showProductPage(productDetails)
                }
            }
            if (data.partnumber.equals("No Results", ignoreCase = true)) {
                itemHolder.binding.image.visibility = View.GONE
                itemHolder.binding.manufacturer.visibility = View.GONE
                itemHolder.binding.barcodeNumber.text = data.barcode
                itemHolder.binding.partNumber.text = data.partnumber
                itemHolder.binding.partName.text = data.partname
                itemHolder.binding.threeSixty.visibility = View.GONE
                Helpers.renderImage(itemHolder.binding.tick,R.drawable.icon_phone)
                holder.binding.stock.setTextColor(Color.BLACK)
                holder.binding.stock.text = ctx.getString(R.string.call)
            } else {
                itemHolder.binding.image.visibility = View.VISIBLE
                itemHolder.binding.manufacturer.visibility = View.VISIBLE
                itemHolder.binding.stock.visibility = View.VISIBLE
                Helpers.renderImage(itemHolder.binding.image,R.drawable.placeholder)
                itemHolder.binding.barcodeNumber.text = data.barcode
                itemHolder.binding.partNumber.text = data.partnumber
                itemHolder.binding.partName.text = data.partname
                itemHolder.binding.manufacturer.text = data.manufacturer.capitalize(Locale.ROOT)

                if(!data.superseded.isNullOrEmpty()) {
                    holder.binding.stock.text = ctx.getString(R.string.superseded_by, data.superseded)
                    holder.binding.tick.setImageResource(R.drawable.ic_baseline_cancel_24)
                    holder.binding.stock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.fcRed))
                }
                else if(data.obsolete) {
                    holder.binding.stock.text = ctx.getString(R.string.obsolete)
                    holder.binding.tick.setImageResource(R.drawable.ic_baseline_cancel_24)
                    holder.binding.stock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.fcRed))
                }

                else if(data.stock==0) {
                    holder.binding.stock.text = ctx.getString(R.string.available_on_order)
                    holder.binding.tick.setImageResource(R.drawable.icon_orders)
                    holder.binding.stock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.green))
                }
                else {
                    holder.binding.stock.text = ctx.getString(R.string.stock_text,data.stock)
                    holder.binding.tick.setImageResource(R.drawable.in_stock_tick)
                    holder.binding.stock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.green))
                }

                if (is360) itemHolder.binding.threeSixty.visibility = View.VISIBLE else itemHolder.binding.threeSixty.visibility = View.GONE
                Helpers.renderImage(itemHolder.binding.image,list[position].image)

        }
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        val headerHolder = holder as HeaderViewHolder
        headerHolder.headerBinding.title.text = title
        headerHolder.headerBinding.clearButton.setOnClickListener {
            if (title.equals(ctx.getString(R.string.previous_scans), ignoreCase = true)) {
                headerItemInterface.clearPreviouslyScanned()
            } else {
                headerItemInterface.clearRecentlyScanned()
            }
        }
    }

    internal interface ScanSectionItemInterface {
        fun talkToExpert()
        fun showProductPage(productDetails: Product)
    }

    internal interface ScanSectionHeaderItemInterface {
        fun clearPreviouslyScanned()
        fun clearRecentlyScanned()
    }
}