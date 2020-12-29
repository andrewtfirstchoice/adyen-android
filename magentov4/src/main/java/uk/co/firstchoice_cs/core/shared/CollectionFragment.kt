package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.collection_fragment.*
import kotlinx.android.synthetic.main.collection_fragment.recycler
import kotlinx.android.synthetic.main.collection_fragment.toolbar
import kotlinx.android.synthetic.main.empty_layout.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.FCRepository.CollectionListInterface
import uk.co.firstchoice_cs.core.database.entities.CollectionItem
import uk.co.firstchoice_cs.core.database.entities.CollectionList
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.SwipeCallback
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager.Companion.instance
import uk.co.firstchoice_cs.core.managers.ThumbNailManager.getThumbImage
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.RowDocumentsBinding
import java.util.*
import kotlin.collections.ArrayList

open class CollectionFragment : Fragment(R.layout.collection_fragment), KoinComponent, SearchView.OnQueryTextListener {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    lateinit var mAnalyticsViewModel: AnalyticsViewModel
    private lateinit var adapter: CollectionsAdapter
    private lateinit var mViewModel: CollectionViewModel
    private lateinit var mDBViewModel: DBViewModel
    private var collectionItems: ArrayList<CollectionItem> = ArrayList()
    private var collectionId: Long = -1L
    private val filteredList: MutableList<DocumentEntry> = ArrayList()
    private var docList: ArrayList<DocumentEntry> = ArrayList()
    private var filterString:String = ""

    open fun showAddEditCollectionsFragment(collectionList: CollectionList) {
        val vm = ViewModelProvider(act).get(AddEditCollectionsViewModel::class.java)
        vm.currentCollection = collectionList
        vm.isEditMode = true
        gotoAddEditCollectionsFragment()
    }



    fun navigateToItemsInCollectionFragment(collectionItem: CollectionItem?, documentEntry: DocumentEntry?)
    {
        val vm = ViewModelProvider(requireActivity()).get(ItemInCollectionsViewModel::class.java)
        vm.collectionItem = collectionItem
        vm.focusedDocument = documentEntry
        gotoItemsInCollectionsFragment()
    }

    open fun gotoAddEditCollectionsFragment()
    {
    }

    open fun gotoItemsInCollectionsFragment()
    {
    }

    open fun gotoManualsDetailsFragment()
    {
    }

    fun launchDocument(doc: DocumentEntry) {
        val b = Bundle()
        b.putString("manufacturer", doc.manufacturer.Name)
        b.putString("model", doc.model.Name)
        if (mAnalyticsViewModel.mFirebaseAnalytics != null)
            mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("ManualViewed", b)
        val vm = ViewModelProvider(act).get(ManualDetailsViewModel::class.java)
        vm.doc = doc
        gotoManualsDetailsFragment()
    }


    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filterString = newText
        filterResults(filterString)
        return true
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(act).get(CollectionViewModel::class.java)
        mDBViewModel = ViewModelProvider(this).get(DBViewModel::class.java)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@CollectionFragment).navigateUp()
            }
        }
        initViewModels()
        setHasOptionsMenu(true)

        initRecycler()
        initializeEmptyScreen()
        setUpToolbar()
        act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        observe()
    }

    private fun buildHeader() {
        var currentLetter = "A"
        var firstRow = true
        for (doc in filteredList) {
            doc.isHeader = false
            if (doc.model.Name.isNotEmpty()) {
                val letter = doc.model.Name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
                val sComp = letter.compareTo(currentLetter.toUpperCase(Locale.ENGLISH))
                if (sComp > 0 || firstRow) {
                    doc.isHeader = true
                    currentLetter = letter
                    doc.headerValue = letter
                    firstRow = false
                }
            }
        }
    }

    private fun observe() {

        mDBViewModel.getCollectionItems(collectionId).observe(viewLifecycleOwner, { collectionLists: List<CollectionItem> ->
            collectionItems.clear()
            collectionItems.addAll(collectionLists)
            refreshData()
        })
    }

    private fun initRecycler() {
        recycler.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
        val linearLayoutManager = LinearLayoutManager(this.context)
        recycler.layoutManager = linearLayoutManager
        recycler.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })

        val collectionList =  mViewModel.collectionList
        if(collectionList!=null)
            collectionId = collectionList.id.toLong()
        adapter = CollectionsAdapter()
        recycler.adapter = adapter
        val swipeCallBack: ItemTouchHelper.SimpleCallback = SwipeDeleteCallback()
        val helper = ItemTouchHelper(swipeCallBack)
        helper.attachToRecyclerView(recycler)
    }

    fun refreshData() {
        val documentMngr = instance ?: return
        val allList = documentMngr.allDocuments
        docList.clear()
        for (entry in allList) {
            if (entry.document.isOnDevice) {
                if (isInCollection(entry)) docList.add(entry)
            }
        }
        filteredList.clear()
        filteredList.addAll(docList)
        sortFilterList()
    }

    private fun sortFilterList()
    {
        val sorted  = filteredList.sortedWith(compareBy(
                { it.manufacturer.Name },
                { it.model.Name }

        ))
        filteredList.clear()
        filteredList.addAll(sorted)
        buildHeader()
        setupPage(filteredList)
        adapter.notifyDataSetChanged()
    }

    private fun filterResults(searchWords: String) {
        val searchTerms = searchWords.split("\\s+").toTypedArray()
        filteredList.clear()
        if (searchTerms.isEmpty()) {
            clearFilter()
            filteredList.addAll(docList)
            return
        }
        for (searchWord in searchTerms) {
            for (entry in docList) {
                if (entry.model.Name.contains(searchWord, ignoreCase = true)
                        || entry.manufacturer.Name.contains(searchWord, ignoreCase = true)) {
                    if (!filteredList.contains(entry)) filteredList.add(entry)
                }
            }
        }

        val sorted  = filteredList.sortedWith(compareBy { it.manufacturer.Name })
        filteredList.clear()
        filteredList.addAll(sorted)
        buildHeader()
        showHideEmptyView(filteredList.isEmpty())
    }

    private fun showHideEmptyView(stateIsEmpty: Boolean) {
        if (stateIsEmpty) {
            recycler.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            if(filterString.isBlank())
                initEmptyNoFilter()
            else
                initEmptyWithFilter()
        } else {
            recycler.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    private fun initEmpty(emptyButtonStr: String, emptyDescriptionStr: String, emptyImageResource: Int, emptyTitleStr: String) {
        emptyView.visibility = View.GONE
        emptyView.emptyTitle.text=emptyTitleStr
        emptyView.emptyDescription.text=emptyDescriptionStr
        emptyView.emptyButton.text=emptyButtonStr
        emptyView.emptyImage.setImageResource(emptyImageResource)
        if(emptyButtonStr.isBlank())
            emptyView.emptyButton.visibility = View.GONE
        else
            emptyView.emptyButton.visibility = View.VISIBLE
    }

    private fun initializeEmptyScreen()
    {
        emptyView.visibility = View.GONE

        emptyView.emptyButton.setOnClickListener{
            val collectionList =  mViewModel.collectionList
            if(collectionList!=null)
                showAddEditCollectionsFragment(collectionList)
        }
    }


    private fun initEmptyNoFilter()
    {
        initEmpty("Start Adding", "Add to your collection to group manuals together", R.drawable.icon_collections, "This collection has no manuals")
    }

    private fun initEmptyWithFilter()
    {
        initEmpty("", "Filtered collection manuals", R.drawable.icon_collections, "No Results")
    }


    private fun clearFilter() {
        filteredList.clear()
        filteredList.addAll(docList)
        adapter.notifyDataSetChanged()
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

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun initViewModels() {
        mAnalyticsViewModel = ViewModelProvider(requireActivity()).get(AnalyticsViewModel::class.java)
    }


    private fun setupPage(docList: List<DocumentEntry>) {
        if (docList.isEmpty()) {
            recycler.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
        } else {
            recycler.visibility = View.VISIBLE
            emptyView.visibility = View.GONE
        }
    }

    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun restoreFabState()
        fun onScrollUp()
    }

    private fun isInCollection(doc: DocumentEntry): Boolean {
        for (collItem in collectionItems) {
            if (collItem.name.equals(doc.document.displayName, ignoreCase = true)) return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_collection_menu_items, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        val searchEditText = searchView.findViewById<View>(R.id.search_src_text) as EditText
        searchEditText.setTextColor(ContextCompat.getColor(ctx,R.color.white))
        searchEditText.setHintTextColor(ContextCompat.getColor(ctx,R.color.fcLightGrey))
        searchEditText.hint = "Filter manuals"
        val searchButton: ImageView = searchView.findViewById(R.id.search_button)
        searchButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_search_white))
        val searchCloseButton: ImageView = searchView.findViewById(R.id.search_close_btn)
        searchCloseButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_close_white_24dp))
        searchCloseButton.setOnClickListener {
            filterString = ""
            searchView.setQuery(filterString, false)
            searchView.onActionViewCollapsed()
            refreshRecycler()
        }

        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun refreshRecycler() {
        clearFilter()
        filterResults("")
        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_edit) {
            val collectionList =  mViewModel.collectionList
            if(collectionList!=null)
                showAddEditCollectionsFragment(collectionList)
        }
        return super.onOptionsItemSelected(item)
    }



    private fun setUpToolbar() {
        act.setSupportActionBar(toolbar)
        val collectionList =  mViewModel.collectionList
        if(collectionList!=null)
            toolbar.title = collectionList.name
        toolbar.setNavigationOnClickListener { NavHostFragment.findNavController(this@CollectionFragment).navigateUp() }
    }

    inner class CollectionsAdapter internal constructor() : RecyclerView.Adapter<CollectionsAdapter.ItemViewHolder>() {
        private val mInflater: LayoutInflater = LayoutInflater.from(this@CollectionFragment.context)

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemCount(): Int {
            return filteredList.size
        }

        private fun toggleFavourite(doc: DocumentEntry) {
            doc.fav = !doc.fav
            if (instance != null) instance?.updateDocument(doc)
            refreshData()
        }

        private fun documentInCollection(doc: DocumentEntry): Boolean {
            for (colItem in collectionItems) {
                if (doc.document.displayName.equals(colItem.name, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val itemView = mInflater.inflate(R.layout.row_documents, parent, false)
            return ItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = filteredList[position]
            val docInCollection = documentInCollection(item)
            val collectionItem = collectionForDocument(item)
            holder.binding.chip.setChipBackgroundColorResource(if (docInCollection) R.color.fcBlue else R.color.fcRed)
            holder.binding.chip.text = if (docInCollection) "Edit" else "Add"

            holder.binding.chip.setOnClickListener { navigateToItemsInCollectionFragment(collectionItem, item) }

            holder.binding.favIcon.setOnClickListener { toggleFavourite(item) }
            if (item.fav) {
                holder.binding.favIcon.setChipIconResource(R.drawable.icon_star_full)
            } else {
                holder.binding.favIcon.setChipIconResource(R.drawable.icon_star_empty)
            }
            holder.binding.header.visibility = if (item.isHeader) View.VISIBLE else View.GONE
            if (item.headerValue != null) holder.binding.headerText.text = item.headerValue!!.toUpperCase(Locale.ROOT)


            val file = getThumbImage(item)
            Helpers.renderImage(holder.binding.imgPdfThumbnail,file)

            //this is a fudge to prevent legacy v2 bug showing incorrect manufacturer name
            if(item.manufacturer.Name==item.model.Name)
                holder.binding.txtManufacturer.text = "First Choice Consumables"
            else
                holder.binding.txtManufacturer.text = item.manufacturer.Name

            holder.binding.txtCategory.text = item.category

            val linkedTo = item.linkedTo
            if (linkedTo!=null && item.v4Doc!=null) {
                item.documentType = item.v4Doc?.docType?:item.documentType
            }

            if (!linkedTo?.categories.isNullOrEmpty()) {
                item.model.Name = linkedTo?.partDescription
                item.category = Helpers.documentTypesMap[item.documentType]
            }

                item.category = Helpers.documentTypesMap["PDFMODEL"]


            if(linkedTo?.isPart==1)
                holder.binding.txtModel.text = linkedTo.partNum
            else
                holder.binding.txtModel.text = item.model.Name
            Helpers.renderLastViewed(item.lastSeen, holder.binding.txtLastViewed)
            holder.binding.txtLastViewed.text  = ""
            val listener = View.OnClickListener {launchDocument(item) }
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

        inner class ItemViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            var binding: RowDocumentsBinding = RowDocumentsBinding.bind(view)
        }
    }

    inner class SwipeDeleteCallback: SwipeCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            super.onSwiped(viewHolder, direction)
            val position = viewHolder.bindingAdapterPosition
            val docToDelete = docList[position]
            //delete the collection items that may be left over
            mDBViewModel.deleteCollectionListItemByName(docToDelete.document.displayName, object : CollectionListInterface {
                override fun onCollectionAdded(key: Long) {}
                override fun onComplete() {
                    instance?.removeDocument(docToDelete)
                    docList.remove(docToDelete)
                    refreshData()
                    setupPage(docList)
                }
            })
        }
    }
}