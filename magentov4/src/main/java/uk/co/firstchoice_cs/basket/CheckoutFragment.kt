package uk.co.firstchoice_cs.basket

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adyen.checkout.components.model.PaymentMethodsApiResponse
import com.adyen.checkout.components.model.payments.request.PaymentComponentData
import com.adyen.checkout.components.model.payments.request.PaymentMethodDetails
import com.adyen.checkout.bcmc.BcmcConfiguration
import com.adyen.checkout.card.CardConfiguration
import com.adyen.checkout.core.api.Environment
import com.adyen.checkout.core.exception.CheckoutException
import com.adyen.checkout.core.log.Logger
import com.adyen.checkout.core.util.LocaleUtil
import com.adyen.checkout.dropin.DropIn
import com.adyen.checkout.dropin.DropInConfiguration
import com.adyen.checkout.googlepay.GooglePayConfiguration
import com.google.gson.Gson
import kotlinx.android.synthetic.main.checkout_payment_method.view.*
import kotlinx.android.synthetic.main.footer_widget.view.*
import kotlinx.android.synthetic.main.fragment_basket.*
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uk.co.firstchoice_cs.*
import uk.co.firstchoice_cs.adyen.PaymentMethodsViewModel
import uk.co.firstchoice_cs.adyen.data.storage.KeyValueStorage
import uk.co.firstchoice_cs.adyen.data.storage.KeyValueStorageImpl
import uk.co.firstchoice_cs.adyen.service.FCDropInService
import uk.co.firstchoice_cs.core.api.AddressManager
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.api.magentoAPI.MagentoAPI
import uk.co.firstchoice_cs.core.api.magentoAPI.OrderResult
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.database.cart.CartItem
import uk.co.firstchoice_cs.core.database.cart.CartViewModel
import uk.co.firstchoice_cs.core.helpers.*
import uk.co.firstchoice_cs.core.helpers.CheckoutHelper.buildItem
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import uk.co.firstchoice_cs.firstchoice.BuildConfig.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.CheckoutBasketItemBinding
import uk.co.firstchoice_cs.firstchoice.databinding.FragmentCheckoutBinding
import uk.co.firstchoice_cs.more.WebFragment
import java.util.*


class CheckoutFragment : Fragment(R.layout.fragment_checkout) {

    val TAG: String = "CheckoutFragment"
    private lateinit var cartViewModel: CartViewModel
    private lateinit var cartItems: List<CartItem>
    private var basketQueue: Queue<CartItem> = LinkedList()
    lateinit var binding: FragmentCheckoutBinding
    private var pONumber: String = ""
    var comment: String = ""

    private val paymentMethodsViewModel: PaymentMethodsViewModel by viewModel()
    private val keyValueStorage: KeyValueStorage by inject()
    private var isWaitingPaymentMethods = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)

        if(App.instance.awaitingPaymentResult)
        {
            App.instance.awaitingPaymentResult = false
            Toast.makeText(requireContext(), App.instance.paymentCallResult, Toast.LENGTH_SHORT).show()
            processPaymentResult()
        }

        paymentMethodsViewModel.paymentMethodResponseLiveData.observe(
            this,
            {
                if (it != null) {
                    Logger.d(TAG, "Got paymentMethods response - oneClick? ${it.storedPaymentMethods?.size ?: 0}")
                    if (isWaitingPaymentMethods) startDropIn(it)
                } else {
                    Logger.v(TAG, "API response is null")
                }
            }
        )

        setFragmentResultListener("paymentRequest") { _, bundle ->
            val result = bundle.getString("paymentResult")
            if (result != null) {
                App.selectedPaymentType = result
                setPaymentType()
            }
        }

        setFragmentResultListener("addAddressRequest") { _, bundle ->
            val addressStr = bundle.getString("addAddressResult")
            val gson = Gson()
            val address = gson.fromJson(addressStr, AddressManager.Address::class.java)
            val mode = bundle.getInt("mode")
            if (address != null) {
                if(mode==0) {
                    App.addresses.setSelectedBillingAddress(address = address)
                    setBillingAddress()
                }
                else {
                    App.addresses.setSelectedShippingAddress(address = address)
                    setShippingAddress()
                }
            }
        }
    }


    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            goBack()
        }
    }

    private fun setupBackButton() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }



    private fun gotoDeliveryAddresses() {
        val bundle = Bundle()
        bundle.putInt("mode", AddressManager.AddressTypes.SHIPPING.ordinal)
        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.action_checkoutFragment_to_deliveryAddressesFragment, bundle)
    }

    private fun gotoBillingAddresses() {
        val bundle = Bundle()
        bundle.putInt("mode", AddressManager.AddressTypes.BILLING.ordinal)
        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.action_checkoutFragment_to_deliveryAddressesFragment, bundle)
    }

    private fun gotoAddDeliveryAddresses() {
        val bundle = Bundle()
        bundle.putInt("mode", AddressManager.AddressTypes.SHIPPING.ordinal)
        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.action_checkoutFragment_to__add_deliveryAddressesFragment, bundle)
    }

    private fun gotoAddBillingAddresses() {
        val bundle = Bundle()
        bundle.putInt("mode", AddressManager.AddressTypes.BILLING.ordinal)
        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.action_checkoutFragment_to__add_billingAddressesFragment, bundle)
    }

    private fun gotoDeliveryMethods() {

        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.action_checkoutFragment_to_deliveryMethodsFragment)
    }

    private fun returnToBasketThenShowOrderConfirmation(orderResult: OrderResult) {
        val gson = Gson()
        val jsonString = gson.toJson(orderResult)
        setFragmentResult("checkoutRequest", bundleOf("checkoutResult" to jsonString))
        goBack()
    }

    private fun goBack() {
        Helpers.hideKeyboard(view)
        NavHostFragment.findNavController(this@CheckoutFragment).navigateUp()
    }


    private fun createCustomerAccount() {
        val bundle = Bundle()
        bundle.putString(WebFragment.ARG_PARAM_URL, FireBaseViewModel.getCreateAccountUrl())
        bundle.putString(WebFragment.ARG_PARAM_TITLE, "Create Account")
        NavHostFragment.findNavController(this@CheckoutFragment)
            .navigate(R.id.create_account_action, bundle)
    }

    fun isValid(): Boolean {
        var res = true
        if (App.addresses.getSelectedShippingAddress() == null)
            res = false
        if (App.addresses.getSelectedBillingAddress() == null)
            res = false
        if (CartHelper.getSelectedShippingMethod() == null)
            res = false
        binding.footer.binding.button.isEnabled = res
        return res
    }

    private fun showDuplicatePOAlert() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("Duplicate PO Number")
            .setMessage("Unable to checkout due to duplicate PO number")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun loginExpiryWarning() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setTitle("Login Expired")
            .setMessage("Please sign into your account again, to process this order")
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun getPaymentIcon(type: String): Int {
        return when (type) {
            Constants.PAYMENT_TYPE_CARD -> {
                R.drawable.ic_baseline_credit_card_24
            }
            Constants.PAYMENT_TYPE_ACCOUNT -> {
                R.drawable.account_icon
            }
            else -> {
                R.drawable.ic_baseline_credit_card_24
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCheckoutBinding.bind(view)
        setUpToolbar()
        setupRecycler()
        setupBackButton()
        isValid()

        var isB2B = false
        if (App.magentoCustomer != null)
            isB2B = CartHelper.isB2B(App.magentoCustomer)

        binding.loginWrapper.root.visibility = View.GONE
        App.loginState.observe(viewLifecycleOwner, {
            lifecycleScope.launch(context = Dispatchers.Main) {
                if (App.loginState.value == Settings.LoginState.LOGGED_IN_ACCOUNT)
                    binding.loginWrapper.root.visibility = View.GONE
                else
                    binding.loginWrapper.root.visibility = View.VISIBLE
            }
        })

        cartViewModel = ViewModelProvider(requireActivity()).get(CartViewModel::class.java)
        cartViewModel.allCartItems.observe(requireActivity(), {
            cartItems = it
            basketQueue = LinkedList(cartItems)
            next() //this will start polling to get new prices
            refreshAdapter() //this will default render the items
        })

        setShippingAddress()

        setBillingAddress()

        setShippingMethod()

        setPaymentMethod(isB2B)

        binding.loginWrapper.loginButton.setOnClickListener {
            NavHostFragment.findNavController(this@CheckoutFragment).navigate(R.id.sign_in_action)
        }
        binding.loginWrapper.createAccountButton.setOnClickListener {
            createCustomerAccount()
        }

        binding.billingAddress.root.setOnClickListener {
            if (App.addresses.mergedAddresses.isNullOrEmpty())
                gotoAddBillingAddresses()
            else
                gotoBillingAddresses()
        }
        binding.deliveryAddress.root.setOnClickListener {

            if (App.addresses.mergedAddresses.isNullOrEmpty())
                gotoAddDeliveryAddresses()
            else
                gotoDeliveryAddresses()
        }
        binding.shippingMethod.root.setOnClickListener { gotoDeliveryMethods() }
        binding.footer.binding.button.setOnClickListener {

            binding.footer.binding.button.isEnabled = false
            processCheckout()
        }

        binding.orderReferenceEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                pONumber = binding.orderReferenceEditText.text.toString()
                Helpers.hideKeyboard(binding.orderReferenceEditText)
                isValid()
                true
            } else false
        }

        binding.commentsEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                comment = binding.commentsEditText.text.toString()
                Helpers.hideKeyboard(binding.orderReferenceEditText)
                isValid()
                true
            } else false
        }
    }

    private fun setPaymentMethod(isB2B: Boolean) {
        if (isB2B) {
            binding.paymentMethod.root.setOnClickListener {
                val paymentDialog = PaymentMethodsDialog.newInstance()
                paymentDialog.show(parentFragmentManager, "paymentRequest")
            }
            if (App.selectedPaymentType.isBlank()) {
                binding.paymentMethod.selectText.visibility = View.VISIBLE
                binding.paymentMethod.paymentTypeText.text = App.selectedPaymentType
                binding.paymentMethod.check.visibility = View.VISIBLE
                binding.paymentMethod.leading.visibility = View.INVISIBLE
            } else {
                setPaymentType()
            }
        } else {
            binding.paymentMethod.selectText.visibility = View.GONE
            binding.paymentMethod.paymentTypeText.text = Constants.PAYMENT_TYPE_CARD
            binding.paymentMethod.check.visibility = View.INVISIBLE
            binding.paymentMethod.leading.setImageResource(getPaymentIcon(binding.paymentMethod.paymentTypeText.text.toString()))
        }
    }

    private fun setShippingMethod() {
        val selectedShippingMethod = CartHelper.getSelectedShippingMethod()
        if (selectedShippingMethod == null) {
            binding.shippingMethod.selectText.visibility = View.VISIBLE
            binding.shippingMethod.leading.visibility = View.GONE
            binding.shippingMethod.deliveryTypeText.visibility = View.GONE
            binding.shippingMethod.descriptionText.visibility = View.GONE
            binding.shippingMethod.priceWidget.visibility = View.GONE
        } else {
            binding.shippingMethod.selectText.visibility = View.GONE
            binding.shippingMethod.deliveryTypeText.text = selectedShippingMethod.carrier
            binding.shippingMethod.descriptionText.text = selectedShippingMethod.description
            binding.shippingMethod.priceWidget.priceExVAT =
                selectedShippingMethod.miscInfo?.get(0)?.MiscAmt ?: 0.0
            binding.shippingMethod.priceWidget.priceIncVAT =
                CartHelper.priceWithTax(selectedShippingMethod.miscInfo?.get(0)?.MiscAmt ?: 0.0)
        }
    }

    private fun processCheckout() {
        if (App.selectedPaymentType == Constants.PAYMENT_TYPE_ACCOUNT) {
            accountProcessing()
        } else {
            if (LoginHelper.isSignedInAndNotExpired()) {
                App.instance.magentoOrder = buildMagentoOrder() //build the order structure
                if(App.instance.magentoOrder!=null) {
                    //do we have the payment methods?
                    val paymentMethods = paymentMethodsViewModel.paymentMethodResponseLiveData.value //get the payment methods if we have them already
                    //yes use them
                    if (paymentMethods != null) {
                        startDropIn(paymentMethods)
                    } else {
                        //no request them and observe the result
                        GlobalScope.launch(context = Dispatchers.IO) {
                            setRequestParameters()
                            paymentMethodsViewModel.requestPaymentMethods()
                        }
                        isWaitingPaymentMethods = true
                        setLoading(true)
                    }
                }
                else {
                    Toast.makeText(requireContext(), "Unable to create order", Toast.LENGTH_LONG).show()
                }
            } else {
                loginExpiryWarning()
                binding.footer.binding.button.isEnabled = true
            }
        }
    }

    private fun setRequestParameters() {
        //set the values that are going to be used for the request
        KeyValueStorageImpl.DEFAULT_EMAIL = App.instance.magentoOrder?.entity?.customer_email ?: ""
        KeyValueStorageImpl.DEFAULT_VALUE = App.instance.magentoOrder?.entity?.grand_total?.times(100.0).toString()
        if (keyValueStorage.getShopperReference().isNullOrEmpty()) {

        }
    }

    private fun accountProcessing() {
        //check for duplicate PO
        lifecycleScope.launch(context = Dispatchers.Main) {
            if (App.loginState.value == Settings.LoginState.LOGGED_IN_ACCOUNT) {
                if (App.customer?.checkDuplicatePo == true) {
                    val customerNumber = App.customer?.custId
                    if (pONumber.isNotBlank() && !customerNumber.isNullOrBlank()) {
                        val isDup = CustomerAPICalls.isDuplicatePO(customerNumber, pONumber)
                        if (isDup) {
                            showDuplicatePOAlert()
                            return@launch
                        }
                    }
                }
            }
        }
        App.instance.magentoOrder = buildMagentoOrder()
        if(App.instance.magentoOrder!=null) {
            sendMagentoOrder()
        }
        else {
            Toast.makeText(requireContext(), "Unable to create order", Toast.LENGTH_LONG).show()
        }
    }




    fun buildMagentoOrder(paymentData: PaymentComponentData<PaymentMethodDetails>? = null) :MagentoOrder?{

        if (paymentData == null)
            binding.progress.visibility = View.VISIBLE
        val selectedShippingMethod = CartHelper.getSelectedShippingMethod()
        val selectedBillingAddress = App.addresses.getSelectedBillingAddress()
        val selectedShippingAddress = App.addresses.getSelectedShippingAddress()
        //shipping
        val baseShippingAmount = selectedShippingMethod?.miscInfo?.get(0)?.MiscAmt ?: 0.0
        val baseShippingInclTax = CartHelper.priceWithTax(baseShippingAmount)
        val baseShippingTaxAmount = baseShippingInclTax - baseShippingAmount
        val shipViaNum = selectedShippingMethod?.shipViaCode
        val flatRateCode = Helpers.shippingMap[shipViaNum]
        //totals
        val baseSubTotal = CartHelper.basketTotalExVat(cartItems)
        val baseSubTotalIncVat = CartHelper.basketTotalIncVat(cartItems)
        val baseGrandTotal = baseSubTotalIncVat + baseShippingInclTax
        val baseTaxAmount = baseGrandTotal - baseSubTotal

        val firstName = CheckoutHelper.getFirstName()
        val lastName =  CheckoutHelper.getLastName()
        val middleName =  CheckoutHelper.getMiddleName()
        val email =  CheckoutHelper.getEmail()
        val customerId =  CheckoutHelper.getCustomerID()

        val items = mutableListOf<Item>()
        for (cartItem in cartItems) {
            if(cartItem.tempPrice!=null) {
                val originalPrice = cartItem.tempPrice?.customerExtendedCost ?:return null
                val basePrice = cartItem.tempPrice?.customerExtendedCost ?: return null
                val basePriceIncTax = CartHelper.priceWithTax(basePrice)
                val baseItemTaxAmount =basePriceIncTax - basePrice
                val baseRowTotalIncTax = cartItem.tempPrice?.customerExtendedCost?:return null

                val item = buildItem(originalPrice, baseSubTotal, baseRowTotalIncTax, basePrice, baseTaxAmount, cartItem, baseSubTotalIncVat, baseItemTaxAmount)
                items.add(item)
            }
            else
            {
                return null
            }

        }

        val shippingAddress = CheckoutHelper.shippingAddress(selectedShippingAddress)
        val shippingTotal = CheckoutHelper.buildShippingTotal(baseShippingAmount, baseShippingInclTax, baseShippingTaxAmount)
        val shipping = CheckoutHelper.buildShipping(shippingAddress, flatRateCode, shippingTotal)
        val shippingAssignments = CheckoutHelper.buildShippingAssignments(shipping, items)
        val extensionAttributes = CheckoutHelper.buildExtensionAttributes(shippingAssignments)
        val payment = CheckoutHelper.buildPayment(baseGrandTotal, baseSubTotal, baseShippingAmount,pONumber)
        val billingAddress = CheckoutHelper.buildBillingAddress(selectedBillingAddress)

        val entity = CheckoutHelper.buildEntity(
            baseGrandTotal,
            baseShippingAmount,
            baseShippingInclTax,
            baseShippingTaxAmount,
            baseSubTotal,
            baseSubTotalIncVat,
            billingAddress,
            email,
            firstName,
            customerId,
            lastName,
            middleName,
            extensionAttributes,
            items,
            payment,
            selectedShippingMethod,
            baseTaxAmount
        )

        return MagentoOrder(entity = entity)
    }

    private fun sendMagentoOrder()
    {
        val gson = Gson()
        val order = gson.toJson(App.instance.magentoOrder)
        lifecycleScope.launch(context = Dispatchers.IO) {
            val orderResult = MagentoAPI.createOrder(order = order)
            withContext(Dispatchers.Main)
            {
                setLoading(false)
                if (orderResult!=null) {
                    cartViewModel.deleteAll()
                    returnToBasketThenShowOrderConfirmation(orderResult)
                } else {
                    Toast.makeText(requireContext(), "Order failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processPaymentResult(){
        val paymentResult = App.instance.paymentResult
        val magentoOrder = App.instance.magentoOrder

    }


    private fun startDropIn(paymentMethodsApiResponse: PaymentMethodsApiResponse) {
        Logger.d(TAG, "startDropIn")
        setLoading(false)
        setRequestParameters()

        val shopperLocaleString = keyValueStorage.getShopperLocale()
        val shopperLocale = LocaleUtil.fromLanguageTag(shopperLocaleString)

        val cardConfiguration = CardConfiguration.Builder(requireActivity())
            .setPublicKey(PUBLIC_KEY)
            .setShopperReference(keyValueStorage.getShopperReference())
            .setShopperLocale(shopperLocale)
            .setEnvironment(Environment.TEST)
            .build()

        val googlePayConfig =
            GooglePayConfiguration.Builder(requireActivity(), keyValueStorage.getMerchantAccount())
                .setCountryCode(keyValueStorage.getCountry())
                .setEnvironment(Environment.TEST)
                .build()


        val bcmcConfiguration = BcmcConfiguration.Builder(requireActivity())
            .setPublicKey(PUBLIC_KEY)
            .setShopperLocale(shopperLocale)
            .setEnvironment(Environment.TEST)
            .build()

        val resultIntent = Intent(requireActivity(), MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val dropInConfigurationBuilder = DropInConfiguration.Builder(
            requireActivity(),
            resultIntent,
            FCDropInService::class.java
        )
            .setEnvironment(Environment.TEST)
            .setClientKey(CLIENT_KEY)
            .setShopperLocale(shopperLocale)
            .addCardConfiguration(cardConfiguration)
            .addBcmcConfiguration(bcmcConfiguration)
            .addGooglePayConfiguration(googlePayConfig)

        val amount = keyValueStorage.getAmount()

        try {
            dropInConfigurationBuilder.setAmount(amount)
        } catch (e: CheckoutException) {
            Logger.e(TAG, "Amount $amount not valid", e)
        }
        App.instance.awaitingPaymentResult = true

        DropIn.startPayment(
            requireActivity(),
            paymentMethodsApiResponse,
            dropInConfigurationBuilder.build()
        )
    }

    private fun setLoading(isLoading: Boolean) {
        isWaitingPaymentMethods = isLoading
        if (isLoading) {
            binding.footer.binding.button.visibility = View.GONE
            binding.progress.visibility = View.VISIBLE
        } else {
            binding.footer.binding.button.visibility = View.VISIBLE
            binding.progress.visibility = View.GONE
        }
    }


    private fun setShippingAddress() {
        val selectedShippingAddress = App.addresses.getSelectedShippingAddress()
        if (selectedShippingAddress == null) {
            binding.deliveryAddress.selectText.visibility = View.VISIBLE
            binding.deliveryAddress.leading.visibility = View.GONE
            binding.deliveryAddress.address1.visibility = View.GONE
            binding.deliveryAddress.address2.visibility = View.GONE
            binding.deliveryAddress.postcode.visibility = View.GONE
            binding.deliveryAddress.contactNumber.visibility = View.GONE
        } else {
            binding.deliveryAddress.title.text = getString(
                R.string.full_name,
                selectedShippingAddress.firstname ?: "",
                selectedShippingAddress.lastname ?: ""
            )
            binding.deliveryAddress.selectText.visibility = View.GONE
            binding.deliveryAddress.address1.text = selectedShippingAddress.company ?: ""
            binding.deliveryAddress.address2.text = selectedShippingAddress.city ?: ""
            binding.deliveryAddress.postcode.text = selectedShippingAddress.postcode ?: ""
            binding.deliveryAddress.contactNumber.text = selectedShippingAddress.telephone ?: ""
        }
        isValid()
    }

    private fun setBillingAddress() {
        val selectedBillingAddress = App.addresses.getSelectedBillingAddress()
        if (selectedBillingAddress == null) {
            binding.billingAddress.selectText.visibility = View.VISIBLE
            binding.billingAddress.leadingBilling.visibility = View.GONE
            binding.billingAddress.address1Billing.visibility = View.GONE
            binding.billingAddress.address2Billing.visibility = View.GONE
            binding.billingAddress.postcodeBilling.visibility = View.GONE
            binding.billingAddress.contactNumberBilling.visibility = View.GONE
        } else {
            binding.billingAddress.titleBilling.text = getString(
                R.string.full_name,
                selectedBillingAddress.firstname ?: "",
                selectedBillingAddress.lastname ?: ""
            )
            binding.billingAddress.selectText.visibility = View.GONE
            binding.billingAddress.address1Billing.text = selectedBillingAddress.company ?: ""
            binding.billingAddress.address2Billing.text = selectedBillingAddress.city ?: ""
            binding.billingAddress.postcodeBilling.text = selectedBillingAddress.postcode ?: ""
            binding.billingAddress.contactNumberBilling.text =
                selectedBillingAddress.telephone ?: ""
        }
        isValid()
    }

    private fun setPaymentType() {
        binding.paymentMethod.selectText.visibility = View.GONE
        binding.paymentMethod.paymentTypeText.text = App.selectedPaymentType
        binding.paymentMethod.check.visibility = View.VISIBLE
        binding.paymentMethod.leading.setImageResource(R.drawable.ic_baseline_credit_card_24)
        binding.paymentMethod.leading.setImageResource(getPaymentIcon(binding.paymentMethod.paymentTypeText.text.toString()))
        isValid()
    }

    private fun refreshItemStatus(cartItem: CartItem?) {
        val pos = cartItem?.let { CartHelper.getPosition(updatedItem = it, items = cartItems) }
        if (cartItem != null && pos != null) {
            binding.recycler.adapter?.notifyItemChanged(pos)
        }
    }

    private fun refreshAdapter() {
        lifecycleScope.launch(context = Dispatchers.Main) {
            binding.recycler.adapter?.notifyDataSetChanged()
            binding.footer.binding.button.requestFocus()
            renderFooter()
        }
    }

    private fun renderFooter() {
        val selectedBillingMethod = CartHelper.getSelectedShippingMethod()
        val deliveryCost = selectedBillingMethod?.miscInfo?.get(0)?.MiscAmt ?: 0.0
        val deliveryCostIncVat = CartHelper.priceWithTax(deliveryCost)

        val subTotalExVat = CartHelper.basketTotalExVat(cartItems)
        val subTotalIncVat = CartHelper.basketTotalIncVat(cartItems)

        val basketTotalIncVat = subTotalIncVat + deliveryCostIncVat
        val basketTotalExVat = CartHelper.basketTotalExVat(cartItems) + deliveryCost

        //items price
        binding.summary.subtotalValueText.priceIncVAT = subTotalIncVat
        binding.summary.subtotalValueText.priceExVAT = subTotalExVat
        //shipping prices
        binding.summary.shippingValueText.priceIncVAT = deliveryCostIncVat
        binding.summary.shippingValueText.priceExVAT = deliveryCost
        //combined prices
        binding.summary.totalValueText.priceIncVAT = basketTotalIncVat
        binding.summary.totalValueText.priceExVAT = basketTotalExVat

        binding.footer.binding.priceWidget.priceExVAT = basketTotalExVat
        binding.footer.binding.priceWidget.priceIncVAT = basketTotalIncVat
    }

    private fun next() {
        if (basketQueue.isEmpty()) {
            refreshAdapter()
            renderFooter()
        } else {
            val item = basketQueue.poll()
            if (item != null) {
                item.priceStatus = Settings.PriceStatus.NONE
                item.qty?.let { getPriceStockEnquiry(item, it) }
            }
        }
    }

    private fun getPriceStockEnquiry(item: CartItem?, qty: Int) {

        val sku = item?.fccPart
        if (sku != null) {
            var price = App.globalData.getPriceStockFromMap(sku, qty)
            if (price != null) {
                item.tempPrice = price
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
                        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                            return@launch

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
                .inflate(R.layout.checkout_basket_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {

            val items = cartItems
            if (position >= items.size)
                return
            holder.binding.discount.visibility = View.INVISIBLE
            holder.binding.oldPrice.visibility = View.INVISIBLE
            holder.binding.priceWidget.visibility = View.INVISIBLE

            val item = items[position]
            val part = Gson().fromJson(item.data, Part::class.java)

            //save the temp qty value
            val qty = item.qty ?: 1
            holder.binding.qtyText.text = getString(R.string.qty, qty)

            holder.binding.partIDText.text = item.partNum

            //show the progress bar if the item status is getting prices
            if (item.priceStatus == Settings.PriceStatus.GETTING_PRICES) {
                holder.binding.progress.visibility = View.VISIBLE
            } else {
                holder.binding.progress.visibility = View.GONE
            }

            //Here we display the data that is stored in the price stock map
            val sku = item.fccPart
            var priceStock: Product? = null
            if (sku != null) {
                priceStock = App.globalData.getPriceStockFromMap(sku, qty)
                if (priceStock != null) {
                    val cost = priceStock.customerCost ?: 0.0
                    if (cost > 0) {
                        holder.binding.priceWidget.renderCost(priceStock)
                        PriceHelper.renderDiscount(
                            priceStock,
                            holder.binding.oldPrice,
                            holder.binding.discount
                        )
                    }
                }
            }

            if (part != null) {

                val canShowPrices = PriceHelper.renderPrices(
                    holder.binding.inStock,
                    holder.binding.tick,
                    priceStock,
                    item.priceStatus
                )

                PriceHelper.render360(holder.binding.threeSixtyIcon, V4APIHelper.is360(part))

                Helpers.renderImage(holder.binding.image, part.images?.get(0)?.url)

                if (canShowPrices)
                    holder.binding.priceWidget.visibility = View.VISIBLE

                holder.binding.manufacturerText.text = part.manufacturer
                holder.binding.descriptionText.text = part.partDescription

                Helpers.renderImage(holder.binding.image, part.images?.get(0)?.url)
            }
        }


        override fun getItemCount(): Int {
            return CartHelper.numberOfBasketItems(cartItems)
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            val binding: CheckoutBasketItemBinding = CheckoutBasketItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }
}

