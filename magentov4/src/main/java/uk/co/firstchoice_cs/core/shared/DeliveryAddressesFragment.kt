package uk.co.firstchoice_cs.core.shared

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.*
import com.google.android.material.button.MaterialButton
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.api.AddressManager
import uk.co.firstchoice_cs.core.helpers.SwipeCallback
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentDeliveryAddressesBinding

open class DeliveryAddressesFragment : Fragment(R.layout.fragment_delivery_addresses) {

    private val adapter = MyAdapter()
    private var mode: AddressManager.AddressTypes = AddressManager.AddressTypes.BILLING
    private var addresses: ArrayList<AddressManager.Address> = ArrayList()
    lateinit var binding:FragmentDeliveryAddressesBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val arg = arguments
        if(arg!=null) {
            val m = arg.getInt("mode")
            mode = if (m == 0) {
                AddressManager.AddressTypes.BILLING
            } else {
                AddressManager.AddressTypes.SHIPPING
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_delivery_addresses, menu)
        DrawableCompat.setTint(menu.getItem(0).icon, ContextCompat.getColor(activity as AppCompatActivity, R.color.white))
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add) {
            goToAddNewAddress()
        }
        return super.onOptionsItemSelected(item)
    }

    protected open fun goToAddNewAddress() {


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentDeliveryAddressesBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(activity)
        binding.recycler.itemAnimator = DefaultItemAnimator()
        binding.recycler.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
        binding.recycler.adapter = adapter
        setupSwipe()
        setUpToolbar()
        renderData()
        if(mode==AddressManager.AddressTypes.BILLING)
            binding.toolbar.title = getString(R.string.select_billing_address)
        else
            binding.toolbar.title = getString(R.string.select_delivery_address)
    }

    private fun renderData()
    {
        addresses.addAll(App.addresses.mergedAddresses)
        adapter.notifyDataSetChanged()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            NavHostFragment.findNavController(this@DeliveryAddressesFragment).navigateUp()
        }
    }

    private fun setupSwipe() {
        val swipeDeleteCallback = SwipeDeleteCallback()
        val helper = ItemTouchHelper(swipeDeleteCallback)
        helper.attachToRecyclerView(binding.recycler)
    }

    internal inner class MyAdapter : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

        fun select(address: AddressManager.Address) {
            if(mode==AddressManager.AddressTypes.BILLING) {
                App.addresses.setSelectedBillingAddress(address)
            }
            else
            {
                App.addresses.setSelectedShippingAddress(address)
            }
            notifyDataSetChanged()
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.delivery_address_list_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val address = addresses[position]

            holder.itemView.setOnClickListener {
                select(address)
            }

            holder.trailing.setOnClickListener {
                select(address)
            }

            val isDefault: Boolean

            if(mode==AddressManager.AddressTypes.BILLING) {
                holder.trailing.visibility = if (address.selected_billing) View.VISIBLE else View.INVISIBLE
                isDefault = address.default_billing?:false
            }
            else
            {
                holder.trailing.visibility = if (address.selected_shipping) View.VISIBLE else View.INVISIBLE
                isDefault = address.default_shipping?:false
            }
            holder.defaultAddressButton.visibility = if (isDefault) View.VISIBLE else View.INVISIBLE

            holder.title.text = getString(R.string.full_name,address.firstname,address.lastname)
            holder.address1.text = address.company
            if(!address.street.isNullOrEmpty())
                holder.address1.text = address.street[0]
            holder.address1.text = ""
            holder.address2.text = address.city
            holder.postcode.text = address.postcode
            holder.contactNumber.text = address.telephone
        }

        override fun getItemCount(): Int {
            return addresses.size
        }

        fun setDefault(address: AddressManager.Address) {
            if(mode==AddressManager.AddressTypes.BILLING) {
                App.addresses.setDefaultBillingAddress(address)
            }
            else
            {
                App.addresses.setDefaultDeliveryAddress(address)
            }
        }

        inner class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val defaultAddressButton: MaterialButton = itemView.findViewById(R.id.default_address_button)
            val trailing: ImageView = itemView.findViewById(R.id.check)
            val contactNumber: TextView = itemView.findViewById(R.id.contactNumber)
            val postcode: TextView = itemView.findViewById(R.id.postcode)
            val address2: TextView = itemView.findViewById(R.id.address2)
            val address1: TextView = itemView.findViewById(R.id.address1)
            val title: TextView = itemView.findViewById(R.id.title)

        }
    }


    inner class SwipeDeleteCallback: SwipeCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            addresses.removeAt(position)
            adapter.notifyDataSetChanged()
        }
    }
}