package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.graphics.drawable.ClipDrawable.HORIZONTAL
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.account_orders_fragment.*
import kotlinx.android.synthetic.main.simple_order_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.customerAPI.Order
import uk.co.firstchoice_cs.core.helpers.LoginHelper
import uk.co.firstchoice_cs.firstchoice.R


open class AccountOrdersFragment : Fragment(R.layout.account_orders_fragment) ,SearchView.OnQueryTextListener{
    private var accountOrdersItems:ArrayList<Order> = ArrayList()
    private var filteredOrderItems:ArrayList<Order> = ArrayList()
    private val ordersAdapter = OrdersAdapter()
    private lateinit var searchView: SearchView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.accounts_order,menu)
        val item: MenuItem = menu.findItem(R.id.action_filter)
        searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        toolbar.setNavigationOnClickListener {
            NavHostFragment.findNavController(this@AccountOrdersFragment).navigateUp()
        }

        setupRecycler()
        initData()
        setUpToolbar()

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@AccountOrdersFragment).navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        super.onViewCreated(view, savedInstanceState)
    }


    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initData() {
        GlobalScope.launch(context = Dispatchers.IO) {
            if (LoginHelper.networkConditionsOKForLogin()) {
                val magentoCustomerId = App.magentoCustomer?.id
                if (magentoCustomerId != null) {
                    val orders = CustomerAPICalls.getOrders(magentoCustomerId)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main)
                    {
                        if (orders != null && !orders.orders.isNullOrEmpty()) {
                            accountOrdersItems.clear()
                            accountOrdersItems.addAll(orders.orders)
                            val sz = accountOrdersItems.size
                            toolbar.subtitle = "$sz orders placed"
                            filter("")
                        }
                    }
                }
            }
        }
    }

    fun filter(filter:String)
    {
        filteredOrderItems.clear()
        if(filter.isNotEmpty()) {
            for (order in this.accountOrdersItems) {
                if (order.orderNum.toString().contains(filter))
                    filteredOrderItems.add(order)
            }
        }
        else
        {
            filteredOrderItems.addAll(accountOrdersItems)
        }
        this.ordersAdapter.notifyDataSetChanged()
    }

    fun hideKeyboard() {
        if(::searchView.isInitialized) {
            val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(searchView.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
        }
    }

    private fun setupRecycler()
    {
        val layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = ordersAdapter
        val itemDecor = DividerItemDecoration(this.context, HORIZONTAL)
        recycler.addItemDecoration(itemDecor)

        recycler.layoutManager = layoutManager
    }

    open fun openDetails(order:Order) {

    }

    inner class OrdersAdapter() : RecyclerView.Adapter<OrdersAdapter.ViewHolder>()
    {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredOrderItems[position]
            holder.orderNumberTxt.text = item.orderNum.toString()
            holder.statusTxt.text = item.status
            holder.dateTxt.text = item.orderDate
            holder.numItemsTxt.text = accountOrdersItems.size.toString()
            holder.itemView.setOnClickListener {
                val order = item.orderNum
                if (order != null) {
                    openDetails(item)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.simple_order_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = filteredOrderItems.size

        inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
             val orderNumberTxt: TextView = mView.orderNumberTxt
             val statusTxt: TextView = mView.statusTxt
             val dateTxt: TextView = mView.dateTxt
             val numItemsTxt: TextView = mView.numItemsTxt
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if(::searchView.isInitialized){
            if (newText != null) {
                filter(newText.trim())
            }
        }
        return false
    }
}