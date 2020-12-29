package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.FCRepository.CollectionListInterface
import uk.co.firstchoice_cs.core.database.entities.CollectionItem
import uk.co.firstchoice_cs.core.database.entities.CollectionList
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.documentTypesMap
import uk.co.firstchoice_cs.core.helpers.Helpers.renderLastViewed
import uk.co.firstchoice_cs.core.helpers.SwipeCallback
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.ManualsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.ManualsFragmentBinding
import uk.co.firstchoice_cs.firstchoice.databinding.RowCollectionsBinding
import uk.co.firstchoice_cs.firstchoice.databinding.RowDocumentsBinding
import java.util.*
import kotlin.collections.ArrayList


open class ManualsFragment : Fragment(R.layout.manuals_fragment), KoinComponent, SearchView.OnQueryTextListener {
    private var addButton: MenuItem? = null
    private lateinit var menuItem: MenuItem
    private lateinit var searchView: SearchView
    private var searchEditText: EditText? = null
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    private var mode = ALL_MODE
    lateinit var mAnalyticsViewModel: AnalyticsViewModel
    private lateinit var manualsAdapter: ManualsFragmentAdapter
    private lateinit var collectionsAdapter: CollectionsAdapter
    private lateinit var mDBViewModel: DBViewModel
    private lateinit var mViewModel: ManualsViewModel
    private var showAdd = false
    private var collectionLists: List<CollectionList> = ArrayList()
    private var collectionItems: List<CollectionItem> = ArrayList()
    private var filterString:String = ""
    private lateinit var binding: ManualsFragmentBinding


    open fun showAddCollectionsFragment() {}
    open fun showSearchFragment() {}
    open fun launchDocument(doc: DocumentEntry) {}
    open fun showItemsInCollectionFragment(collectionItem: CollectionItem?, documentEntry: DocumentEntry) {}
    open fun showCollectionView(collectionList: CollectionList) {}


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ManualsFragmentBinding.bind(view)
        init()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }


    private fun observeCollections() {
        mDBViewModel.collectionItems?.observe(viewLifecycleOwner , { cit->
            this.collectionItems = cit
            mDBViewModel.collectionLists?.observe(viewLifecycleOwner, {
                this.collectionLists = it
                if(mode == COLLECTIONS_MODE)
                {
                    collectionsAdapter.dataChanged()
                }
            })
        })
    }

    private fun init() {
        lifecycleScope.launch(Dispatchers.Main) {
            initViewModels()
            mListener?.restoreFabState()
            initRecycler()
            observeCollections()
            manualsAdapter = ManualsFragmentAdapter()
            collectionsAdapter = CollectionsAdapter()
            setupTabs(binding.tabLayout)
            switchViews(mViewModel.tabPos)
            setUpToolbar()

            binding.emptyView.emptyButton.setOnClickListener {
                if (mode == ALL_MODE) {
                    if (AppStatus.INTERNET_CONNECTED)
                        showSearchFragment()
                } else if (mode == COLLECTIONS_MODE)
                    showAddCollectionsFragment()
            }
            mListener?.setSlider(1)
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    NavHostFragment.findNavController(this@ManualsFragment).navigateUp()
                }
            }
            act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

            binding.headerSearchButton.setOnClickListener {
                if (AppStatus.INTERNET_CONNECTED)
                    showSearchFragment()
                else
                    Alerts.showNoInternetToast()
            }
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(mViewModel.tabPos))
        }
        binding.emptyView.root.visibility = View.GONE
    }


    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_manuals_fragment, menu)
        DrawableCompat.setTint(menu.getItem(1).icon, ContextCompat.getColor(act, R.color.white))
        menuItem = menu.findItem(R.id.action_search)
        searchView = menuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setOnSearchClickListener{
            addButton?.isVisible = false
        }
        searchEditText = searchView.findViewById<View>(R.id.search_src_text) as EditText
        searchEditText?.setTextColor(ContextCompat.getColor(ctx, R.color.white))
        searchEditText?.setHintTextColor(ContextCompat.getColor(ctx, R.color.fcLightGrey))
        searchEditText?.hint = "Filter items"
        val searchButton: ImageView = searchView.findViewById(R.id.search_button)
        searchButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_search_white))

        val searchCloseButton: ImageView = searchView.findViewById(R.id.search_close_btn)
        searchCloseButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_close_white_24dp))

        searchCloseButton.setOnClickListener {
            showAdd(showAdd)
            filterString = ""
            searchView.setQuery(filterString, false)
            searchView.onActionViewCollapsed()
            if (mode == ALL_MODE || mode == FAV_MODE) {
                manualsAdapter.clearFilterList()
                manualsAdapter.filterResults("")
                manualsAdapter.notifyDataSetChanged()
            } else {
                collectionsAdapter.filterResults("")
                collectionsAdapter.notifyDataSetChanged()
            }

            if(mode == ALL_MODE)
                mViewModel.manualsFilter = ""
            if(mode == FAV_MODE)
                mViewModel.favFilter = ""
            if(mode == COLLECTIONS_MODE)
                mViewModel.collectionsFilter = ""
        }
        super.onCreateOptionsMenu(menu, menuInflater)
    }



    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            if(mode == ALL_MODE || mode == FAV_MODE) {
                filterString = newText
                if(mode== ALL_MODE)
                    mViewModel.manualsFilter = filterString
                else
                    mViewModel.favFilter = filterString

                manualsAdapter.filterResults(filterString)
                manualsAdapter.notifyDataSetChanged()
            }
            else
            {
                filterString = newText
                mViewModel.collectionsFilter = filterString
                collectionsAdapter.filterResults(filterString)
                collectionsAdapter.notifyDataSetChanged()
            }
        }
        return true
    }


    private fun initViewModels() {
        mAnalyticsViewModel = ViewModelProvider(act).get(AnalyticsViewModel::class.java)
        mDBViewModel = ViewModelProvider(act).get(DBViewModel::class.java)
        mViewModel = ViewModelProvider(act).get(ManualsViewModel::class.java)
    }

    private fun setupTabs(tabLayout: TabLayout) {
        TabAdapter()
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                switchViews(tab.position)

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun switchViews(pos: Int) {
        mViewModel.tabPos = pos
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(mViewModel.tabPos))
        when (pos) {
            0 -> {
                mode = ALL_MODE
                initEmptyManualsNoFilter()
                showAdd(false)
                binding.recycler.adapter = manualsAdapter
                manualsAdapter.dataChanged()

                if(mViewModel.manualsFilter.isNotEmpty()) {
                    resetQuery(mViewModel.manualsFilter)
                }
            }
            1 -> {
                mode = FAV_MODE
                initEmptyFavManualsNoFilter()
                showAdd(false)
                binding.recycler.adapter = manualsAdapter
                manualsAdapter.dataChanged()

                if(mViewModel.favFilter.isNotEmpty()) {
                    resetQuery(mViewModel.favFilter)
                }
            }
            2 -> {
                mode = COLLECTIONS_MODE
                initEmptyCollectionsNoFilter()
                showAdd(true)
                binding.recycler.adapter = collectionsAdapter
                collectionsAdapter.dataChanged()

                if(mViewModel.collectionsFilter.isNotEmpty()) {
                    resetQuery(mViewModel.collectionsFilter)
                }
            }
        }
    }

    private fun resetQuery(query: String?)
    {
        Handler().postDelayed({
            searchView.isIconified = false // Expand it
            searchView.setQuery(query, true)
        }, 100)
    }


    private fun initEmptyManualsNoFilter()
    {
        initEmpty("Start Searching", "View manuals to save them to your device", R.drawable.icon_manual_download, "You have no downloaded manuals")
    }

    private fun initEmptyManualsWithFilter()
    {
        initEmpty("", "No Filtered Results", R.drawable.icon_manual_download, "Manuals")
    }

    private fun initEmptyFavManualsNoFilter()
    {
        initEmpty("", "Tap the star on the manuals to save them to your favourites list", R.drawable.icon_favourites, "You have no favourite manuals")
    }

    private fun initEmptyFavManualsWithFilter()
    {
        initEmpty("", "No Filtered Results", R.drawable.icon_favourites, "Favourites")
    }

    private fun initEmptyCollectionsNoFilter()
    {
        initEmpty("Start Creating", "Create collections of manuals for jobs or frequently used parts", R.drawable.icon_collections, "You have no manuals collections")
    }

    private fun initEmptyCollectionsWithFilter()
    {
        initEmpty("", "No Filtered Results", R.drawable.icon_collections, "Collections")
    }


    fun showHideEmptyView(stateIsEmpty: Boolean) {
        if (stateIsEmpty) {
            binding.emptyView.root.visibility = View.VISIBLE

            if(mode == FAV_MODE)
            {
                if(filterString.isBlank())
                    initEmptyFavManualsNoFilter()
                else
                    initEmptyFavManualsWithFilter()
            }

            if(mode == ALL_MODE)
            {
                if(filterString.isBlank())
                    initEmptyManualsNoFilter()
                else
                    initEmptyManualsWithFilter()
            }

            if(mode == COLLECTIONS_MODE)
            {
                if(filterString.isBlank())
                    initEmptyCollectionsNoFilter()
                else
                    initEmptyCollectionsWithFilter()
            }

        } else {
            binding.emptyView.root.visibility = View.GONE
        }
        showHideHeaderView(stateIsEmpty)
    }

    private fun showHideHeaderView(stateIsEmpty: Boolean) {
        if (mode == ALL_MODE && !stateIsEmpty) {
            binding.catHeader.visibility = View.VISIBLE
        }
        else {
            binding.catHeader.visibility = View.GONE
        }
    }

    private fun initRecycler() {
        binding.recycler.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })
        binding.recycler.addItemDecoration(DividerItemDecoration(act, DividerItemDecoration.VERTICAL))
        val linearLayoutManager = LinearLayoutManager(this.context)
        binding.recycler.layoutManager = linearLayoutManager
        val swipeCallBack: ItemTouchHelper.SimpleCallback = SwipeDeleteCallback()
        val helper = ItemTouchHelper(swipeCallBack)
        helper.attachToRecyclerView(binding.recycler)
    }



    override fun onPrepareOptionsMenu(menu: Menu) {
        addButton = menu.findItem(R.id.action_add_collection)
        menu.findItem(R.id.action_add_collection).isVisible = showAdd
        super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // action with ID action_refresh was selected
        if (item.itemId == R.id.action_add_collection) {
            showAddCollectionsFragment()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }



    private fun initEmpty(emptyButtonStr: String?, emptyDescriptionStr: String, emptyImageResource: Int, emptyTitleStr: String) {
        if(emptyButtonStr.isNullOrBlank())
            binding.emptyView.emptyButton.visibility = View.GONE
        else
            binding.emptyView.emptyButton.visibility = View.VISIBLE
        binding.emptyView.emptyButton.text = emptyButtonStr
        binding.emptyView.emptyDescription.text = emptyDescriptionStr
        binding.emptyView.emptyImage.setImageResource(emptyImageResource)
        binding.emptyView.emptyTitle.text = emptyTitleStr

    }


    private fun showAdd(show: Boolean) {
        showAdd = show
        act.invalidateOptionsMenu()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
    }


    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun restoreFabState()
        fun onScrollUp()
        fun hideNavBar()
        fun showNavBar()
        fun setSlider(pos: Int)
    }

    inner class CollectionsAdapter : RecyclerView.Adapter<CollectionsAdapter.CollectionHolder>() {

        private val mInflater: LayoutInflater = LayoutInflater.from(this@ManualsFragment.context)
        private val filterList: MutableList<CollectionList> = ArrayList()

        fun dataChanged()
        {
            filterResults(mViewModel.collectionsFilter)
        }

        fun filterResults(searchWords: String) {
            val searchTerms = searchWords.split("\\s+").toTypedArray()
            filterList.clear()
            if (searchTerms.isEmpty()) {
                filterList.clear()
                filterList.addAll(collectionLists)
                dataChanged()
                return
            }
            for (searchWord in searchTerms) {
                for (entry in collectionLists) {
                    if (entry.name.contains(searchWord, ignoreCase = true)) {
                        if (!filterList.contains(entry)) filterList.add(entry)
                    }
                }
            }
            filterList.groupBy {sortByName(it) }
            buildHeader(filterList)
            notifyDataSetChanged()
            showHideEmptyView(filterList.isEmpty())
        }

        private fun sortByName(collectionItem: CollectionList): String = collectionItem.name.toUpperCase(Locale.ROOT)


        private fun numItemsByCollection(collectionId: Int): Int {
            var count = 0
            for (item in collectionItems) {
                if (item.collectionId == collectionId) count++
            }
            return count
        }

        override fun getItemId(position: Int): Long {
            return if (position >= collectionLists.size) -1 else collectionLists[position].id.toLong()
        }

        override fun getItemCount(): Int {
            return filterList.size
        }

        private fun getItem(position: Int): CollectionList {
            return filterList[position]
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionHolder {
            val itemView = mInflater.inflate(R.layout.row_collections, parent, false)
            return CollectionHolder(itemView)
        }


        override fun onBindViewHolder(holder: CollectionHolder, position: Int) {
            val currentItem = getItem(position)
            val count = numItemsByCollection(currentItem.id)

            if (count == 1)
                holder.binding.numitems.text = String.format(Locale.UK, "%d Item", count)
            else
                holder.binding.numitems.text = String.format(Locale.UK, "%d Items", count)

            holder.binding.header.visibility = if (currentItem.headerValue.isEmpty())
                View.GONE
            else
                View.VISIBLE

            val letter = currentItem.name.trim { it <= ' ' }
            if (letter.isNotEmpty())
                holder.binding.headerText.text = currentItem.name.toUpperCase(Locale.ROOT).substring(0, 1)

            holder.binding.title.text = currentItem.name

            val listener = View.OnClickListener { showCollectionView(currentItem) }
            holder.itemView.setOnClickListener(listener)
            holder.binding.button.setOnClickListener(listener)
        }



        private fun buildHeader(list: List<CollectionList>) {
            var currentLetter = "A"
            var firstRow = true
            for (doc in list) {
                val letter = doc.name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
                val sComp = letter.compareTo(currentLetter)
                if (sComp > 0 || firstRow) {
                    currentLetter = letter
                    doc.headerValue = letter
                    firstRow = false
                }
            }
        }

        fun removeCollectionListAndAllItems(id: Int) {
            val collectionList = collectionLists[id]
            mDBViewModel.deleteCollectionAndAllItems(collectionList, object : CollectionListInterface {
                override fun onComplete() {
                }
                override fun onCollectionAdded(key: Long?) {
                }
            })
        }

        inner class CollectionHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            var binding: RowCollectionsBinding = RowCollectionsBinding.bind(view)
        }

        init {
            DocumentManager.instance?.allDocuments
        }
    }

    class TabAdapter : PagerAdapter() {
        override fun getCount(): Int {
            return 3
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return false
        }
    }

    open inner class ManualsFragmentAdapter internal constructor() : RecyclerView.Adapter<ManualsFragmentAdapter.ManualsViewHolder?>(), AdapterInterface {
        var docList: ArrayList<DocumentEntry> = ArrayList()
        private val filterList: MutableList<DocumentEntry> = ArrayList()
        private val mInflater: LayoutInflater = LayoutInflater.from(this@ManualsFragment.context)

        fun dataChanged() {
            val documentManager = DocumentManager.instance  ?: return
            val allList = documentManager.allDocuments

           App.manualsCount.value = allList.size

            updateData(allList)
            docList.clear()
            for (entry in allList) {
                if (entry.document.isOnDevice) {
                    if (mode == FAV_MODE) {
                        if (entry.fav)
                            docList.add(entry)
                    } else if (mode == ALL_MODE) {
                        docList.add(entry)
                    }
                }
            }

            clearFilterList()
            filterList.addAll(docList)
            sortFilterList()
            notifyDataSetChanged()
        }

        private fun sortFilterList()
        {
            val sorted = filterList.sortedWith(compareBy(

                    { it.manufacturer.Name },
                    { it.model.Name }

            ))
            clearFilterList()
            filterList.addAll(sorted)
            buildHeader(filterList)
            showHideEmptyView(filterList.isEmpty())
        }

        fun filterResults(searchWords: String) {
            val searchTerms = searchWords.split("\\s+").toTypedArray()
            clearFilterList()
            if (searchTerms.isEmpty()) {
                filterList.clear()
                filterList.addAll(docList)
                notifyDataSetChanged()
                return
            }
            for (searchWord in searchTerms) {
                for (entry in docList) {
                    if (entry.model.Name.contains(searchWord, ignoreCase = true)
                            || entry.manufacturer.Name.contains(searchWord, ignoreCase = true)) {
                        if (!filterList.contains(entry))
                            filterList.add(entry)
                    }
                }
            }
            sortFilterList()
        }

        //
        // This loops through the document nav_manuals and adds and removes headers
        //
        private fun buildHeader(list: List<DocumentEntry>) {
            var currentLetter: String? = "A"
            var firstRow = true
            for (doc in list) {
                doc.isHeader = false
                if (doc.manufacturer.Name.isNotEmpty()) {
                    val letter = doc.manufacturer.Name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
                    val sComp = currentLetter?.let { letter.compareTo(it) }
                    if (sComp != null) {
                        if (sComp > 0 || firstRow) {
                            doc.isHeader = true
                            currentLetter = letter
                            doc.headerValue = letter
                            firstRow = false
                        }
                    }
                }
            }
        }


        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemCount(): Int {
            return filterList.size
        }

        private fun toggleFavourite(doc: DocumentEntry) {
            doc.fav = !doc.fav
            if (DocumentManager.instance != null) DocumentManager.instance?.updateDocument(doc)
            dataChanged()
        }

        private fun documentInCollection(doc: DocumentEntry): Boolean {
            for (colItem in collectionItems) {
                if (doc.document.displayName.equals(colItem.name, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManualsViewHolder {
            val itemView = mInflater.inflate(R.layout.row_documents, parent, false)
            return ManualsViewHolder(itemView)
        }


        override fun onBindViewHolder(holder: ManualsViewHolder, position: Int) {
            val item = filterList[position]
            val docInCollection = documentInCollection(item)
            val collectionItem = collectionForDocument(item)
            holder.binding.chip.setChipBackgroundColorResource(if (docInCollection) R.color.fcBlue else R.color.fcRed)
            holder.binding.chip.text = if (docInCollection) "Edit" else "Add"
            holder.binding.chip.setOnClickListener { showItemsInCollectionFragment(collectionItem, item) }
            holder.binding.favIcon.setOnClickListener { toggleFavourite(item) }
            if (item.fav) {
                holder.binding.favIcon.setChipIconResource(R.drawable.icon_star_full)
            } else {
                if (mode == FAV_MODE)
                    adapterDataUpdated()
                holder.binding.favIcon.setChipIconResource(R.drawable.icon_star_empty)
            }
            holder.binding.header.visibility = if (item.isHeader) View.VISIBLE else View.GONE
            if (item.headerValue != null) holder.binding.headerText.text = item.headerValue?.toUpperCase(Locale.ROOT)

            Helpers.renderDocumentEntryImage(item,holder.binding.imgPdfThumbnail)


            holder.binding.imgPdfThumbnail.setOnClickListener { launchDocument(item) }

            Helpers.renderManufacturerName(item,holder.binding.txtManufacturer)

            val linkedTo = item.linkedTo
            if (linkedTo!=null && item.v4Doc!=null) {
                item.documentType = item.v4Doc?.docType?:item.documentType
            }

            if (linkedTo!=null && !linkedTo.categories.isNullOrEmpty()) {
                item.model.Name = linkedTo.partDescription
                item.category = documentTypesMap[item.documentType]
            }

                item.category = documentTypesMap["PDFMODEL"]

            holder.binding.txtCategory.text = item.category
            if (item.category == null)
                holder.binding.txtCategory.visibility = View.GONE
            else
                holder.binding.txtCategory.visibility = if (item.category.isNullOrEmpty()) View.GONE else View.VISIBLE

            if(linkedTo?.isPart==1)
                holder.binding.txtModel.text = linkedTo.partNum
            else
                holder.binding.txtModel.text = item.model.Name

            renderLastViewed(item.lastSeen,holder.binding.txtLastViewed)

            val listener = View.OnClickListener { launchDocument(item) }
            holder.binding.nextArrow.setOnClickListener(listener)
            holder.itemView.setOnClickListener(listener)
        }

        private fun collectionForDocument(item: DocumentEntry): CollectionItem? {
            for (colItem in collectionItems) {
                if (colItem.name.equals(item.document.displayName, ignoreCase = true)) {
                    return colItem
                }
            }
            return null
        }

        private fun updateData(docs: List<DocumentEntry>) {
            this.docList.addAll(elements = docs)
        }

        override fun adapterDataUpdated() {
            dataChanged()
        }

        override fun removeCollectionListAndAllItems(id: Int) {}
        override fun addCollectionItem(documentEntry: DocumentEntry?, collectionId: Int) {}

        fun deleteItem(position: Int) {
            val docToDelete = filterList[position]
            mDBViewModel.deleteCollectionListItemByName(docToDelete.document.displayName, object : CollectionListInterface {
                override fun onCollectionAdded(key: Long) {}
                override fun onComplete() {
                }
            })
            DocumentManager.instance ?.removeDocument(docToDelete)
            manualsAdapter.dataChanged()
        }

        fun clearFilterList() {
            filterList.clear()
        }

        inner class ManualsViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            val binding: RowDocumentsBinding = RowDocumentsBinding.bind(view)
        }

        init {
            mDBViewModel = ViewModelProvider(this@ManualsFragment).get(DBViewModel::class.java)
        }
    }

    inner class SwipeDeleteCallback: SwipeCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.bindingAdapterPosition
            if (mode == FAV_MODE || mode == ALL_MODE) {
                manualsAdapter.deleteItem(position)
            } else if (mode == COLLECTIONS_MODE) {
                collectionsAdapter.removeCollectionListAndAllItems(position)
            }
        }
    }

    companion object {
        private const val FAV_MODE = 100
        private const val ALL_MODE = 200
        private const val COLLECTIONS_MODE = 300
    }
}