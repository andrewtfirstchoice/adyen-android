package uk.co.firstchoice_cs.basket

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_delivery_methods.*
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.api.customerAPI.ShipVia
import uk.co.firstchoice_cs.core.helpers.CartHelper
import uk.co.firstchoice_cs.core.widgets.PriceWidget
import uk.co.firstchoice_cs.firstchoice.R

class DeliveryMethodsFragment : Fragment(R.layout.fragment_delivery_methods) {
    private val adapter = MyAdapter()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this.activity)
        recycler.layoutManager = mLayoutManager
        recycler.itemAnimator = DefaultItemAnimator()
        recycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        recycler.adapter = adapter
        adapter.notifyDataSetChanged()

        setUpToolbar()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            NavHostFragment.findNavController(
                this@DeliveryMethodsFragment).navigateUp()
        }
    }

    private inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.delivery_method_list_item, parent, false)
            return ViewHolder(v)
        }

        fun setSelected(shippingMethod: ShipVia)
        {
            val sv = App.customer?.shipVias
            if (sv != null) {
                for(ship in sv)
                {
                    ship.selected = ship == shippingMethod
                }
            }
            notifyDataSetChanged()
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val shippingMethod = App.customer?.shipVias?.get(position)
            if(shippingMethod!=null) {

                holder.itemView.setOnClickListener {
                    setSelected(shippingMethod)
                }
                holder.trailing.setOnClickListener {
                    setSelected(shippingMethod)
                }
                holder.trailing.visibility = if (shippingMethod.selected) View.VISIBLE else View.INVISIBLE
                holder.deliveryTypeText.text = shippingMethod.carrier
                holder.descriptionText.text = shippingMethod.description
                holder.priceWidget.priceExVAT = shippingMethod.miscInfo?.get(0)?.MiscAmt?:0.0
                holder.priceWidget.priceIncVAT = CartHelper.priceWithTax(shippingMethod.miscInfo?.get(0)?.MiscAmt
                    ?: 0.0)
                holder.trailing.setOnClickListener { Toast.makeText(this@DeliveryMethodsFragment.context, "clicked", Toast.LENGTH_LONG).show() }
            }
        }

        override fun getItemCount(): Int {
            return  App.customer?.shipVias?.size?:0
        }


        private inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val trailing: ImageView = itemView.findViewById(R.id.check)
            val deliveryTypeText: TextView = itemView.findViewById(R.id.deliveryTypeText)
            val descriptionText: TextView = itemView.findViewById(R.id.descriptionText)
            val priceWidget: PriceWidget = itemView.findViewById(R.id.priceWidget)
        }
    }
}