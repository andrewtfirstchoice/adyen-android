package uk.co.firstchoice_cs.more

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_account_order_details.*
import kotlinx.android.synthetic.main.order_details_item.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.api.customerAPI.*
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.LoginHelper
import uk.co.firstchoice_cs.firstchoice.R

class AccountOrderDetailsFragment : Fragment(R.layout.fragment_account_order_details) {


    private var products: List<Product>? = null
    private var lines: List<LineX>? = null
    private var orderDetails: OrderDetails? = null
    private var order: Order? = null
    private val ordersAdapter = LinesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            order = Order(it.getString("eccOrderNum"), it.getString("estimatedDelivery"), it.getInt("lines"), it.getString("orderDate"), it.getInt("orderNum"), it.getDouble("orderTotal"), it.getString("poNum"), it.getString("status"))
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hideKeyboard()
        setupRecycler()
        initData()
        setUpToolbar()


        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@AccountOrderDetailsFragment).navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun initData() {
        toolbar.subtitle = "#${order?.orderNum}"
        GlobalScope.launch(context = Dispatchers.IO) {
            if (LoginHelper.networkConditionsOKForLogin()) {
                val magentoCustomerId = App.magentoCustomer?.id
                val orderNumber = order?.orderNum
                if (magentoCustomerId != null && orderNumber!=null) {
                    orderDetails = CustomerAPICalls.getOrderDetails(magentoCustomerId, orderNumber)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main)
                    {
                        val details = orderDetails?.orderDetails?.get(0)
                        if (details!=null) {
                            lines = details.lines
                            render(details)
                            loadProductsFromLines()
                        }
                    }
                }
            }
        }
    }

    private fun getProduct(sku:String):Product?
    {
        val allProducts = products
        if (allProducts != null) {
            for (product in allProducts) {
                if(product.fccPart == sku)
                    return product
            }
        }
        return null
    }

    private fun loadProductsFromLines() {
        var commaSeparated = ""
        val allLines = lines
        if(allLines!=null) {
            for (line in allLines) {
                commaSeparated = commaSeparated.plus(line.partNum + ",")
            }
            if(commaSeparated.isBlank())
                return
            else
                commaSeparated = commaSeparated.substring(0,commaSeparated.length-1)
        }

        if(AppStatus.INTERNET_CONNECTED) {
            pbDownload.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {

                    val res = V4APICalls.product(commaSeparated,0,30)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        if (pbDownload != null)
                            pbDownload.visibility = View.GONE
                        if(res!=null) {
                            products = res.product
                            ordersAdapter.notifyDataSetChanged()
                        }
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {

                        if (pbDownload != null)
                            pbDownload.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getShippingType(orderDetail:OrderDetail): String?
    {
        val via = App.customer?.shipVias
        if (via != null) {
            for(item in via) {
                if(item.shipViaCode==orderDetail.shipViaCode)
                    return item.carrier
            }
        }
        return "Unknown"
    }

    private fun setupRecycler()
    {
        val layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = ordersAdapter
        val itemDecor = DividerItemDecoration(this.context, ClipDrawable.HORIZONTAL)
        recycler.addItemDecoration(itemDecor)

        recycler.layoutManager = layoutManager
    }

    inner class LinesAdapter() : RecyclerView.Adapter<LinesAdapter.ViewHolder>()
    {
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = lines?.get(position)
            if(item!=null) {
                val product = item.partNum?.let { getProduct(it) }
                val url = product?.images?.get(0)?.url
                Helpers.renderImage(holder.image,url)

                holder.manufacturerText.text = product?.manufacturer
                holder.partIDText.text = item.partNum
                holder.descriptionText.text = item.description
                holder.qtyText.text =  item.qty?.toInt().toString()
                holder.priceText.text = "£${item.customerPrice.toString()}"
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.order_details_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = lines?.size?:0

        inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val image: ImageView = mView.image
            val manufacturerText: TextView = mView.manufacturerText
            val partIDText: TextView = mView.partIDText
            val descriptionText: TextView = mView.descriptionText
            val qtyText: TextView =  mView.qtyText
            val priceText: TextView =  mView.priceText
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    private fun render(orderDetail: OrderDetail)
    {
        reorderButton.setOnClickListener {
            Toast.makeText(requireContext(),"Reorder Clicked", Toast.LENGTH_SHORT).show()
        }

        needHelpButton.setOnClickListener {
            Toast.makeText(requireContext(), "Need Help Clicked", Toast.LENGTH_SHORT).show()
        }

        viewShipmentsButton.setOnClickListener {
            Toast.makeText(requireContext(), "View shipments Clicked", Toast.LENGTH_SHORT).show()
        }



        //order status
        orderNumberTxt.text = orderDetail.orderNum.toString()
        statusTxt.text = order?.status
        dateTxt.text = order?.orderDate
        numItemsTxt.text = order?.lines.toString()
        //delivery methods
        shipping_method.text = getShippingType(orderDetail)
        val useOTS:Boolean = orderDetail.useOTS?:false
        if(useOTS) {
            deliverToName.text = orderDetail.oneTimeShipAddress?.get(0)?.otsName
            address1.text = orderDetail.oneTimeShipAddress?.get(0)?.otsAddress1
            address2.text = orderDetail.oneTimeShipAddress?.get(0)?.otsAddress2
            postCode.text = orderDetail.oneTimeShipAddress?.get(0)?.otsZip
            telephoneNumber.text = orderDetail.oneTimeShipAddress?.get(0)?.OTSPhoneNum
        }
        else
        {
            deliverToName.text = orderDetail.shipToAddress?.get(0)?.name
            address1.text = orderDetail.shipToAddress?.get(0)?.address1
            address2.text = orderDetail.shipToAddress?.get(0)?.address2
            postCode.text = orderDetail.shipToAddress?.get(0)?.zip
            telephoneNumber.text = orderDetail.shipToAddress?.get(0)?.phoneNum
        }

        //payment methods
        Helpers.renderImage(paymentImage,R.drawable.paypal)

        paymentMethod.text = "PayPal"
        if(useOTS) {
            paymentFromName.text = orderDetail.oneTimeShipAddress?.get(0)?.otsName
            paymentAddress1.text = orderDetail.oneTimeShipAddress?.get(0)?.otsAddress1
            paymentAddress2.text = orderDetail.oneTimeShipAddress?.get(0)?.otsAddress2
            paymentPostCode.text = orderDetail.oneTimeShipAddress?.get(0)?.otsZip
            paymentTelephone.text = orderDetail.oneTimeShipAddress?.get(0)?.OTSPhoneNum
        }
        else
        {
            paymentFromName.text = orderDetail.shipToAddress?.get(0)?.name
            paymentAddress1.text = orderDetail.shipToAddress?.get(0)?.address1
            paymentAddress2.text = orderDetail.shipToAddress?.get(0)?.address2
            paymentPostCode.text = orderDetail.shipToAddress?.get(0)?.zip
            paymentTelephone.text = orderDetail.shipToAddress?.get(0)?.phoneNum
        }
        //totals
        subtotalValueText.text = "£${orderDetail.lineTotal.toString()}"
        shippingValueText.text = "£${orderDetail.miscCharges.toString()}"
        taxValueText.text = "£${orderDetail.vatAmount.toString()}"
        totalValueText.text = "£${orderDetail.orderTotal.toString()}"

    }


    private fun hideKeyboard() {
        val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(view?.findFocus()?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }
}
