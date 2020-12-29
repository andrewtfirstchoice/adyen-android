package uk.co.firstchoice_cs.more

import android.os.Bundle
import android.os.Handler
import androidx.navigation.fragment.findNavController
import uk.co.firstchoice_cs.core.api.customerAPI.Order
import uk.co.firstchoice_cs.core.shared.AccountOrdersFragment
import uk.co.firstchoice_cs.firstchoice.R


class AccountOrdersFrag : AccountOrdersFragment() {

    override fun openDetails(order:Order) {
        hideKeyboard()
        Handler().postDelayed({
            val bundle = Bundle()

            order.eccOrderNum?.let { bundle.putString("eccOrderNum", it) }
            order.estimatedDelivery?.let { bundle.putString("estimatedDelivery", it) }
            order.lines?.let { bundle.putInt("lines", it) }
            order.orderDate?.let { bundle.putString("orderDate", it) }
            order.orderNum?.let { bundle.putInt("orderNum", it) }
            order.orderTotal?.let { bundle.putDouble("orderTotal", it) }
            order.poNum?.let { bundle.putString("poNum", it) }
            order.status?.let { bundle.putString("status", it) }

              findNavController().navigate(R.id.action_accountOrdersFragment_to_accountOrderDetailsFragment, bundle, null, null)
        }, 100)
    }
}