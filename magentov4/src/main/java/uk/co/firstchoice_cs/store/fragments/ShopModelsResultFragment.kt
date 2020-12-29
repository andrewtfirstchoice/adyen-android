package uk.co.firstchoice_cs.store.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.view.View.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.App.Companion.globalData
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.Constants.BADGE_MAX_CHARACTERS
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.MOBILE_ONLY_CONNECTION_TEST
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.api.v4API.DocumentProd
import uk.co.firstchoice_cs.core.api.v4API.DocumentX
import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.generateLengthString
import uk.co.firstchoice_cs.core.helpers.Helpers.getDocumentIDFromURL
import uk.co.firstchoice_cs.core.helpers.Helpers.getSizeOfDownload
import uk.co.firstchoice_cs.core.helpers.PriceHelper
import uk.co.firstchoice_cs.core.helpers.V4APIHelper.is360
import uk.co.firstchoice_cs.core.helpers.V4APIHelper.linkedPartToPart
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.ThumbNailManager.createPdfThumbnail
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.DocumentTypesBottomSheetItemBinding
import uk.co.firstchoice_cs.firstchoice.databinding.PartsSearchResultBinding
import uk.co.firstchoice_cs.firstchoice.databinding.ShopModelsResultFragmentBinding
import uk.co.firstchoice_cs.store.vm.ShopViewModel
import java.io.File


class ShopModelsResultFragment : Fragment(R.layout.shop_models_result_fragment) {

    private var shopViewModel: ShopViewModel? = null
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var partList: ArrayList<LinkedPart?> = ArrayList()
    private var documentList: ArrayList<DocumentProd?> = ArrayList()

    private lateinit var itemDecor: DividerItemDecoration
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val partsAdapter = PartsAdapter()
    private val documentsAdapter = DocumentAdapter()
    private var tabMode = 0
    private lateinit var binding: ShopModelsResultFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_shop_models_results, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }


    private fun showEmpty(title: String) {
        binding.recycler.visibility = GONE
        binding.emptyLayout.root.visibility = VISIBLE
        binding.emptyLayout.emptyTitle.text = title
        binding.emptyLayout.emptyDescription.text = getString(R.string.clear_filters_message)
        binding.emptyLayout.emptyButton.visibility = GONE
    }

    private fun hideEmpty() {
        binding.emptyLayout.root.visibility = GONE
        binding.recycler.visibility = VISIBLE
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ShopModelsResultFragmentBinding.bind(view)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchViews(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        shopViewModel = ViewModelProvider(act).get(ShopViewModel::class.java)

        val model = shopViewModel?.shopModelResultSetMetaData

        binding.manufacturerModelText.text = model?.manufacturer
        if(!model?.topLevel.isNullOrEmpty())
            binding.partDescriptionText.text = model?.topLevel?.get(0)?.description.toString()
        binding.partIDModelText.text = model?.partDescription

        Helpers.renderModelImage(binding.modelImage,model)

        linearLayoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        itemDecor = DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL)
        setUpDownloadBottomSheet()
        binding.recycler.layoutManager = linearLayoutManager
        binding.recycler.addItemDecoration(itemDecor)
        setUpToolbar(view)
        binding.tabLayout.getTabAt(0)?.select()
        getData()
    }


    private fun setUpDownloadBottomSheet() {
        val sheetBehavior = BottomSheetBehavior.from( binding.downloadBottomSheet)
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


    private fun setUpToolbar(view: View) {
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }

    private fun switchViews(pos: Int) {
        tabMode = pos

        when (pos) {
            0 -> {
                showParts()
            }
            1 -> {
                showDocuments()
            }
        }
    }

    private fun getData() {

        binding.progress.visibility = VISIBLE
        val sku = shopViewModel?.shopModelResultSetMetaData?.fccPart
        if (sku != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val product = V4APICalls.product(sku, 0)
                    withContext(Dispatchers.Main) {
                        binding.progress.visibility = GONE
                        val prod = product?.product
                        val lp = prod?.get(0)?.linkedParts
                        val doc = prod?.get(0)?.documents
                        setData(lp, doc)
                    }
                } catch (ex: Exception) {
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        binding.progress.visibility = GONE
                    }
                }
            }
        }
    }

    private fun setData(linkedParts: List<LinkedPart>?, documents: List<DocumentProd>?) {
        if (!linkedParts.isNullOrEmpty()) {
            partList.addAll(linkedParts)
        }

        if (!documents.isNullOrEmpty()) {
            documentList.addAll(documents)
        }

        binding.tabLayout.getTabAt(0)?.orCreateBadge?.maxCharacterCount = BADGE_MAX_CHARACTERS
        binding.tabLayout.getTabAt(1)?.orCreateBadge?.maxCharacterCount = BADGE_MAX_CHARACTERS
        binding.tabLayout.getTabAt(0)?.orCreateBadge?.number = partList.size
        binding.tabLayout.getTabAt(1)?.orCreateBadge?.number = documentList.size

        if (documentList.isNullOrEmpty() && partList.isNullOrEmpty()) {
            showParts()
        } else if (partList.isNotEmpty()) {
            showParts()
        } else if (documentList.isNotEmpty()) {
            showDocuments()
        }
    }



    private fun showParts() {
        tabMode = 0
        binding.tabLayout.getTabAt(tabMode)?.select()
        binding.recycler.adapter = partsAdapter
        partsAdapter.dataChanged()
    }

    private fun showDocuments() {
        tabMode = 1
        binding.tabLayout.getTabAt(tabMode)?.select()
        binding.recycler.adapter = documentsAdapter
        documentsAdapter.dataChanged()
    }

    private fun refreshItemStatus(item: LinkedPart?)
    {
        val pos = partList.indexOf(item)
        if (pos != -1) {
            binding.recycler.adapter?.notifyItemChanged(pos)
        }
    }

    private fun getPriceStockEnquiry(item: LinkedPart?) {

        if (item?.priceStatus == Settings.PriceStatus.GETTING_PRICES)
            return

        val sku = item?.fccPart
        if (sku != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) {
                        item.priceStatus = Settings.PriceStatus.GETTING_PRICES
                        refreshItemStatus(item)
                    }
                    val price = CustomerAPICalls.getPrice(sku, 1)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        if (price != null)
                            item.priceStatus = Settings.PriceStatus.SUCCESS_GETTING_PRICES
                        else
                            item.priceStatus = Settings.PriceStatus.FAILED_GETTING_PRICES
                        refreshItemStatus(item)
                    }
                } catch (ex: Exception) {
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        item.priceStatus = Settings.PriceStatus.FAILED_GETTING_PRICES
                        refreshItemStatus(item)
                    }
                }
            }
        }
    }



    inner class PartsAdapter() : RecyclerView.Adapter<PartsAdapter.PartsViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: PartsViewHolder, position: Int) {
            val p = partList
            if (position >= p.size)
                return
            val item = p[position]

            holder.binding.discount.visibility = INVISIBLE
            holder.binding.oldPrice.visibility = INVISIBLE
            holder.binding.priceWidget.visibility = INVISIBLE
            holder.binding.progress.visibility = INVISIBLE
            val sku = item?.fccPart
            var priceStock:Product?=null
            if(sku!=null) {
                priceStock = globalData.getPriceStockFromMap(sku, 1)
                if (priceStock != null) {
                val cost = priceStock.customerCost?:0.0
                if(cost > 0) {
                    holder.binding.priceWidget.renderCost(priceStock)
                    PriceHelper.renderDiscount(priceStock, holder.binding.oldPrice,holder.binding.discount)
                }
                } else {
                    holder.binding.progress.visibility = VISIBLE
                    getPriceStockEnquiry(item)
                }
            }

            holder.binding.partIDText.text = item?.partNum
            holder.binding.progress.visibility = GONE

            if (item != null) {
                PriceHelper.renderPrices(holder.binding.inStock,holder.binding.tick,priceStock,item.priceStatus)
            }

            holder.binding.descriptionText.text = item?.partDescription
            Helpers.renderImage(holder.binding.image,item?.imageUrl)

            holder.binding.manufacturerText.text = item?.manufacturer

            PriceHelper.render360(holder.binding.threeSixtyIcon, is360(item))

            with(holder.itemView) {
                tag = item
                setOnClickListener {
                    if (item != null) {
                        Helpers.navigateToAddPartFragmentWithSuperC(linkedPartToPart(item), R.id.action_shopModelsResultFragment_to_addPartFragment, requireParentFragment())
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartsViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.parts_search_result, parent, false)
            return PartsViewHolder(view)
        }

        override fun getItemCount(): Int {
            return partList.size
        }

        fun dataChanged() {
            if(partList.isEmpty()) {
                showEmpty("No Parts Found")
            }
            else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        inner class PartsViewHolder internal constructor(view: View) : ViewHolder(view) {
            val binding:PartsSearchResultBinding = PartsSearchResultBinding.bind(view)
        }
    }


    inner class DocumentAdapter : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.document_types_bottom_sheet_item, parent, false)
            return DocumentViewHolder(view)
        }

        fun dataChanged() {
            if(documentList.isEmpty()) {
                showEmpty("No Manuals Found")
            }
            else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        override fun getItemCount(): Int = documentList.size

        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val docBinding = DocumentTypesBottomSheetItemBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }

        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {

            val item = documentList[position]
            val docType = Helpers.documentTypesMap.getValue(item?.docType?:"UK")
            holder.docBinding.subtitle.text = docType
            holder.docBinding.viewed.visibility = GONE
            holder.docBinding.viewed.text = getString(R.string.last_viewed)

            holder.docBinding.image.setImageResource(Helpers.documentTypesImageMap.getValue(item?.docType?:"UK"))
            holder.itemView.setOnClickListener {
                downloadManual(item)
            }
        }
    }

    ///Downloading section
    private fun checkBeforeDownload(document: DocumentX,linkedTo: LinkedPart) {
        val docEntry = buildDocument(document,linkedTo)
        if (docEntry.isOnDevice) {
            DocumentManager.instance?.updateDocument(docEntry)
            goToManualsDetails(docEntry)
        } else {
            val mobile = isMobile()
            if (mobile||MOBILE_ONLY_CONNECTION_TEST) {
                askBeforeDownloadIfLargeFile(docEntry)
            } else {
                startDownLoad(docEntry)
            }
        }
    }

    private fun isMobile(): Boolean {
        return AppStatus.INTERNET_CONNECTED_MOBILE && !AppStatus.INTERNET_CONNECTED_WIFI || MOBILE_ONLY_CONNECTION_TEST
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
        docEntry.document.address = document.url?:""
        return docEntry
    }


    private fun askBeforeDownloadIfLargeFile(docEntry: DocumentEntry) {
        lifecycleScope.launch(context = Dispatchers.IO) {
            val size = getSizeOfDownload(docEntry)
            if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                return@launch
            withContext(Dispatchers.Main) {
                if (size < Settings.MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT && !MOBILE_ONLY_CONNECTION_TEST)
                    startDownLoad(docEntry)
                else
                    showDownloadWarning(docEntry, size)
            }
        }
    }

    private fun showDownloadWarning(docEntry: DocumentEntry, size: Int) {
        val downloadMessage = String.format(getString(R.string.mobile_allowance_warning), generateLengthString(size))
        AlertDialog.Builder(ctx)
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
        binding.downloadBottomSheet.binding.statusTxt.text = "Downloading...."
        docEntry.downloading = true
        docEntry.downloadID = PRDownloader.download(docEntry.url.replace(" ", "%20"), App.docDir, docEntry.file.name).build()
                .setOnProgressListener {
                    docEntry.bytesDownloaded = it.currentBytes
                    docEntry.totalBytes = it.totalBytes
                }
                .setOnStartOrResumeListener {
                    docEntry.resetDownload()
                }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        val allList = DocumentManager.instance?.allDocuments
                        App.manualsCount.value = allList?.size?:0
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
        val file = File(fileUrl)
        createPdfThumbnail(file)
        binding.downloadBottomSheet.hide()
        showDocumentDownloadSuccess()
        goToManualsDetails(docEntry)
    }

    private fun goToManualsDetails(docEntry: DocumentEntry)
    {
        val manualsDetailsViewModel: ManualDetailsViewModel = ViewModelProvider(act).get(
            ManualDetailsViewModel::class.java)
        manualsDetailsViewModel.doc = docEntry
        NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_shopModelsResultFragment_to_manuals_details_fragment_store, null, null, null)
    }

    private fun showDocumentDownloadSuccess()
    {
        val inflater = act.layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast_green, act.findViewById(R.id.llToast))
        val t = Toast.makeText(act, "Downloaded to 'Manuals'", Toast.LENGTH_SHORT)
        t.view = layout
        t.show()
    }

    fun downloadManual(doc: DocumentProd?) {
        val url = doc?.url
        if(url!=null) {
            val documentID: String = getDocumentIDFromURL(url)
            lifecycleScope.launch(Dispatchers.IO) {
                val documentV4 = V4APICalls.searchDocument(documentID)
                val document = documentV4?.document?.get(0)
                withContext(Dispatchers.Main) {
                    val linkedTo = document?.linkedTo?.get(0)
                    if (document != null && linkedTo!=null)
                        checkBeforeDownload(document, linkedTo)
                }
            }
        }
    }
}