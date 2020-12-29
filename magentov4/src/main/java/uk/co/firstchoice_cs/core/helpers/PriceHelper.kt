package uk.co.firstchoice_cs.core.helpers

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.textview.MaterialTextView
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R

object PriceHelper : KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val fcRed = ContextCompat.getColor(ctx, R.color.fcRed)
    private val fcGreen = ContextCompat.getColor(ctx, R.color.green)

    fun renderDiscount(priceStock: Product?,oldPrice: TextView?, discountChip: Chip?)
    {
        discountChip?.visibility = View.INVISIBLE
        oldPrice?.visibility = View.INVISIBLE
        oldPrice?.text = CartHelper.getCurrencyFormatter().format(priceStock?.customerCost)
        val discount = priceStock?.discountPercentage?.toInt()
        discountChip?.text = "${discount}%"
        if (discount != null && discount > 0) {
            discountChip?.visibility = View.VISIBLE
            oldPrice?.visibility = View.VISIBLE
        }
    }

    fun render360(threeSixtyIcon:ImageView,is360:Boolean)
    {
        if (is360)
            threeSixtyIcon.visibility = View.VISIBLE
        else
            threeSixtyIcon.visibility = View.GONE
    }



    fun renderPrices(inStock: TextView?, tick: ImageView?, priceStock: Product?, priceStatus:Settings.PriceStatus?):Boolean
    {
        var canBeAddedToBasket = false
        when {
            priceStatus == Settings.PriceStatus.FAILED_GETTING_PRICES -> {
                inStock?.text = ctx.getString(R.string.error_loading_product)
                tick?.setImageResource(R.drawable.ic_baseline_cancel_24)
                inStock?.setTextColor(fcRed)
            }
            !priceStock?.supersededBy.isNullOrEmpty() -> {
                inStock?.text = ctx.getString(R.string.superseded_by, priceStock?.supersededBy)
                tick?.setImageResource(R.drawable.ic_baseline_cancel_24)
                inStock?.setTextColor(fcRed)
            }
           priceStock?.obsolete ?: false -> {
                inStock?.text = ctx.getString(R.string.obsolete)
                tick?.setImageResource(R.drawable.ic_baseline_cancel_24)
                inStock?.setTextColor(fcRed)
            }
            priceStock?.customerExtendedCost == 0.0 -> {
                inStock?.text = ctx.getString(R.string.on_application)
                tick?.setImageResource(R.drawable.ic_baseline_cancel_24)
                inStock?.setTextColor(fcRed)
            }
            priceStock!=null && priceStock.stock == 0 -> {
                canBeAddedToBasket = true
                val suffix = if (priceStock.avgLeadTime == 1.0) "" else "S"
                inStock?.text = ctx.getString(R.string.delivery_estimate, priceStock.avgLeadTime?.toInt() ?: 0, suffix)
                tick?.setImageResource(R.drawable.icon_truck)
                inStock?.setTextColor(fcGreen)
            }
            priceStock!=null && priceStock.stock!! > 0 -> {
                canBeAddedToBasket = true
                inStock?.text = ctx.getString(R.string.stock_text, priceStock.stock)
                tick?.setImageResource(R.drawable.in_stock_tick)
                inStock?.setTextColor(fcGreen)
            }
            else -> {
                inStock?.text = ""
                tick?.setImageResource(R.drawable.invisible_resource)
            }
        }
        return canBeAddedToBasket
    }
}


