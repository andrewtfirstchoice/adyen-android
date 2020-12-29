package uk.co.firstchoice_cs.basket

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.database.cart.CartItem
import uk.co.firstchoice_cs.core.database.cart.CartViewModel
import uk.co.firstchoice_cs.core.helpers.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.BasketItemBinding
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentBasketBinding
import java.util.*

class BasketFragment : Fragment(R.layout.fragment_basket), KoinComponent {
    private lateinit var cartViewModel: CartViewModel
    private var cartItems: List<CartItem>? = null
    private var basketQueue: Queue<CartItem> = LinkedList()
    private var mListener: OnFragmentInteractionListener? = null
    lateinit var binding:FragmentBasketBinding

    private var checkingOut: Boolean = false
    private var basketItemsValid: Boolean = false
    private var totalsValid: Boolean = true
    private var basketItemsFailureTestScenario: Boolean = false
    private var basketPricesFailureTestScenario: Boolean = false
    private var savedPrice: Double = 0.0

    override fun onResume() {
        super.onResume()
        //when we get a result from payments take use back to checkout
        if(App.instance.awaitingPaymentResult)
        {
            goToCheckout()
            return
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentBasketBinding.bind(view)
        binding.footer.binding.button.isEnabled = false
        setUpToolbar()
        setupRecycler()
        setupBackButton()
        renderToolbar()
        binding.footer.visibility = View.GONE

        binding.footer.binding.button.setOnClickListener {
            binding.footer.binding.button.isEnabled = false
            checkingOut = true
            basketPricesFailureTestScenario = false
            basketItemsFailureTestScenario = false
            refreshBasket()
        }

        binding.emptyLayout.root.visibility = View.GONE
        binding.emptyLayout.emptyButton.visibility = View.GONE
        binding.emptyLayout.emptyImage.setImageResource(R.drawable.icon_basket)
        binding.emptyLayout.emptyTitle.text = getString(R.string.basket_items)
        binding.emptyLayout.emptyDescription.text = getString(R.string.basket_is_empty)

        cartViewModel = ViewModelProvider(requireActivity()).get(CartViewModel::class.java)
        cartViewModel.allCartItems.observe(requireActivity(), {
            cartItems = it
            renderToolbar()
            if(cartItems.isNullOrEmpty()) {
                binding.emptyLayout.root.visibility = View.VISIBLE
                binding.footer.visibility = View.GONE
            }
            else {
                binding.footer.visibility = View.VISIBLE
                binding.emptyLayout.root.visibility = View.GONE
            }

            refreshBasket()
        })
    }

    private fun refreshBasket()
    {
        binding.footer.binding.button.isEnabled = false
        basketQueue = LinkedList(cartItems?.toMutableList())
        next() //this will start polling to get new prices
        refreshAdapter() //this will default render the items
    }


    private fun showPricesChangedAlert()
    {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("Basket Changed")
            .setMessage("Some of the prices have been updated since being added to the basket.  Please check and confirm before moving to checkout")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showItemsInvalidAlert()
    {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("Items Unavailable")
            .setMessage("Some of the items in your basket are no longer available and will need to be removed from your basket for you to checkout.")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showInvalidAlert()
    {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("Items Unavailable")
            .setMessage("Some of the items in your basket are no longer available and will need to be removed from your basket for you to checkout.  Also some of the prices have been updated since being added to the basket")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }



    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        setFragmentResultListener("checkoutRequest") { _, bundle ->
            val result = bundle.getString("checkoutResult")
            if (result != null) {
                if(result.isNotBlank())
                {
                    bundle.putString("order", result)
                    NavHostFragment.findNavController(this@BasketFragment)
                        .navigate(R.id.action_basketFragment_to_orderConfirmationFragment,bundle)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.basket_menu, menu)

        val clearItem = menu.findItem(R.id.action_clear)
        clearItem.setOnMenuItemClickListener {
            deleteAll()
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun goToCheckout() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            NavHostFragment.findNavController(this@BasketFragment)
                .navigate(R.id.action_basket_fragment_to_checkoutFragment, null, null, null)
        }
    }


    private fun refreshAdapter() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            basketItemsValid = true
            binding.recycler.adapter?.notifyDataSetChanged()
            binding.footer.binding.button.requestFocus()
            renderFooter()
        }
    }

    private fun deleteItem(cartItem: CartItem?) {
        cartItem?.let { cartViewModel.delete(it) }
    }

    private fun deleteAll(): Boolean {
        cartViewModel.deleteAll()
        return true
    }

    private fun updateQty(cartItem: CartItem?) {
        if (cartItem != null) {
            cartItem.qty = cartItem.tempQty
            cartViewModel.insert(cartItem)
        }
    }

    private fun renderToolbar() {
        binding.toolbar.subtitle = CartHelper.numberOfBasketItems(cartItems).toString() + ""
    }

    private fun refreshItemStatus(cartItem: CartItem?) {
        val pos = cartItem?.let { CartHelper.getPosition(updatedItem = it, items = cartItems) }
        if (cartItem != null && pos != null) {
            binding.recycler.adapter?.notifyItemChanged(pos)
        }
    }

    private fun getPriceStockEnquiry(item: CartItem?, qty: Int) {

        val sku = item?.fccPart
        if(sku!=null) {
            var price = App.globalData.getPriceStockFromMap(sku, qty)
            if (price != null) {
                item.priceStatus = Settings.PriceStatus.SUCCESS_GETTING_PRICES
                refreshItemStatus(item)
                next()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        withContext(Dispatchers.Main) {
                            item.priceStatus = Settings.PriceStatus.GETTING_PRICES
                            refreshItemStatus(item)
                        }
                        price = CustomerAPICalls.getPrice(sku, qty)

                        withContext(Dispatchers.Main) {
                            if (price != null)
                                item.priceStatus = Settings.PriceStatus.SUCCESS_GETTING_PRICES
                            else
                                item.priceStatus = Settings.PriceStatus.FAILED_GETTING_PRICES
                            refreshItemStatus(item)
                            next()
                        }
                    } catch (ex: Exception) {
                        Log.e("getPriceStockEnquiry", ex.message ?: "error")
                        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                            return@launch
                        withContext(Dispatchers.Main) {
                            item.priceStatus = Settings.PriceStatus.FAILED_GETTING_PRICES
                            refreshItemStatus(item)
                            next()
                        }
                    }
                }
            }
        }
    }

    private fun renderFooter() {

        val totalIncVat = CartHelper.basketTotalIncVat(cartItems)
        savedPrice = totalIncVat
        binding.footer.binding.button.isEnabled = basketItemsValid
        binding.footer.binding.priceWidget.priceExVAT = CartHelper.basketTotalExVat(cartItems)
        binding.footer.binding.priceWidget.priceIncVAT = totalIncVat
    }

    private fun checkOrderPriceValidity() {
        val totalIncVat = CartHelper.basketTotalIncVat(cartItems)
        if (checkingOut) {
            checkingOut = false
            totalsValid = true
            if (savedPrice != totalIncVat)
                totalsValid = false
            if (basketPricesFailureTestScenario)
                totalsValid = false
            if (basketItemsValid && totalsValid) {
                goToCheckout()
            } else if (basketItemsValid && !totalsValid) {
                showPricesChangedAlert()
            } else if (!basketItemsValid && totalsValid) {
                showItemsInvalidAlert()
            } else if (!basketItemsValid && !totalsValid) {
                showInvalidAlert()
            }
        }
    }

    private fun next() {
        if (basketQueue.isEmpty()) {
            refreshAdapter()

            checkOrderPriceValidity()
            renderFooter()
        } else {
            val item = basketQueue.poll()
            if(item!=null) {
                item.priceStatus = Settings.PriceStatus.NONE
                item.qty?.let { getPriceStockEnquiry(item, it) }
            }
        }
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecycler() {
        binding.recycler.layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        binding.recycler.addItemDecoration(DividerItemDecoration(activity, LinearLayoutManager.VERTICAL))
        binding.recycler.adapter = RecyclerViewAdapter()

        val swipeCallBack: ItemTouchHelper.SimpleCallback = SwipeDeleteCallback()
        val helper = ItemTouchHelper(swipeCallBack)
        helper.attachToRecyclerView(binding.recycler)
    }

    private fun setupBackButton() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@BasketFragment).navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }


    inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.basket_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            if(basketItemsFailureTestScenario && position == itemCount-1)
                basketItemsValid = false

            val canBeAddedToBasket: Boolean
            val items = cartItems
            if (items != null) {
                if (position >= items.size)
                    return
            }
            holder.binding.discount.visibility = View.INVISIBLE
            holder.binding.oldPrice.visibility = View.INVISIBLE
            holder.binding.priceWidget.visibility = View.INVISIBLE

            val item = items?.get(position)
            val part = Gson().fromJson(item?.data, Part::class.java)

            //save the temp qty value
            val qty = item?.qty
            item?.tempQty = 0
            if (qty != null) {
                item.tempQty = qty
            }

            holder.binding.partIDText.text = item?.partNum
            holder.binding.qtyText.setText(item?.qty.toString())
            holder.binding.qtyText.clearFocus()

            holder.binding.qtyText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val q = holder.binding.qtyText.text.toString()
                    if (q.isBlank())
                        holder.binding.qtyText.setText(item?.qty.toString())
                    item?.tempQty = q.toInt()
                    updateQty(item)
                    true
                } else {
                    false
                }
            }
            //show the progress bar if the item status is getting prices
            if (item?.priceStatus == Settings.PriceStatus.GETTING_PRICES) {
                holder.binding.progress.visibility = View.VISIBLE
            } else {
                holder.binding.progress.visibility = View.GONE
            }

            //Here we display the data that is stored in the price stock map
            val sku = item?.fccPart
            var priceStock: Product? = null
            if(sku!=null && qty!=null) {
                priceStock = App.globalData.getPriceStockFromMap(sku, qty)
                if (priceStock != null) {
                    val cost = priceStock.customerCost?:0.0
                    if(cost > 0) {
                        holder.binding.priceWidget.renderCost(priceStock)
                        PriceHelper.renderDiscount(priceStock, holder.binding.oldPrice,holder.binding.discount)
                    }
                }
            }

            holder.binding.plusButton.setOnClickListener {
                val newQty = item?.qty?.plus(1)
                if (newQty != null && newQty > 0) {
                    item.tempQty = newQty
                    updateQty(item)
                }
            }
            holder.binding.minusButton.setOnClickListener {
                val newQty = item?.qty?.minus(1)
                if (newQty != null && newQty > 0) {
                    item.tempQty = newQty
                    updateQty(item)
                }
            }

            if (part != null) {
                canBeAddedToBasket = PriceHelper.renderPrices(holder.binding.inStock,holder.binding.tick,priceStock,item?.priceStatus)

                PriceHelper.render360(holder.binding.threeSixtyIcon, V4APIHelper.is360(part))


                if(canBeAddedToBasket)
                    holder.binding.priceWidget.visibility = View.VISIBLE
                else
                   basketItemsValid = false

                holder.binding.manufacturerText.text = part.manufacturer
                holder.binding.descriptionText.text = part.partDescription

                Helpers.renderImage(holder.binding.image,part.images?.get(0)?.url)
            }
        }


        override fun getItemCount(): Int {
            return cartItems?.size?:0
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            val binding = BasketItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    inner class SwipeDeleteCallback: SwipeCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            super.onSwiped(viewHolder, direction)
            val cartItem = cartItems?.get(viewHolder.bindingAdapterPosition)
            if (cartItem != null) {
                basketQueue.clear()
                deleteItem(cartItem)
            }
        }
    }


    interface OnFragmentInteractionListener {
    }

}