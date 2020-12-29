package uk.co.firstchoice_cs.core.shared

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.v4API.*
import uk.co.firstchoice_cs.core.database.cart.CartItem
import uk.co.firstchoice_cs.core.database.cart.CartViewModel
import uk.co.firstchoice_cs.core.database.recent_searches.RecentSearchItem
import uk.co.firstchoice_cs.core.database.recent_searches.RecentSearchViewModel
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.CartHelper.cartItemFromPart
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.PriceHelper
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.helpers.V4APIHelper
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.ThumbNailManager
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.*
import uk.co.firstchoice_cs.store.vm.ShopViewModel
import java.util.*


open class AddPartFragment : Fragment(R.layout.fragment_add_part) {

    private lateinit var productDetails: Product
    private lateinit var cartItem: CartItem
    private lateinit var prodStr: String
    private lateinit var cartViewModel: CartViewModel
    private lateinit var cartItems: List<CartItem>
    private lateinit var product: Part
    private lateinit var imageList: MutableList<String>
    private lateinit var binding:FragmentAddPartBinding
    private var mListener: OnFragmentInteractionListener? = null
    private var isUpdate = false
    private var tabMode = 0
    private val techAdapter = TechAdapter()
    private val documentsAdapter = DocumentAdapter()
    private val modelsAdapter = ModelsAdapter()
    private var modelList: ArrayList<FitsModel> = ArrayList()
    private var technicalList: ArrayList<String> = ArrayList()
    private var docs: ArrayList<DocumentProd> = ArrayList()
    private var tempQty = 1
    private var initialised = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAddPartBinding.bind(view)
        binding.dots.bringToFront()
        hideEmpty()
        setUpDownloadBottomSheet()
        showAddButtons(false)
        binding.priceWidget.visibility = View.INVISIBLE
        binding.toolbar.title = product.partNum
        binding.toolbar.subtitle = product.manufacturer
        binding.dots.setupWithViewPager(binding.viewPager)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchViews(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })


        setUpToolbar()

        insertToRecentSearches()

        buildPartItem()

        adjustImageSizeToHalfScreenHeight()
    }


    open fun goTo360View(product: Part) {
    }

    open fun navigateToModel(model: Model) {
    }

    open fun navigateToStore() {
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(
                context.toString()
                        + " must implement OnFragmentInteractionListener"
            )
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun showSnackBarMessage(message: String, duration: Int)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            prodStr = SafetyChecks.safeArg(arguments, "ProductArg")
            product = Gson().fromJson(prodStr, Part::class.java)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.add_parts, menu)
    }

    private fun setUpDownloadBottomSheet() {
        val sheetBehavior = BottomSheetBehavior.from(binding.downloadBottomSheet)
        sheetBehavior.peekHeight = 0
        sheetBehavior.isHideable = true
        binding.downloadBottomSheet.setSheetBehavior(sheetBehavior)
        binding.downloadBottomSheet.hide()

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Helpers.logBottomSheetState("DownloadBottomSheet", newState)
            }
        })
    }



    private fun adjustImageSizeToHalfScreenHeight() {
        val height = resources.configuration.screenHeightDp.toDouble()
        val headerAndFooter = 100.0f
        val density = resources.displayMetrics.density.toDouble()
        binding.guidelineTop.setGuidelineBegin(((height - headerAndFooter) * density).toInt() / 2)
    }

    private fun buildPartItem() {
        cartItem = cartItemFromPart(prodStr, product)
        initialised = true
        cartViewModel = ViewModelProvider(requireActivity()).get(CartViewModel::class.java)
        cartViewModel.allCartItems.observe(requireActivity(), {
            cartItems = it
            var found = false
            for (item in cartItems) {
                if (item.partNum == product.partNum) {
                    found = true
                    cartItem = item
                    isUpdate = true
                    tempQty = cartItem.qty ?: 1
                    break
                }
            }
            if (!found) {
                isUpdate = false
                tempQty = 1
            }
            loadProducts()
        })
    }

    private fun insertToRecentSearches() {
        val recentSearchViewModel =
            ViewModelProvider(requireActivity()).get(RecentSearchViewModel::class.java)
        recentSearchViewModel.allSearches.observe(requireActivity(), {
            val searches = it
            var found = false
            for (search in searches) {
                if (search.part_num == product.partNum) {
                    found = true
                    break
                }
            }

            if (!found) {
                recentSearchViewModel.insert(
                    RecentSearchItem(
                        Gson().toJson(product),
                        product.partNum
                    )
                )
            }
        })
    }

    private fun draw()
    {
        setTabToData()
        hideUnusedTabs()
        render(cartItem)
    }

    private fun switchViews(pos: Int) {
        tabMode = pos
        when (pos) {
            0 -> {
                hideEmpty()
                showTechnicalDetails()
            }
            1 -> {
                hideEmpty()
                showModels()
            }
            2 -> {
                hideEmpty()
                showDocuments()
            }
        }
    }

    private fun showTechnicalDetails() {
        binding.recycler.adapter = techAdapter
        techAdapter.dataChanged()
    }

    private fun showDocuments() {
        binding.recycler.adapter = documentsAdapter
        documentsAdapter.dataChanged()
    }

    private fun showModels() {
        binding.recycler.adapter = modelsAdapter
        modelsAdapter.dataChanged()
    }

    private fun showEmpty(title: String, description: String) {
        binding.recycler.visibility = View.GONE
        binding.empty.root.visibility = View.VISIBLE
        binding.empty.emptyTitle.text = title
        binding.empty.emptyDescription.text = description
        binding.empty.emptyButton.visibility = View.GONE
    }

    private fun hideEmpty() {
        binding.empty.root.visibility = View.GONE
        binding.empty.emptyButton.visibility = View.GONE
        binding.recycler.visibility = View.VISIBLE
    }

    private fun loadProducts() {
        binding.pbDownload.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sku = product.fccPart
                if (sku != null) {
                    val productResult = V4APICalls.product(sku, 0, 2000)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch

                    withContext(Dispatchers.Main) {
                        binding.pbDownload.visibility = View.GONE
                        val prod = productResult?.product
                        if (!prod.isNullOrEmpty()) {
                            productDetails = prod[0]
                            docs.clear()
                            modelList.clear()

                            if (!productDetails.documents.isNullOrEmpty())
                                docs.addAll(productDetails.documents!!)
                            if (!productDetails.fitsModel.isNullOrEmpty())
                                modelList.addAll(productDetails.fitsModel!!)

                            draw()
                        }
                    }
                }
            } catch (ex: Exception) {
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    binding.pbDownload.visibility = View.GONE
                }
            }
        }
    }

    private fun navigateToModel(item: FitsModel) {
        val vm = ViewModelProvider(requireActivity()).get(ShopViewModel::class.java)
        val converted = Model(
            barcode = item.barcode,
            fccPart = item.fccPart,
            imageType = item.imageType,
            imageUrl = item.imageUrl,
            manufacturer = item.manufacturer,
            linkedParts = 0,
            partDescription = item.partDescription,
            partNum = item.partNum,
            prodCode = item.prodCode,
            topLevel = item.topLevel
        )
        vm.shopModelResultSetMetaData = converted
        navigateToModel(converted)
    }

    private fun hideUnusedTabs() {

        if (technicalList.isNullOrEmpty())
            binding.tabLayout.getTabAt(0)?.view?.visibility = View.GONE

        if (modelList.isNullOrEmpty())
            binding.tabLayout.getTabAt(1)?.view?.visibility = View.GONE

        if (docs.isNullOrEmpty())
            binding. tabLayout.getTabAt(2)?.view?.visibility = View.GONE

        if(docs.isNullOrEmpty()&&technicalList.isNullOrEmpty()&&modelList.isNullOrEmpty())
            binding.tabLayout.visibility = View.INVISIBLE
    }

    private fun setTabToData() {


        if (technicalList.size > 0) {
            binding.tabLayout.getTabAt(0)?.select()
            switchViews(0)
            return
        }
        if (modelList.size > 0) {
            binding.tabLayout.getTabAt(1)?.select()
            switchViews(1)
            return
        }
        if (docs.size > 0) {
            binding.tabLayout.getTabAt(2)?.select()
            switchViews(2)
            return
        }
    }

    private fun initImages() {
        val is360 = setupImages()

        showHideDots(is360)

        if (is360) {
            binding.viewPager.visibility = View.VISIBLE
            binding.singleImageView.visibility = View.INVISIBLE
        } else {
            binding.viewPager.visibility = View.INVISIBLE
            binding.singleImageView.visibility = View.VISIBLE

            Helpers.renderImage(binding.singleImageView, product.images?.get(0)?.url)
        }

        binding.threeSixtyView.setOnClickListener {
            goTo360View(product)
        }

        binding.viewPager.adapter = MyViewPagerAdapter()
    }

    private fun showAddButtons(show: Boolean)
    {
        binding.addToBasketButton.visibility = if(show) View.VISIBLE else View.INVISIBLE
        binding.minusButton.visibility = if(show) View.VISIBLE else View.INVISIBLE
        binding.plusButton.visibility =  if(show) View.VISIBLE else View.INVISIBLE
        binding.qtyText.visibility =  if(show) View.VISIBLE else View.INVISIBLE
        binding.guidelineBottom.setGuidelinePercent(if (show) 0.85f else 1.0f)
    }

    private fun render(cartItem: CartItem) {
        var canBeAddedToBasket = false
        showAddButtons(canBeAddedToBasket)
        binding.priceWidget.visibility = View.INVISIBLE
        initImages()
        binding.nameText.text = cartItem.partDescription
        binding.manufacturerText.text = cartItem.mfrweb
        binding.partNumberText.text = cartItem.partNum
        binding.skuText.text = cartItem.fccPart
        binding.stockText.text = getString(R.string.stock_text, cartItem.stock)
        binding.descriptionText.text = cartItem.partDescription
        binding.categoryText.text = cartItem.classDescription

        binding.minusButton.setOnClickListener {
            updateQty(-1)
        }
        binding.plusButton.setOnClickListener {
            updateQty(1)
        }
        val sku = product.fccPart

        if (!sku.isNullOrBlank()) {
            val priceStock = App.globalData.getPriceStockFromMap(sku, tempQty)

            if (priceStock != null) {
                binding.priceWidget.renderCost(priceStock)
                canBeAddedToBasket = PriceHelper.renderPrices(
                    binding.stockText,
                    binding.checkedImageView,
                    priceStock,
                    cartItem.priceStatus
                )
            } else {
                getPriceStockEnquiry(cartItem)
            }
        }

        binding.qtyText.setText(tempQty.toString())

        if (isUpdate)
            binding.addToBasketButton.text = getString(R.string.update_basket)

        showAddButtons(canBeAddedToBasket)
        binding.addToBasketButton.setOnClickListener {
            if (canBeAddedToBasket)
                addItem(cartItem)
        }
    }

    private fun getPriceStockEnquiry(cartItem: CartItem?) {

        if (cartItem?.priceStatus == Settings.PriceStatus.GETTING_PRICES)
            return

        val sku = cartItem?.fccPart
        if (sku != null) {
            cartItem.priceStatus = Settings.PriceStatus.GETTING_PRICES
            binding.progress.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val priceStock = CustomerAPICalls.getPrice(sku, tempQty)
                    withContext(Dispatchers.Main) {
                        binding.progress.visibility = View.GONE
                        cartItem.priceStatus =
                            if (priceStock != null) Settings.PriceStatus.SUCCESS_GETTING_PRICES else Settings.PriceStatus.FAILED_GETTING_PRICES
                        render(cartItem)
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        cartItem.priceStatus = Settings.PriceStatus.FAILED_GETTING_PRICES
                        binding.progress.visibility = View.GONE
                        render(cartItem)
                    }
                }
            }
        }
    }


    private fun addItem(item: CartItem) {
        item.qty = tempQty
        cartViewModel.insert(item)
        mListener?.showSnackBarMessage(
            if (isUpdate) "Item updated in cart" else "Item added to cart",
            Snackbar.LENGTH_SHORT
        )
    }

    private fun setupImages(): Boolean {
        imageList = ArrayList()
        val url = product.images?.get(0)?.url?.toUpperCase(Locale.ENGLISH)
        val type = product.images?.get(0)?.type?.toUpperCase(Locale.ENGLISH)
        if(!url.isNullOrBlank()&&!type.isNullOrBlank()) {
            when {
                type.contains("SPIN60", true) -> {
                    imageList.add(V4APIHelper.getImage(product, "01"))
                    imageList.add(V4APIHelper.getImage(product, "16"))
                    imageList.add(V4APIHelper.getImage(product, "30"))
                    imageList.add(V4APIHelper.getImage(product, "45"))
                    return true
                }
                type.contains("SPIN40", true) -> {
                    imageList.add(V4APIHelper.getImage(product, "01"))
                    imageList.add(V4APIHelper.getImage(product, "11"))
                    imageList.add(V4APIHelper.getImage(product, "21"))
                    imageList.add(V4APIHelper.getImage(product, "31"))
                    return true
                }
                else -> {
                    imageList.add(V4APIHelper.getImage(product, false))
                    return false
                }
            }
        }
        else {
            imageList.add(V4APIHelper.getImage(product, false))
            return false
        }
    }

    private fun showHideDots(spin: Boolean) {
        if (spin) {
            binding.threeSixtyView.visibility = View.VISIBLE
            binding.dots.visibility = View.VISIBLE
        } else {
            binding.threeSixtyView.visibility = View.GONE
            binding.dots.visibility = View.GONE
        }
    }

    private fun updateQty(inc: Int) {
        if (inc < 0) {
            val q = tempQty
            if (q > 1) {
                tempQty = tempQty.dec()
            }
        } else {
            tempQty = tempQty.inc()

        }
        binding.qtyText.setText(tempQty.toString())
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    inner class ModelsAdapter : RecyclerView.Adapter<ModelsAdapter.ModelsViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ModelsViewHolder, position: Int) {
            val item = modelList[position]

            holder.itemView.setOnClickListener {
                navigateToModel(item)
            }

            holder.modelsBinding.manufacturerModelText.text = item.manufacturer

            Helpers.renderImage(holder.modelsBinding.modelImage, item.imageUrl)

            val cat = item.Categories?.get(0)
            val description = cat?.equipmentCategory?.get(0)?.description

            if (description.isNullOrEmpty())
                holder.modelsBinding.partIDModelText.text = item.partDescription
            else
                holder.modelsBinding.partDescriptionText.text = description
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.add_parts_model_item, parent, false)
            return ModelsViewHolder(view)
        }

        override fun getItemCount(): Int = modelList.size

        fun dataChanged() {
            if (modelList.isEmpty()) {
                showEmpty("No Models Found", "Try clearing your filters to broaden your search")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        inner class ModelsViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val modelsBinding = AddPartsModelItemBinding.bind(mView)
        }
    }

    inner class DocumentAdapter : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

        fun dataChanged() {

            if (docs.isEmpty()) {
                showEmpty("No Manuals", "No manuals available")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
            val item = docs[position]
            val docType = Helpers.documentTypesMap.getValue(item.docType ?: "")
            holder.docTypeBinding.subtitle.text = docType
            holder.docTypeBinding.viewed.visibility = View.GONE
            holder.docTypeBinding.viewed.text = getString(R.string.last_viewed)

            holder.docTypeBinding.image.setImageResource(
                Helpers.documentTypesImageMap.getValue(
                    item.docType ?: ""
                )
            )
            holder.itemView.setOnClickListener {
                val documentID: String = Helpers.getDocumentIDFromURL(item.url ?: "")
                lifecycleScope.launch(Dispatchers.IO) {
                    val documentV4 = V4APICalls.searchDocument(documentID)
                    val document = documentV4?.document?.get(0)
                    withContext(Dispatchers.Main) {
                        val linkedTo = document?.linkedTo?.get(0)
                        if (document != null && linkedTo != null)
                            checkBeforeDownload(document, linkedTo)
                    }
                }
            }
        }

        ///Downloading section
        private fun checkBeforeDownload(document: DocumentX, linkedTo: LinkedPart) {
            val docEntry = buildDocument(document, linkedTo)
            if (docEntry.isOnDevice) {
                DocumentManager.instance?.updateDocument(docEntry)
                showDocumentDownloadSuccess()
            } else {
                val mobile = isMobile()
                if (mobile || Settings.MOBILE_ONLY_CONNECTION_TEST) {
                    askBeforeDownloadIfLargeFile(docEntry)
                } else {
                    startDownLoad(docEntry)
                }
            }
        }

        private fun isMobile(): Boolean {
            return AppStatus.INTERNET_CONNECTED_MOBILE && !AppStatus.INTERNET_CONNECTED_WIFI || Settings.MOBILE_ONLY_CONNECTION_TEST
        }

        private fun buildDocument(document: DocumentX, linkedTo: LinkedPart): DocumentEntry {
            val docEntry = DocumentEntry()
            docEntry.v4Doc = document
            docEntry.linkedTo = linkedTo
            val man = uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer()
            val mod = uk.co.firstchoice_cs.core.api.legacyAPI.models.Model()
            man.Name = linkedTo.manufacturer
            mod.Name = linkedTo.partDescription

            docEntry.manufacturer = man
            docEntry.model = mod
            docEntry.document.address = document.url ?: ""
            return docEntry
        }


        private fun askBeforeDownloadIfLargeFile(docEntry: DocumentEntry) {
            lifecycleScope.launch(context = Dispatchers.IO) {
                val size = Helpers.getSizeOfDownload(docEntry)
                withContext(Dispatchers.Main) {
                    if (size < Settings.MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT && !Settings.MOBILE_ONLY_CONNECTION_TEST)
                        startDownLoad(docEntry)
                    else
                        showDownloadWarning(docEntry, size)
                }
            }
        }

        private fun showDownloadWarning(docEntry: DocumentEntry, size: Int) {
            val downloadMessage = String.format(
                getString(R.string.mobile_allowance_warning),
                Helpers.generateLengthString(size)
            )
            AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle("Mobile Data Warning")
                .setMessage(downloadMessage)
                .setPositiveButton(android.R.string.yes) { dialog, _ ->
                    startDownLoad(docEntry)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                    dialog.dismiss()
                }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }


        @SuppressLint("SetTextI18n")
        private fun startDownLoad(docEntry: DocumentEntry) {
            binding.downloadBottomSheet.expand()
            binding.downloadBottomSheet.startDownload(docEntry)

            docEntry.downloadID = PRDownloader.download(
                docEntry.url.replace(" ", "%20"),
                App.docDir,
                docEntry.file.name
            ).build()
                .setOnProgressListener {
                    docEntry.bytesDownloaded = it.currentBytes
                    docEntry.totalBytes = it.totalBytes
                    binding.downloadBottomSheet.setDocumentDownloadProgress(docEntry)
                }
                .setOnStartOrResumeListener {
                    docEntry.resetDownload()
                }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        val allList = DocumentManager.instance?.allDocuments
                        App.manualsCount.value = allList?.size ?: 0
                        binding.downloadBottomSheet.setComplete()
                        DocumentManager.instance?.updateDocument(docEntry)
                        downloadThumb(docEntry)
                    }

                    override fun onError(error: com.downloader.Error?) {
                        binding.downloadBottomSheet.setError()
                        binding.downloadBottomSheet.hide()
                    }
                })
        }

        private fun downloadThumb(docEntry: DocumentEntry) {
            val fileUrl = App.docDir + "/" + docEntry.file.name
            val file = java.io.File(fileUrl)
            ThumbNailManager.createPdfThumbnail(file)
            binding.downloadBottomSheet.hide()
            showDocumentDownloadSuccess()
            val manualsDetailsViewModel: ManualDetailsViewModel =
                ViewModelProvider(requireActivity()).get(ManualDetailsViewModel::class.java)
            manualsDetailsViewModel.doc = docEntry
            navigateToStore()
        }

        private fun showDocumentDownloadSuccess() {
            val inflater = requireActivity().layoutInflater
            val layout = inflater.inflate(
                R.layout.custom_toast_green,
                requireActivity().findViewById(R.id.llToast)
            )
            val t = Toast.makeText(requireContext(), "Downloaded to 'Manuals'", Toast.LENGTH_SHORT)
            t.view = layout
            t.show()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.document_types_bottom_sheet_item, parent, false)
            return DocumentViewHolder(view)
        }

        override fun getItemCount(): Int = docs.size

        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val docTypeBinding = DocumentTypesBottomSheetItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    inner class TechAdapter : RecyclerView.Adapter<TechAdapter.DocumentViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {


        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.tech_info_item, parent, false)
            return DocumentViewHolder(view)
        }

        fun dataChanged() {

            if (technicalList.isEmpty()) {
                showEmpty("No Tech Info", "No technical information available")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        override fun getItemCount(): Int = technicalList.size


        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val techBinder = TechInfoItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    inner class MyViewPagerAdapter : PagerAdapter() {

        override fun instantiateItem(collection: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(context)
            val layout = inflater.inflate(R.layout.pager_image, collection, false) as ViewGroup
            val pagerImage = layout.findViewById<ImageView>(R.id.pagerImage)
            val pb = layout.findViewById<ProgressBar>(R.id.pb)
            pb.visibility = View.GONE
            val image = imageList[position]

            if (image.isNotEmpty()) {
                pb.visibility = View.GONE
                Helpers.renderImage(pagerImage, if (image == "NOIMAGE") "" else image)
            }
            collection.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, view: Any) {
            container.removeView(view as View)
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view == `object`
        }

        override fun getCount(): Int {
            return imageList.size
        }
    }
}