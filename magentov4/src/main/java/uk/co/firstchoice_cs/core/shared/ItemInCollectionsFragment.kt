package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.item_in_collection_manuals.*
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.FCRepository.CollectionItemInterface
import uk.co.firstchoice_cs.core.database.entities.CollectionItem
import uk.co.firstchoice_cs.core.database.entities.CollectionList
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.AddEditCollectionsViewModel
import uk.co.firstchoice_cs.core.viewmodels.ItemInCollectionsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*

open class ItemInCollectionsFragment : Fragment() {
    private lateinit var adapter: ItemInCollectionsAdapter
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var mViewModel: ItemInCollectionsViewModel
    private lateinit var mDBViewModel: DBViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_items_in_collections, menu)
        DrawableCompat.setTint(menu.getItem(0).icon, ContextCompat.getColor(requireActivity(), R.color.white))
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { NavHostFragment.findNavController(this@ItemInCollectionsFragment).navigateUp() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_create_new_collection) {
            showAddEditCollectionsFragment()
        }
        return super.onOptionsItemSelected(item)
    }

    open fun showAddEditCollectionsFragment() {
        val mViewModel = ViewModelProvider(requireActivity()).get(AddEditCollectionsViewModel::class.java)
        mViewModel.currentCollection = null
        mViewModel.isEditMode = false
        gotoAddEditCollectionsFragment()
    }

    open fun gotoAddEditCollectionsFragment() {

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.item_in_collection_manuals, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAdded) {
            mViewModel = ViewModelProvider(requireActivity()).get(ItemInCollectionsViewModel::class.java)
            mDBViewModel = ViewModelProvider(requireActivity()).get(DBViewModel::class.java)
            recycler.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
            val linearLayoutManager = LinearLayoutManager(this.context)
            recycler.layoutManager = linearLayoutManager
            val create: MaterialButton = view.findViewById(R.id.createNewCollectionBtn)
            create.setOnClickListener { showAddEditCollectionsFragment() }
            mListener?.restoreFabState()
            recycler.init(object : ScrollAwareInterface {
                override fun onScrollUp() {
                    if (isAdded) mListener?.onScrollUp()
                }

                override fun onScrollDown() {
                    if (isAdded) mListener?.onScrollDown()
                }
            })
            setUpToolbar()
            adapter = ItemInCollectionsAdapter(this.context)
            setupPage()
            recycler.adapter = adapter
            observe()
            val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    NavHostFragment.findNavController(this@ItemInCollectionsFragment).navigateUp()
                }
            }
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
            initializeEmptyScreen()
        }
    }

    private fun initializeEmptyScreen()
    {
        emptyView.visibility = View.GONE
        initEmpty("","You have no manuals collections",R.drawable.icon_collections,"Create a new collection above")
    }

    private fun initEmpty(emptyButtonStr: String, emptyDescriptionStr: String, emptyImageResource: Int, emptyTitleStr: String) {
        emptyView.emptyTitle.text=emptyTitleStr
        emptyView.emptyDescription.text=emptyDescriptionStr
        emptyView.emptyButton.text=emptyButtonStr
        emptyView.emptyImage.setImageResource(emptyImageResource)
        if(emptyButtonStr.isBlank())
            emptyView.emptyButton.visibility = View.GONE
        else
            emptyView.emptyButton.visibility = View.VISIBLE
    }


    private fun observe() {
        mDBViewModel.collectionItems.observe(viewLifecycleOwner, { collectionItems: List<CollectionItem?>? ->
            mViewModel.collectionItems = collectionItems
            mDBViewModel.collectionLists.observe(viewLifecycleOwner, { collectionLists: List<CollectionList?>? ->

                val sorted  = collectionLists?.sortedWith(compareBy { it?.name })
                mViewModel.collectionLists = sorted
                buildHeader(mViewModel.collectionLists)
                adapter.notifyDataSetChanged()
                setupPage()
            })
        })
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

    private fun setupPage() {
        if (adapter.hasItems()) {
            emptyView.visibility = View.GONE
            recycler.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        }
    }

    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun onScrollUp()
        fun restoreFabState()
    }

    internal inner class ItemInCollectionsAdapter(context: Context?) : RecyclerView.Adapter<ItemInCollectionsAdapter.ItemViewHolder?>() {
        private val mInflater: LayoutInflater = LayoutInflater.from(context)
        private fun numItemsByCollection(collectionId: Int): Int {
            var count = 0
            for (item in mViewModel.collectionItems) {
                if (item.collectionId == collectionId) count++
            }
            return count
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val itemView = mInflater.inflate(R.layout.item_in_collections_row, parent, false)
            return ItemViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val currentListItem = mViewModel.collectionLists[position]
            val inCollection = isInCollection(currentListItem)

            holder.tick.visibility = if (inCollection != null) View.VISIBLE else View.INVISIBLE

            val count = numItemsByCollection(currentListItem.id)
            if (count == 1)
                holder.numItems.text = String.format(Locale.UK, "%d Item", count)
            else
                holder.numItems.text = String.format(Locale.UK, "%d Items", count)

            val onClickListener = View.OnClickListener { toggle(currentListItem) }

            holder.tick.setOnClickListener(onClickListener)
            holder.showCollectionListButton.setOnClickListener(onClickListener)
            holder.itemView.setOnClickListener(onClickListener)
            holder.header.visibility = if (currentListItem.headerValue.isEmpty()) View.GONE else View.VISIBLE

            val letter = currentListItem.name.trim { it <= ' ' }
            if (letter.isNotEmpty()) holder.headerText.text = currentListItem.name.toUpperCase(Locale.ROOT).substring(0, 1)
            holder.title.text = currentListItem.name
        }

        override fun getItemId(position: Int): Long {
            return if (position >= mViewModel.collectionLists.size) -1 else mViewModel.collectionLists[position].id.toLong()
        }

        override fun getItemCount(): Int {
            return mViewModel.collectionLists.size
        }

        private fun isInCollection(currentList: CollectionList): CollectionItem? {
            val address: String = if (mViewModel.collectionItem != null) {
                mViewModel.collectionItem.address
            } else {
                if (mViewModel.focusedDocument == null) return null
                mViewModel.focusedDocument.document.address
            }
            for (item in mViewModel.collectionItems) {
                if (item.collectionId == currentList.id) {
                    if (item.address.equals(address, ignoreCase = true)) return item
                }
            }
            return null
        }

        private fun toggle(currentList: CollectionList) {
            //check for delete first
            val foundItem = isInCollection(currentList)
            if (foundItem != null) {
                mDBViewModel.deleteItemFromCollection(foundItem.id)
            } else {
                if (mViewModel.collectionItem != null) {
                    val newItem = CollectionItem(0,
                            currentList.id,
                            mViewModel.collectionItem.name,
                            mViewModel.collectionItem.address,
                            mViewModel.collectionItem.manufacturer,
                            mViewModel.collectionItem.model)
                    addItemToCollection(newItem)
                } else if (mViewModel.focusedDocument != null) {
                    val newItem = CollectionItem(0,
                            currentList.id,
                            mViewModel.focusedDocument.document.displayName,
                            mViewModel.focusedDocument.document.address,
                            mViewModel.focusedDocument.manufacturer.Name,
                            mViewModel.focusedDocument.model.Name)
                    mViewModel.collectionItem = newItem
                    addItemToCollection(newItem)
                }
            }
        }

        private fun addItemToCollection(currentItem: CollectionItem?) {
            if (currentItem != null) {
                mDBViewModel.insert(currentItem, object : CollectionItemInterface {
                    override fun onItemAdded(key: Long) {
                        mViewModel.collectionItem.id = key.toInt()
                    }

                    override fun onComplete() {
                        LoggingHelper.debugMsg(this.javaClass.canonicalName, "Collection item added")
                    }
                })
            }
        }

        val isEmpty: Boolean
            get() = mViewModel.collectionLists.size == 0

        fun hasItems(): Boolean {
            return mViewModel.collectionLists.size > 0
        }

        inner class ItemViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.title)
            val numItems: TextView = view.findViewById(R.id.numitems)
            val header: ViewGroup = view.findViewById(R.id.header)
            val headerText: TextView = view.findViewById(R.id.headerText)
            val showCollectionListButton: RelativeLayout = view.findViewById(R.id.button)
            val tick: Chip = view.findViewById(R.id.tick)
        }
    }
}