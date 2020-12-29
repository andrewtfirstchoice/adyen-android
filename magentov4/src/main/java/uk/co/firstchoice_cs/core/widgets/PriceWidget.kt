package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.price_widget_layout.view.*
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.helpers.CartHelper
import uk.co.firstchoice_cs.firstchoice.R
import java.text.DecimalFormat

class PriceWidget : ConstraintLayout {
    var priceIncVAT = 0.0
        set(price) {
            field = price
            val d = DecimalFormat("'£'0.00")
            priceIncVATText?.text = d.format(price)
        }
    var priceExVAT = 0.0
        set(price) {
            field = price
            val d = DecimalFormat("'£'0.00")
            priceExVATText.text = d.format(price)
        }

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    fun renderCost(priceStock: Product)
    {
        val cost = priceStock.customerCost ?: 0.0
        val extCost = CartHelper.customerCostIncVat(priceStock) ?: 0.0
        if (cost > 0.0 && extCost > 0.0) {
            visibility = View.VISIBLE
            priceIncVAT = extCost
            priceExVAT = cost
        }
    }

    private fun init(context: Context) {
        View.inflate(context, R.layout.price_widget_layout, this)
    }
}