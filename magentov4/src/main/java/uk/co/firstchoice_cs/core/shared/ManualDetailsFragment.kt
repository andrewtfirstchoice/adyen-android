package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.View.OnFocusChangeListener
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ShareActionProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import kotlinx.android.synthetic.main.manual_details_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts.AlertResponse
import uk.co.firstchoice_cs.core.alerts.Alerts.partNotFoundAlert
import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpcrunch.HelpCrunch
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.documentTypesMap
import uk.co.firstchoice_cs.core.helpers.Helpers.setColorFilter
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.KeyWordMngr.SearchTerm
import uk.co.firstchoice_cs.core.shared.ManualDetailsFragment.AddPartsAdapter.PartViewHolder
import uk.co.firstchoice_cs.core.helpers.MappingHelper.mapProductToLinkedPart
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.ItemInCollectionsViewModel
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.RowPartsBinding
import java.util.*
import kotlin.collections.ArrayList


open class ManualDetailsFragment : Fragment(R.layout.manual_details_fragment), KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var menuItemAddCollection: MenuItem
    private lateinit var menuFav: MenuItem
    private lateinit var menuShare: MenuItem
    private var shareActionProvider: ShareActionProvider? = null
    private var mViewModel: ManualDetailsViewModel = ViewModelProvider(act).get(
        ManualDetailsViewModel::class.java)
    private var mAnalyticsViewModel = ViewModelProvider(act).get(AnalyticsViewModel::class.java)
    private val allParts: ArrayList<LinkedPart> = ArrayList()
    private val filterList: ArrayList<LinkedPart> = ArrayList()
    private lateinit var adapter: AddPartsAdapter
    private var selectedProduct: LinkedPart? = null
    private var loaded:Boolean?=null
    private lateinit var sheetBehavior: BottomSheetBehavior<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }



    override fun onPrepareOptionsMenu(menu: Menu) {
        val doc = mViewModel.doc
        if (doc != null) {
            menuFav.setIcon(if (doc.fav) R.drawable.icon_star_full else R.drawable.icon_star_empty)
            setMenuItemColor()
        }
        super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_items_pdf_fragment, menu)
        menuItemAddCollection = menu.findItem(R.id.action_fav)
        menuFav = menu.findItem(R.id.action_fav)
        menuShare = menu.findItem(R.id.action_share)
        // Fetch and store ShareActionProvider
        shareActionProvider = ShareActionProvider(this.context)
        setMenuItemColor()
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun setMenuItemColor() {
        DrawableCompat.setTint(menuItemAddCollection.icon, ContextCompat.getColor(ctx, R.color.white))
        DrawableCompat.setTint(menuFav.icon, ContextCompat.getColor(ctx, R.color.white))
        DrawableCompat.setTint(menuShare.icon, ContextCompat.getColor(ctx, R.color.white))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_collection -> addToCollection()
            R.id.action_fav -> addToFavourites()
            R.id.action_share -> share()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun share() {
        val doc = mViewModel.doc
        if (doc != null) {
            val docURI = FileProvider.getUriForFile(ctx, act.applicationContext.packageName + ".provider", doc.file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.putExtra(Intent.EXTRA_STREAM, docURI)
            share.putExtra(Intent.EXTRA_SUBJECT, "Sharing File from First Choice Group Ltd")
            share.type = "application/pdf"
            shareActionProvider?.setShareIntent(share)
            if (isAdded) act.startActivity(share)
        }
    }

    private fun exit() {
        NavHostFragment.findNavController(this@ManualDetailsFragment).navigateUp()
    }

    private fun addToCollection() {
        val doc = mViewModel.doc
        if (doc != null)
            showItemsInCollectionFragment(doc)
    }

    private fun showItemsInCollectionFragment(documentEntry: DocumentEntry) {
        val vm = ViewModelProvider(act).get(ItemInCollectionsViewModel::class.java)
        vm.collectionItem = null
        vm.focusedDocument = documentEntry
        gotoCollectionFragment()

    }

    open fun gotoCollectionFragment() {

    }

    private fun addToFavourites() {
        val doc = mViewModel.doc
        if (doc != null) {
            doc.fav = !doc.fav
            DocumentManager.instance?.updateDocument(doc)
            menuFav.setIcon(if (doc.fav) R.drawable.icon_star_full else R.drawable.icon_star_empty)
            setMenuItemColor()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mListener?.restoreFabState()
        initBottomSheet()
        initComponents()

        retry.setOnClickListener{ onRetryClicked()}

        empty.setOnClickListener { HelpCrunch.onChat()}

        val bottomBarClickListener = View.OnClickListener {
            when (sheetBehavior.state) {
                BottomSheetBehavior.STATE_COLLAPSED -> {
                    expand()
                }
                else -> {
                    collapse()
                }
            }
        }
        bs_toolbar.setNavigationOnClickListener(bottomBarClickListener)
        bs_toolbar.setOnClickListener(bottomBarClickListener)

        setUpToolbar()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@ManualDetailsFragment).navigateUp()
            }
        }
        act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        val doc = mViewModel.doc
        if (doc != null) {
            val b = Bundle()
            b.putString("manufacturer", doc.manufacturer.Name)
            b.putString("model", doc.model.Name)
            mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("ManualViewed", b)
            doc.lastSeen++
            DocumentManager.instance?.updateDocument(doc)

            try {
                pdfView.fromFile(doc.file)
                        .enableSwipe(true) // allows to block changing pages using swipe
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(false) // render annotations (such as comments, colors or forms)
                        .enableAntialiasing(true) // improve rendering a little bit on low-res screens
                        .spacing(0)
                        .autoSpacing(false) // add dynamic spacing to fit each page on its own on the screen
                        .pageSnap(true) // snap pages to screen boundaries
                        .pageFling(false) // make a fling change only a single page like ViewPager
                        .nightMode(false) // toggle night mode
                        .onRender {
                            linkedPartsBottomSheet.layoutParams.height = pdfView.height
                            searchParts()
                        }
                        .load()
            }
            catch (ex:Exception)
            {
                Toast.makeText(ctx,"Error viewing manual - please delete manual and re-download",Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onPause() {
        super.onPause()
        val doc = mViewModel.doc
        if (doc != null) {
            doc.v4Doc = null
        }
    }

    private fun initBottomSheet() {
        sheetBehavior = BottomSheetBehavior.from(linkedPartsBottomSheet)
        sheetBehavior.peekHeight = 170
        sheetBehavior.isHideable = true
        sheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
        sheetBehavior.addBottomSheetCallback(object : BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if(bs_toolbar==null)
                    return
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        bs_toolbar.setNavigationIcon(R.drawable.ic_arrow_up)
                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        bs_toolbar.setNavigationIcon(R.drawable.ic_arrow_down)
                    }
                    BottomSheetBehavior.STATE_HALF_EXPANDED -> {
                        bs_toolbar.setNavigationIcon(R.drawable.ic_arrow_down)
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {
                    }
                    BottomSheetBehavior.STATE_SETTLING -> {
                    }
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    private fun collapse() {
        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun expand() {
        sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    private fun setBottomHeaderText(numReturned: Int) {
        if(bs_toolbar!=null) {
            sheetBehavior.state = if (numReturned == 0) BottomSheetBehavior.STATE_HIDDEN else BottomSheetBehavior.STATE_COLLAPSED
            bs_toolbar.title = "Linked Products"
            bs_toolbar.subtitle = String.format("%s items", numReturned)
        }
    }

    private fun onRetryClicked() {
        if(AppStatus.INTERNET_CONNECTED)
        {
            error.visibility = View.GONE
            val part = selectedProduct?.fccPart
            if (part != null)
                getProductDetails(part)
        }
        else
        {
            error.visibility = View.VISIBLE
        }
    }

    private fun initComponents() {

        adapter = AddPartsAdapter(object : AddPartsAdapterInterface {
            override fun itemClicked(product: LinkedPart) {
                selectedProduct = product
                onRetryClicked()
            }
        })
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this.context)


        searchBar.onFocusChangeListener = OnFocusChangeListener { _: View?, _: Boolean -> }
        searchBar.setOnSearchInputRequest(object : ManualsSearchBarView.OnSearchInputListener {
            override fun onKeywordRequest(keyword: SearchTerm) {
                adapter.filterResults(keyword)
                adapter.sortFilteredAndNotifyDataSetChanged()
                setupPage()
            }

            override fun onSearchClicked() {}
            override fun onClear() {
                filterList.clear()
                filterList.addAll(allParts)
                adapter.sortFilteredAndNotifyDataSetChanged()
                setupPage()
            }
        })

        setupPage()
    }

    private fun setupPage() {
        if(recycler==null)
            return
        if (adapter.isEmpty) {
            recycler.visibility = View.GONE
            empty.visibility = View.VISIBLE
            searchBar.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            empty.visibility = View.GONE
            searchBar.visibility = View.VISIBLE
        }
    }

    private fun showPartNotFound() {
        partNotFoundAlert(object : AlertResponse {
            override fun processPositive(output: Any?) {
                partNotFoundAction()
            }

            override fun processNegative(output: Any?) {}
        })
    }

    open fun partNotFoundAction()
    {

    }



    private fun getProductDetails(part: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            progress.visibility = View.VISIBLE
        }
        lifecycleScope.launch(Dispatchers.IO) {
            val res = V4APICalls.product(part,0,200)
            val product = res?.product?.get(0)
            withContext(Dispatchers.Main) {
                progress.visibility = View.GONE
                if (product != null && toolbar!=null) {
                    loadProductPage(product)
                } else {
                    if (toolbar != null) {
                        showPartNotFound()
                    }
                }
            }
        }
    }

    open fun loadProductPage(product: Product) {

    }




    private fun getLinkedPartsV4(doc: DocumentEntry) {
        lifecycleScope.launch(Dispatchers.IO) {
            allParts.clear()
            val part = doc.linkedTo?.fccPart
            if (part != null) {
                val linkedParts = V4APICalls.searchLinkedParts(part)
                loaded = true
                if (mViewModel.doc?.linkedTo?.isModel == 1) {
                    if (linkedParts != null) {
                        linkedParts.product[0].linkedParts?.let { allParts.addAll(elements = it) }
                        withContext(Dispatchers.Main) {
                            updateAdapter()
                        }
                    }
                } else {
                    val prod = linkedParts?.product?.get(0)
                    if (prod != null) {
                        allParts.add(mapProductToLinkedPart(prod))
                        withContext(Dispatchers.Main) {
                            updateAdapter()
                        }
                    }
                }
            }
        }
    }


    private fun searchParts() {
        allParts.clear()
        val doc = mViewModel.doc
        if (doc != null) {
            if(doc.linkedTo!=null)
            {
                getLinkedPartsV4(doc)
            }
            else {
                //getLinkedPartsV3(doc)
            }
        }
    }

    private fun updateAdapter() {
        if(searchBar!=null) {
            adapter.updatePartList()
            val keyword = searchBar.searchTerm
            if (keyword != null)
                adapter.filterResults(keyword)
            adapter.sortFilteredAndNotifyDataSetChanged()
            setBottomHeaderText(allParts.size)
            setupPage()
        }
    }

    private fun setUpToolbar() {
        act.setSupportActionBar(toolbar)
        val doc = mViewModel.doc

        if (doc != null) {
           // toolbar.title = doc.manufacturer.Name + " " + doc.model.Name
            toolbar.title = doc.model.Name
            var documentType = "Parts Diagram"

            val v4Doc = doc.v4Doc

            if (v4Doc != null && !v4Doc.docType.isNullOrEmpty())
                documentType = documentTypesMap[v4Doc.docType].toString()
            val dType = doc.documentType
            if (dType !=null)
                documentType = documentTypesMap[dType].toString()

            toolbar.subtitle = documentType
            val icon = toolbar.overflowIcon
            if (icon != null) {
                setColorFilter(icon,Color.WHITE)
            }
            toolbar.setNavigationOnClickListener { exit() }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        showing = true
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        showing = false
        mListener = null
    }

    interface OnFragmentInteractionListener {
        fun restoreFabState()
    }

    inner class AddPartsAdapter internal constructor(callback: AddPartsAdapterInterface) : RecyclerView.Adapter<PartViewHolder>() {
        private val callback: AddPartsAdapterInterface

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PartViewHolder {
            //Inflate the layout, initialize the View Holder
            val v = LayoutInflater.from(parent.context).inflate(R.layout.row_parts, parent, false)
            return PartViewHolder(v)
        }

        fun filterResults(keyword: SearchTerm) {
            val searchWords = keyword.word.replace("\"", "").trim { it <= ' ' }
            val searchTerms = searchWords.split("\\s+").toTypedArray()
            filterList.clear()
            if (searchTerms.isEmpty()) {
                filterList.addAll(allParts)
                notifyDataSetChanged()
                return
            }
            else {
                for (searchWord in searchTerms) {
                    for (entry in allParts) {
                        val man = entry.manufacturer
                        val partDescription = entry.partDescription
                        val partNum = entry.partNum
                        if (!man.isNullOrBlank() && !partDescription.isNullOrBlank() && !partNum.isNullOrBlank()) {
                            if (partDescription.contains(searchWord, ignoreCase = true)
                                || man.contains(searchWord, ignoreCase = true)
                                || partNum.contains(searchWord, ignoreCase = true))
                            {
                                if (!filterList.contains(entry)) filterList.add(entry)
                            }
                        }
                    }
                }
                notifyDataSetChanged()
            }
        }

        override fun onBindViewHolder(holder: PartViewHolder, position: Int) {
            val currentPart = filterList[position]
            holder.binding.tvDescription.text = currentPart.partDescription
            holder.binding.tvManufacturer.text = currentPart.manufacturer?.capitalize(Locale.ROOT)
            holder.binding.tvSku.text = currentPart.partNum

            if(!currentPart.supersededPartNum.isNullOrEmpty()) {
                holder.binding.tvStock.text = getString(R.string.superseded_by,currentPart.supersededPartNum)
                holder.binding.tick.setImageResource(R.drawable.ic_baseline_cancel_24)
                holder.binding.tvStock.setTextColor(ContextCompat.getColor(ctx,R.color.fcRed))
            }
            else if(!currentPart.obsolete) {
                holder.binding.tvStock.text = getString(R.string.obsolete)
                holder.binding.tick.setImageResource(R.drawable.ic_baseline_cancel_24)
                holder.binding.tvStock.setTextColor(ContextCompat.getColor(ctx,R.color.fcRed))
            }

            else if(currentPart.stock==0) {
                holder.binding.tvStock.text = getString(R.string.available_on_order)
                holder.binding.tick.setImageResource(R.drawable.icon_orders)
                holder.binding.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.fcRed))
            }
            else {
                holder.binding.tvStock.text = getString(R.string.stock_text,currentPart.stock)
                holder.binding.tick.setImageResource(R.drawable.in_stock_tick)
                holder.binding.tvStock.setTextColor(ContextCompat.getColor(holder.itemView.context,R.color.green))
            }
            Helpers.renderImage(holder.binding.ivThumbnail,currentPart.imageUrl)

            holder.itemView.setOnClickListener { callback.itemClicked(currentPart) }
        }

        override fun getItemCount(): Int {
            return filterList.size
        }

        private fun stockSelector(p: LinkedPart): Int? = p.stock
        private fun manufacturerSKUSelector(p: LinkedPart): String? = p.partNum


        fun updatePartList() {
            filterList.clear()
            val allPartsMap = allParts.groupBy { stockSelector(it) }

            for ((_, v) in allPartsMap) {
                filterList.addAll(v.sortedBy { manufacturerSKUSelector(it) })
            }
        }

        fun sortFilteredAndNotifyDataSetChanged() {

            val sorted  = filterList.sortedWith(compareBy(

                    { it.obsolete },
                    { it.supersededFccPart },
                    { it.stock == 0 }
            )).sortedByDescending { it.stock }
            filterList.clear()
            filterList.addAll(sorted)
            notifyDataSetChanged()
        }

        val isEmpty: Boolean
            get() = filterList.isEmpty()

        inner class PartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var binding: RowPartsBinding = RowPartsBinding.bind(itemView)
        }

        init {
            filterList.clear()
            filterList.addAll(allParts)
            this.callback = callback
        }
    }

    internal interface AddPartsAdapterInterface {
        fun itemClicked(product: LinkedPart)
    }

    companion object {
        @kotlin.jvm.JvmField
        var showing: Boolean = false
    }
}