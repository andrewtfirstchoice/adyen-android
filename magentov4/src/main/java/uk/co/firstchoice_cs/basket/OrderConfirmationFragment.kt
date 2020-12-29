package uk.co.firstchoice_cs.basket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import uk.co.firstchoice_cs.core.api.magentoAPI.OrderResult
import uk.co.firstchoice_cs.core.helpers.CartHelper
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.OrderConfirmationBinding
import uk.co.firstchoice_cs.firstchoice.databinding.OrderConfirmationListItemBinding

class OrderConfirmationFragment : Fragment(R.layout.order_confirmation) {

    var orderResult:OrderResult? = null
    lateinit var binding:OrderConfirmationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)


        val args = arguments
        val res = args?.getString("order", "")
        val gson = Gson()
        orderResult = gson.fromJson(res, OrderResult::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = OrderConfirmationBinding.bind(view)

        setUpToolbar()
        setupRecycler()
        setupBackButton()

        renderOrder()
    }

    private fun setupRecycler() {
        binding.recycler.layoutManager =
            LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        binding.recycler.addItemDecoration(
            DividerItemDecoration(
                activity,
                LinearLayoutManager.VERTICAL
            )
        )
        binding.recycler.adapter = RecyclerViewAdapter()
    }

    inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.order_confirmation_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val items = orderResult?.items
            if (items != null) {
                val item = items[position]
                holder.binding.priceWidget.visibility = View.VISIBLE
                holder.binding.manufacturerText.text = item.name
                holder.binding.partIDText.text = item.sku
                holder.binding.qtyText.text = getString(R.string.qty, item.qty_ordered)
                holder.binding.priceWidget.priceExVAT = item.base_price
                holder.binding.priceWidget.priceIncVAT = item.row_total_incl_tax
            }
        }


        override fun getItemCount(): Int {
            return  orderResult?.items?.size?:0
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            val binding: OrderConfirmationListItemBinding = OrderConfirmationListItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }
    private fun close() {
        Helpers.hideKeyboard(view)
        NavHostFragment.findNavController(this@OrderConfirmationFragment).navigateUp()
    }


    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            close()
        }
    }

    private fun setupBackButton() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }


    fun renderOrder()
    {
        binding.subTotalValueText.text =  CartHelper.getCurrencyFormatter().format(orderResult?.subtotal)
        binding.shippingValueText.text =  CartHelper.getCurrencyFormatter().format(orderResult?.shipping_amount)
        binding.totalValueText.text =  CartHelper.getCurrencyFormatter().format(orderResult?.base_grand_total)
        binding.vatValueText.text =  CartHelper.getCurrencyFormatter().format(orderResult?.tax_amount)
        binding.orderNumberText.text = orderResult?.increment_id.toString()
    }
}