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
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.Constants.BADGE_MAX_CHARACTERS
import uk.co.firstchoice_cs.Constants.SEARCH_DATA
import uk.co.firstchoice_cs.Constants.SEARCH_DATA_2
import uk.co.firstchoice_cs.Constants.SEARCH_TERM
import uk.co.firstchoice_cs.Constants.SEARCH_TITLE
import uk.co.firstchoice_cs.Constants.SEARCH_TYPE
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.MOBILE_ONLY_CONNECTION_TEST
import uk.co.firstchoice_cs.core.api.customerAPI.CustomerAPICalls
import uk.co.firstchoice_cs.core.api.v4API.*
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.generateLengthString
import uk.co.firstchoice_cs.core.helpers.Helpers.getDocumentIDFromURL
import uk.co.firstchoice_cs.core.helpers.Helpers.getSizeOfDownload
import uk.co.firstchoice_cs.core.helpers.PriceHelper
import uk.co.firstchoice_cs.core.helpers.SafetyChecks.safeArg
import uk.co.firstchoice_cs.core.helpers.V4APIHelper
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.ThumbNailManager.createPdfThumbnail
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.DocumentsSearchResultCardBinding
import uk.co.firstchoice_cs.firstchoice.databinding.ModelsSearchResultCardBinding
import uk.co.firstchoice_cs.firstchoice.databinding.PartsSearchResultBinding
import uk.co.firstchoice_cs.firstchoice.databinding.ShopPartsResultFragmentBinding
import uk.co.firstchoice_cs.store.bottom_sheets.DocumentTypeBottomSheetInterface
import uk.co.firstchoice_cs.store.bottom_sheets.FilterBottomSheetInterface
import uk.co.firstchoice_cs.store.vm.ShopViewModel
import java.io.File
import java.net.URLEncoder
import kotlin.collections.ArrayList


class ShopPartsResultFragment : Fragment(R.layout.shop_parts_result_fragment),
    FilterBottomSheetInterface, DocumentTypeBottomSheetInterface {

    private val visibleThreshold = 10
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var stats: Stats? = null

    private var searchingParts: Boolean = false
    private var searchingDocuments: Boolean = false
    private var searchingModels = false
    private var searchingFreeText = false

    private var partList: ArrayList<Part> = ArrayList()
    private var modelList: ArrayList<Model> = ArrayList()
    private var documentList: ArrayList<Document> = ArrayList()

    private var partFilterList: ArrayList<Part> = ArrayList()
    private var modelFilterList: ArrayList<Model> = ArrayList()
    private var documentFilterList: ArrayList<Document> = ArrayList()

    private lateinit var itemDecor: DividerItemDecoration
    private lateinit var linearLayoutManager: LinearLayoutManager
    private val partsAdapter = PartsAdapter()
    private val documentsAdapter = DocumentAdapter()
    private val modelsAdapter = ModelsAdapter()
    private var searchTerm = ""
    private var searchType = ""
    private var searchData = ""
    private var searchData2 = ""
    private var searchTitle = ""
    private var searchTermEncoded = ""
    private var tabMode = 0
    private var topLevelFilterString = ""
    private var manufacturerFilterString = ""
    private var classFilterString = ""
    private var equipmentFilterString = ""

    private var searchTermEncodedStringSaved = ""
    private var topLevelFilterStringSaved = ""
    private var manufacturerFilterStringSaved = ""
    private var classFilterStringSaved = ""
    private var equipmentFilterStringSaved = ""
    private var modelStartIndex = 0
    private var documentStartIndex = 0
    private var partStartIndex = 0
    private var freeTextStartIndex = 0






    private lateinit var binding: ShopPartsResultFragmentBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ShopPartsResultFragmentBinding.bind(view)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchViews(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
            }
        })

        initEmpty()
        hideEmpty()
        linearLayoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        itemDecor = DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL)
        binding.recycler.layoutManager = linearLayoutManager
        binding.recycler.addItemDecoration(itemDecor)
        setUpToolbar(view)
        binding.tabLayout.getTabAt(tabMode)?.select()

        setUpDocTypeBottomSheet()
        setUpDownloadBottomSheet()
        setUpFilterBottomSheet()

        binding.toolbar.title = searchTitle
        setupScrollListener()
        binding.filterBottomSheet.initFilter(
                searchData,
                searchTerm,
                searchTermEncoded,
                searchTitle,
                searchType
            )
        startSearch()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        arguments?.let {
            searchData = safeArg(arguments, SEARCH_DATA)
            searchData2 = safeArg(arguments, SEARCH_DATA_2)
            searchTerm = safeArg(arguments, SEARCH_TERM)
            searchTermEncoded = URLEncoder.encode(searchTerm, "utf-8")
            searchType = safeArg(arguments, SEARCH_TYPE)
            searchTitle = safeArg(arguments, SEARCH_TITLE)

            when (searchType) {
                Constants.SEARCH_TYPE_TOP_LEVEL -> topLevelFilterString = searchData
                Constants.SEARCH_TYPE_MANUFACTURER -> manufacturerFilterString = searchData
                Constants.SEARCH_TYPE_EQUIPMENT_CATEGORY -> {
                    equipmentFilterString = searchData
                    topLevelFilterString = searchData2
                }
                Constants.SEARCH_TYPE_CLASS_ID -> classFilterString = searchData
            }

            saveSearchVariablesForRestore()
        }
    }

    private fun saveSearchVariablesForRestore() {
        manufacturerFilterStringSaved = manufacturerFilterString
        topLevelFilterStringSaved = topLevelFilterString
        equipmentFilterStringSaved = equipmentFilterString
        classFilterStringSaved = classFilterString
        searchTermEncodedStringSaved = searchTermEncoded
    }

    private fun restoreSearchVariables() {
        manufacturerFilterString = manufacturerFilterStringSaved
        topLevelFilterString = topLevelFilterStringSaved
        equipmentFilterString = equipmentFilterStringSaved
        classFilterString = classFilterStringSaved
        searchTermEncoded = searchTermEncodedStringSaved
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_shop_parts_results, menu)

        val filterItem = menu.findItem(R.id.action_filter)

        filterItem.setOnMenuItemClickListener {
            toggleFilterBottomSheet()
        }
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun toggleFilterBottomSheet(): Boolean {
        val sheetBehavior = BottomSheetBehavior.from(binding.filterBottomSheet)
        if(sheetBehavior.state==BottomSheetBehavior.STATE_COLLAPSED || sheetBehavior.state==BottomSheetBehavior.STATE_HIDDEN) {
            binding.filterBottomSheet.expand()
        }
        else
        {
            binding.filterBottomSheet.hide()
        }
        return true
    }


    private fun setUpDocTypeBottomSheet() {
        val sheetBehavior = BottomSheetBehavior.from(binding.bottomSheetDocType)
        sheetBehavior.peekHeight = 0
        sheetBehavior.isHideable = true
        binding.bottomSheetDocType.setCallback(this)
        binding.bottomSheetDocType.setSheetBehavior(sheetBehavior)
        binding.bottomSheetDocType.hide()

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                Helpers.logBottomSheetState("DocTypeBottomSheet", newState)
            }
        })
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

    private fun setUpFilterBottomSheet() {
        val sheetBehavior = BottomSheetBehavior.from(binding.filterBottomSheet)
        sheetBehavior.peekHeight = 0
        sheetBehavior.isHideable = true
        binding.filterBottomSheet.setCallback(this)
        binding.filterBottomSheet.setSheetBehavior(sheetBehavior)
        binding.filterBottomSheet.hide()

        sheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(newState==BottomSheetBehavior.STATE_EXPANDED)
                {
                    binding.disableTabs.visibility = VISIBLE
                }
                else if(newState==BottomSheetBehavior.STATE_COLLAPSED||newState==BottomSheetBehavior.STATE_HIDDEN)
                {
                    binding.disableTabs.visibility = GONE
                }
            }
        })
    }

    private fun showEmpty(title: String) {
        binding.emptyView.root.visibility = VISIBLE
        binding.emptyView.emptyTitle.text = title
        binding.emptyView.emptyDescription.text = getString(R.string.clear_filters_message)
        binding.emptyView.emptyButton.visibility = GONE
    }

    private fun hideEmpty() {
        binding.emptyView.root.visibility = GONE
    }

    private fun initEmpty() {
        binding.emptyView.emptyButton.visibility = GONE
    }




    private fun setupScrollListener() {
        val layoutManager = binding.recycler.layoutManager as LinearLayoutManager
        binding.recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val visibleItemCount = layoutManager.childCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                listScrolled(visibleItemCount, lastVisibleItem, totalItemCount)
            }
        })
    }

    fun listScrolled(visibleItemCount: Int, lastVisibleItemPosition: Int, totalItemCount: Int) {

        when (tabMode) {
            0 -> {
                if (visibleItemCount + lastVisibleItemPosition + visibleThreshold >= totalItemCount) {
                    searchParts(searchTermEncoded)
                }
            }
            1 -> {
                if (visibleItemCount + lastVisibleItemPosition + visibleThreshold >= totalItemCount) {
                    searchModels()
                }
            }
            2 -> {
                if (visibleItemCount + lastVisibleItemPosition + visibleThreshold >= totalItemCount) {
                    searchDocuments()
                }
            }
        }
    }

    private fun clearDecks() {
        stats = null

        modelStartIndex = 0
        documentStartIndex = 0
        partStartIndex = 0
        freeTextStartIndex = 0

        searchingParts = false
        searchingDocuments = false
        searchingModels = false
        searchingFreeText = false

        partList.clear()
        modelList.clear()
        documentList.clear()

        partFilterList.clear()
        modelFilterList.clear()
        documentFilterList.clear()

        partsAdapter.notifyDataSetChanged()
        documentsAdapter.notifyDataSetChanged()
        modelsAdapter.notifyDataSetChanged()
    }

    private fun startSearch() {

        if (stats == null) {
            if (searchType == V4APICalls.SearchTypeFreeText) {
                freeTextSearch()
            } else {
                binding.recycler.adapter = partsAdapter
                getParts()
            }
        } else {
            setBadges()
            switchViews(tabMode)
        }
    }

    private fun setUpToolbar(view: View) {
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }

    private fun switchViews(pos: Int) {
        lifecycleScope.launch(context = Dispatchers.Main) {
            tabMode = pos
            when (pos) {
                0 -> {
                    binding.recycler.adapter = partsAdapter
                    partsAdapter.dataChanged()
                    if (partList.isEmpty())
                        getParts()
                }
                1 -> {
                    binding.recycler.adapter = modelsAdapter
                    modelsAdapter.dataChanged()
                    if (modelList.isEmpty())
                        getModels()
                }
                2 -> {
                    binding.recycler.adapter = documentsAdapter
                    documentsAdapter.dataChanged()
                    if (documentList.isEmpty())
                        getDocuments()
                }
            }
        }
    }

    private fun setBadges() {
        val s = stats
        if (s != null) {
            binding.tabLayout.getTabAt(0)?.orCreateBadge?.maxCharacterCount = BADGE_MAX_CHARACTERS
            binding.tabLayout.getTabAt(1)?.orCreateBadge?.maxCharacterCount = BADGE_MAX_CHARACTERS
            binding.tabLayout.getTabAt(2)?.orCreateBadge?.maxCharacterCount = BADGE_MAX_CHARACTERS
            binding.tabLayout.getTabAt(0)?.orCreateBadge?.number = s.stats.parts
            binding.tabLayout.getTabAt(1)?.orCreateBadge?.number = s.stats.models
            binding.tabLayout.getTabAt(2)?.orCreateBadge?.number = s.stats.documents
        }
    }

    private fun clearBadges() {
        val s = stats
        if (s != null) {
            binding.tabLayout.getTabAt(0)?.orCreateBadge?.number = 0
            binding.tabLayout.getTabAt(1)?.orCreateBadge?.number = 0
            binding.tabLayout.getTabAt(2)?.orCreateBadge?.number = 0
        }
    }

    private fun getParts() {
        searchParts(searchTermEncoded)
        searchStats(searchTermEncoded)
    }

    private fun getDocuments() {
        searchDocuments()
    }

    private fun getModels() {
        searchModels()
    }

    private fun freeTextSearch() {
        if (searchingFreeText)
            return
        binding.toolbar.title = searchTerm
        lifecycleScope.launch(context = Dispatchers.IO) {
            showProgress()
            try {
                searchingFreeText = true
                val searchETL = V4APICalls.ncSearchETL(searchTerm, 0)

                freeTextStartIndex++
                searchingFreeText = false

                searchTermEncoded = searchETL?.search ?: ""
                manufacturerFilterString = searchETL?.manufacturer ?: ""
                equipmentFilterString = searchETL?.equipmentCategory ?: ""
                classFilterString = searchETL?.classId ?: ""
                searchStats(searchTermEncoded)
                switchViews(tabMode)
                hideProgress()

            } catch (ex: Exception) {
                searchingFreeText = false
                hideProgress()
            }
        }
    }


    private fun searchModels() {
        if (searchingModels)
            return
        val numModels = stats?.stats?.models
        if (numModels != null && modelList.size >= numModels)
            return
        lifecycleScope.launch(context = Dispatchers.IO) {
            showProgress()
            try {
                searchingModels = true
                val ncModels = V4APICalls.ncModels(
                    manufacturerFilterString,
                    equipmentFilterString,
                    classFilterString,
                    topLevelFilterString,
                    searchTermEncoded,
                    modelStartIndex
                )
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    modelStartIndex++
                    searchingModels = false
                    hideProgress()
                    binding.progress.visibility = GONE
                    val p = ncModels?.models
                    if (p != null) {
                        modelList.addAll(p)
                    }
                    modelsAdapter.dataChanged()
                }
            } catch (ex: Exception) {
                searchingModels = false
                hideProgress()
            }
        }
    }

    private fun searchDocuments() {
        if (searchingDocuments)
            return
        val numDocuments = stats?.stats?.documents
        if (numDocuments != null && documentList.size >= numDocuments)
            return
        lifecycleScope.launch(context = Dispatchers.IO) {
            try {
                showProgress()
                searchingDocuments = true
                val ncDocuments = V4APICalls.ncDocuments(
                    manufacturerFilterString,
                    equipmentFilterString,
                    classFilterString,
                    topLevelFilterString,
                    searchTermEncoded,
                    documentStartIndex
                )
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    documentStartIndex++
                    searchingDocuments = false
                    hideProgress()
                    val p = ncDocuments?.documents
                    if (p != null) {
                        documentList.addAll(p)
                    }
                    documentsAdapter.dataChanged()
                }
            } catch (ex: Exception) {
                searchingDocuments = false
                hideProgress()
            }
        }
    }

    private suspend fun showProgress() {
        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
            return
        withContext(Dispatchers.Main) {
            binding.progress.visibility = VISIBLE
        }
    }

    private suspend fun hideProgress() {
        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
            return
        withContext(Dispatchers.Main) {
            binding.progress.visibility = GONE
        }
    }

    private fun searchStats(search: String) {
        binding.progress.visibility = VISIBLE
        lifecycleScope.launch(context = Dispatchers.IO) {
            try {
                stats = V4APICalls.ncTabs(
                    manufacturerFilterString,
                    equipmentFilterString,
                    classFilterString,
                    topLevelFilterString,
                    search,
                    0
                )
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    setBadges()
                }
            } catch (ex: Exception) {
                hideProgress()
            }
        }
    }

    private fun searchParts(search: String) {
        if (searchingParts)
            return
        val numParts = stats?.stats?.parts
        if (numParts != null && partList.size >= numParts)
            return
        binding.progress.visibility = VISIBLE
        lifecycleScope.launch(context = Dispatchers.IO) {
            try {
                searchingParts = true
                val ncParts = V4APICalls.ncSearch(
                    manufacturerFilterString,
                    equipmentFilterString,
                    classFilterString,
                    topLevelFilterString,
                    search,
                    partStartIndex
                )
                hideProgress()
                    partStartIndex++
                    searchingParts = false
                binding.progress.visibility = GONE
                val p = ncParts?.parts
                if (p != null) {
                    partList.addAll(p)
                }
                withContext(Dispatchers.Main) {
                    partsAdapter.dataChanged()
                }
            } catch (ex: Exception) {
                searchingParts = false
                hideProgress()
            }
        }
    }
    private suspend fun refreshItemStatus(item: Part?,priceStatus: Settings.PriceStatus) {
        if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
            return
        withContext(Dispatchers.Main) {
            item?.priceStatus = priceStatus
            val pos = partFilterList.indexOf(item)
            if (pos != -1) {
                binding.recycler.adapter?.notifyItemChanged(pos)
            }
        }
    }

    fun getPriceStockEnquiry(item: Part?, qty: Int) {

        if (item?.priceStatus == Settings.PriceStatus.GETTING_PRICES)
            return

        val sku = item?.fccPart
        if (sku != null) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    refreshItemStatus(item,Settings.PriceStatus.GETTING_PRICES)
                    val price = CustomerAPICalls.getPrice(sku, qty)
                    refreshItemStatus(item,if(price!=null) Settings.PriceStatus.SUCCESS_GETTING_PRICES else Settings.PriceStatus.FAILED_GETTING_PRICES )
                } catch (ex: Exception) {
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        refreshItemStatus(item,Settings.PriceStatus.FAILED_GETTING_PRICES)
                    }
                }
            }
        }
    }


    inner class PartsAdapter : RecyclerView.Adapter<PartsAdapter.PartsViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: PartsViewHolder, position: Int) {
            val p = partFilterList
            if (position >= p.size)
                return
            val item = p[position]

            holder.binding.discount.visibility = INVISIBLE
            holder.binding.oldPrice.visibility = INVISIBLE
            holder.binding.priceWidget.visibility = INVISIBLE
            holder.binding.partIDText.text = item.partNum
            holder.binding.descriptionText.text = item.partDescription
            holder.binding.manufacturerText.text = item.manufacturer

            //show the progress bar if the item status is getting prices
            if (item.priceStatus == Settings.PriceStatus.GETTING_PRICES) {
                holder.binding.progress.visibility = VISIBLE
            } else {
                holder.binding.progress.visibility = GONE
            }
            val fccPart = item.fccPart

            val priceStock = fccPart?.let { globalData.getPriceStockFromMap(it, 1) }
            if (priceStock != null) {
                val cost = priceStock.customerCost ?: 0.0
                if (cost > 0) {
                    holder.binding.priceWidget.renderCost(priceStock)
                    PriceHelper.renderDiscount(priceStock, holder.binding.oldPrice, holder.binding.discount)
                }
            } else {
                getPriceStockEnquiry(item, 1)
            }

            PriceHelper.renderPrices(holder.binding.inStock, holder.binding.tick, priceStock, item.priceStatus)

            PriceHelper.render360(holder.binding.threeSixtyIcon, V4APIHelper.is360(item))

            Helpers.renderImage(holder.binding.image, item.images?.get(0)?.url)

            with(holder.itemView) {
                tag = item
                setOnClickListener {

                    Helpers.navigateToAddPartFragmentWithSuperC(
                        item,
                        R.id.action_shopPartsResultFragment_to_addPartFragment_store,
                        requireParentFragment()
                    )
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.parts_search_result, parent, false)
            return PartsViewHolder(view)
        }

        override fun getItemCount(): Int {
            return partFilterList.size
        }

        fun dataChanged() {
            partFilterList.clear()
            partFilterList.addAll(partList)
            if (partFilterList.isEmpty()) {
                showEmpty("No Parts Found")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        inner class PartsViewHolder internal constructor(view: View) : ViewHolder(view) {
            val binding:PartsSearchResultBinding = PartsSearchResultBinding.bind(view)
        }
    }

    inner class ModelsAdapter : RecyclerView.Adapter<ModelsAdapter.ModelsViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: ModelsViewHolder, position: Int) {
            val model = modelFilterList[position]

            holder.itemView.setOnClickListener {
                goToModelResultsFragment(model)
            }

            Helpers.renderModelImage(holder.modelBinding.modelImage,model)

            holder.modelBinding.manufacturerModelText.text = model.manufacturer
            holder.modelBinding.partIDModelText.text = model.partDescription
            holder.modelBinding.partDescriptionText.text = model.topLevel?.get(0)?.description
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelsViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.models_search_result_card, parent, false)
            return ModelsViewHolder(view)
        }

        override fun getItemCount(): Int = modelFilterList.size

        fun dataChanged() {
            modelFilterList.clear()
            modelFilterList.addAll(modelList)
            if (modelFilterList.isEmpty()) {
                showEmpty("No Models Found")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        inner class ModelsViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val modelBinding = ModelsSearchResultCardBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    inner class DocumentAdapter : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
            val item = documentFilterList[position]
            holder.docBinding.manufacturerModelText.text = item.manufacturer


            Helpers.renderDocumentImage(holder.docBinding.modelImage, item)

            holder.docBinding.partDescriptionText.text = item.partDescription
            holder.docBinding.partIDModelText.text = item.partNum
            when {
                item.files.isNullOrEmpty() -> holder.docBinding.numDiagrams.text = "0 Documents"
                item.files.size == 1 -> holder.docBinding.numDiagrams.text = "${item.files.size} Document"
                else -> holder.docBinding.numDiagrams.text = "${item.files.size} Documents"
            }

            holder.itemView.setOnClickListener {
                binding.bottomSheetDocType.expand(item)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.documents_search_result_card, parent, false)
            return DocumentViewHolder(view)
        }

        fun dataChanged() {
            documentFilterList.clear()
            documentFilterList.addAll(documentList)
            if (documentFilterList.isEmpty()) {
                showEmpty("No Manuals Found")
            } else {
                hideEmpty()
                notifyDataSetChanged()
            }
        }


        override fun getItemCount(): Int = documentFilterList.size

        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val docBinding = DocumentsSearchResultCardBinding.bind(mView)
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    override fun downloadManual(doc: Document?, fileX: uk.co.firstchoice_cs.core.api.v4API.File) {
        val documentID: String = getDocumentIDFromURL(fileX.url)
        lifecycleScope.launch(Dispatchers.IO) {
            val documentV4 = V4APICalls.searchDocument(documentID)
            val document = documentV4?.document?.get(0)
            if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                return@launch
            withContext(Dispatchers.Main) {
                if (doc != null) {
                    val linkedTo = document?.linkedTo?.get(0)
                    if (linkedTo != null)
                        checkBeforeDownload(document, linkedTo)
                }
            }
        }
    }
    override fun cancelFilters(){
        restoreSearchVariables()
        clearBadges()
        clearDecks()
        startSearch()
    }


    override fun applyFilters(
        selectedFiltersMan: java.util.ArrayList<Manufacturer>,
        selectedFiltersClass: java.util.ArrayList<Clas>,
        selectedFiltersEq: java.util.ArrayList<EquipmentCategory>
    ) {
        if(manufacturerFilterStringSaved.isEmpty())
            manufacturerFilterString = ""
        if(classFilterStringSaved.isEmpty())
            classFilterString = ""
        if(equipmentFilterStringSaved.isEmpty())
            equipmentFilterString = ""

        for (man in selectedFiltersMan) {
            manufacturerFilterString += man.prodCode + ","
        }
        for (cl in selectedFiltersClass) {
            classFilterString += cl.id + ","
        }
        for (eq in selectedFiltersEq) {
            equipmentFilterString += eq.id + ","
        }

        clearBadges()
        clearDecks()
        startSearch()
    }


    ///Downloading section
    private fun checkBeforeDownload(document: DocumentX, linkedTo: LinkedPart) {
        val docEntry = buildDocument(document, linkedTo)
        if (docEntry.isOnDevice) {
            DocumentManager.instance?.updateDocument(docEntry)
            showDocumentDownloadSuccess()
        } else {
            val mobile = isMobile()
            if (mobile || MOBILE_ONLY_CONNECTION_TEST) {
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
        docEntry.document.address = document.url ?: ""
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
        val downloadMessage =
            String.format(getString(R.string.mobile_allowance_warning), generateLengthString(size))
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
        val file = File(fileUrl)
        createPdfThumbnail(file)
        binding.downloadBottomSheet.hide()
        showDocumentDownloadSuccess()
        goToManualsDetails(docEntry)
    }

    private fun goToModelResultsFragment(model: Model) {
        val vm = ViewModelProvider(act).get(ShopViewModel::class.java)
        vm.shopModelResultSetMetaData = model
        NavHostFragment.findNavController(requireParentFragment()).navigate(
            R.id.action_shopPartsResultFragment_to_shopModelsResultFragment_store,
            null,
            null,
            null
        )
    }

    private fun goToManualsDetails(docEntry: DocumentEntry) {
        val manualsDetailsViewModel: ManualDetailsViewModel =
            ViewModelProvider(act).get(ManualDetailsViewModel::class.java)
        manualsDetailsViewModel.doc = docEntry
        NavHostFragment.findNavController(requireParentFragment()).navigate(
            R.id.action_shopPartsResultFragment_to_manuals_details_fragment_store,
            null,
            null,
            null
        )
    }

    private fun showDocumentDownloadSuccess() {
        val inflater = act.layoutInflater
        val layout = inflater.inflate(R.layout.custom_toast_green, act.findViewById(R.id.llToast))
        val t = Toast.makeText(act, "Downloaded to 'Manuals'", Toast.LENGTH_SHORT)
        t.view = layout
        t.show()
    }


}