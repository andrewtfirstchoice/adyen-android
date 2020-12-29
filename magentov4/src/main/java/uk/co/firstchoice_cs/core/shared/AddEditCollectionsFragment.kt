package uk.co.firstchoice_cs.core.shared

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.textview.MaterialTextView
import uk.co.firstchoice_cs.core.alerts.Alerts.showAlert
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.FCRepository.CollectionItemInterface
import uk.co.firstchoice_cs.core.database.FCRepository.CollectionListInterface
import uk.co.firstchoice_cs.core.database.entities.CollectionItem
import uk.co.firstchoice_cs.core.database.entities.CollectionList
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.hideKeyboard
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.DocumentManager.Companion.instance
import uk.co.firstchoice_cs.core.viewmodels.AddEditCollectionsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.AddEditCollectionsFragmentBinding
import java.util.*

open class AddEditCollectionsFragment : Fragment() {
    protected var collectionId = 0L //used only for collections mode
    private var documentManager: DocumentManager? = null
    private lateinit var addCollectionsAdapter: AddEditCollectionsAdapter
    private lateinit var mViewModel: AddEditCollectionsViewModel
    private lateinit var binding: AddEditCollectionsFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return setLayout(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = AddEditCollectionsFragmentBinding.bind(view)
        mViewModel = ViewModelProvider(requireActivity()).get(AddEditCollectionsViewModel::class.java)
        init()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun setLayout(inflater: LayoutInflater, container: ViewGroup?): View {
        return inflater.inflate(R.layout.add_edit_collections_fragment, container, false)
    }

    private fun goBack()
    {
        setFragmentResult(REQUEST_KEY, bundleOf("data" to ""))
        NavHostFragment.findNavController(this@AddEditCollectionsFragment).navigateUp()
    }

    private fun init() {
        documentManager = instance
        addCollectionsAdapter = AddEditCollectionsAdapter(null)
        binding.addCollectionsRecycler.adapter = addCollectionsAdapter
        binding.addCollectionsRecycler.layoutManager = LinearLayoutManager(this.context)
        binding.addCollectionsRecycler.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        if (mViewModel.isEditMode)
            binding.toolbar.title = "Edit Collection"
        else
            binding.toolbar.title = "Create New Collection"


        addCollectionsAdapter = if (!mViewModel.isEditMode)
            AddEditCollectionsAdapter(null)
        else
            AddEditCollectionsAdapter(mViewModel.currentCollection)

        if (mViewModel.currentCollection != null)
            binding.addCollectionTextInput.setText(if (mViewModel.isEditMode) mViewModel.currentCollection?.name else "")

        binding.addCollectionsRecycler.adapter = addCollectionsAdapter
        setUpToolbar()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_items_in_collection, menu)
        //this makes the icons white and works on older versions of android
        DrawableCompat.setTint(menu.getItem(0).icon, ContextCompat.getColor(requireActivity(), R.color.white))
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // action with ID action_refresh was selected
        if (item.itemId == R.id.action_done) {
            val name = binding.addCollectionTextInput.editableText.toString().trim { it <= ' ' }
            if (name.isEmpty()) {
                showAlert("We need more information", "Please enter a name for your collection", null)
            } else {
                hideKeyboard(binding.addCollectionTextInput)
                if (mViewModel.isEditMode)
                    addCollectionsAdapter.editClicked() else addCollectionsAdapter.addClicked(name)
                goBack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class AddEditCollectionsAdapter(private val currentCollection: CollectionList?) : RecyclerView.Adapter<AddEditCollectionsViewHolder>() {
        private val documentList: MutableList<DocumentEntry> = ArrayList()
        private var collectionItemLists: List<CollectionItem>? = null
        private val mDBViewModel: DBViewModel = ViewModelProvider(requireActivity()).get(DBViewModel::class.java)
        private val allList: List<DocumentEntry> = documentManager!!.allDocuments
        private var currentCollectionListID: Long = 0
        private fun observe() {
            if (currentCollection != null) {
                val mDBViewModel = ViewModelProvider(requireActivity()).get(DBViewModel::class.java)
                val callGetCollectionItems = mDBViewModel.getCollectionItems(currentCollection.id.toLong())
                callGetCollectionItems.observe(this@AddEditCollectionsFragment.viewLifecycleOwner,
                    { collectionLists: List<CollectionItem>? ->
                        collectionItemLists = collectionLists
                        refreshData()
                    })
            }
        }

        private fun setIsInCollection(entry: DocumentEntry) {
            if (collectionItemLists == null) entry.addCollectionTickSelected = false else {
                for (collItem in collectionItemLists!!) {
                    if (collItem.name.equals(entry.document.displayName, ignoreCase = true)) {
                        if (collItem.collectionId == currentCollection!!.id) {
                            entry.addCollectionTickSelected = true
                            break
                        } else {
                            entry.addCollectionTickSelected = false
                        }
                    }
                }
            }
        }

        private fun refreshData() {
            documentList.clear()
            for (entry in allList) {
                setIsInCollection(entry)
                if (entry.document.isOnDevice) {
                    documentList.add(entry)
                }
            }
            val sorted  = documentList.sortedWith(compareBy(
                    { it.manufacturer.Name },
                    { it.model.Name }

            ))
            documentList.clear()
            documentList.addAll(sorted)
            buildHeader(documentList)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddEditCollectionsViewHolder {
            //Inflate the layout, initialize the View Holder
            val v = LayoutInflater.from(parent.context).inflate(R.layout.add_edit_collections_row, parent, false)
            return AddEditCollectionsViewHolder(v)
        }

        override fun onBindViewHolder(holder: AddEditCollectionsViewHolder, position: Int) {
            val item = documentList[position]
            if (item.isHeader) {
                holder.header.visibility = View.VISIBLE
                val letter = item.manufacturer.Name.trim { it <= ' ' }
                if (letter.isNotEmpty()) holder.headerText.text = item.manufacturer.Name.toUpperCase(Locale.ROOT).substring(0, 1)
            } else {
                holder.header.visibility = View.GONE
            }

            Helpers.renderDocumentEntryImage(item,holder.pdfThumbnail)

            if (item.fileName.contains(item.manufacturer.Name)) {
                holder.category.visibility = View.VISIBLE
                holder.category.text = HtmlCompat.fromHtml(item.fileName, HtmlCompat.FROM_HTML_MODE_LEGACY)
            } else {
                holder.category.visibility = View.GONE
            }
            Helpers.renderManufacturerName(item,holder.manufacturer)

            holder.model.text = item.model.Name
            holder.nextArrow.visibility = if (item.addCollectionTickSelected) View.VISIBLE else View.GONE
            holder.itemView.setOnClickListener {
                item.addCollectionTickSelected = !item.addCollectionTickSelected
                holder.nextArrow.visibility = if (item.addCollectionTickSelected) View.VISIBLE else View.GONE
                notifyDataSetChanged()
            }
        }

        //
        // This loops through the document nav_manuals and adds and removes headers
        //
        private fun buildHeader(list: List<DocumentEntry>) {
            var currentLetter: String? = "A"
            var firstRow = true
            for (doc in list) {
                if (doc.manufacturer.Name.isNotEmpty()) {
                    val letter = doc.manufacturer.Name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
                    val sComp = letter.compareTo(currentLetter!!)
                    if (sComp > 0 || firstRow) {
                        currentLetter = letter
                        doc.headerValue = letter
                        firstRow = false
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return documentList.size
        }

        val isEmpty: Boolean
            get() = documentList.isEmpty()

        fun addClicked(name: String) {
            notifyDataSetChanged()
            currentCollectionListID = 0
            mDBViewModel.insert(CollectionList(currentCollectionListID.toInt(), name), object : CollectionListInterface {
                override fun onCollectionAdded(key: Long) {
                    currentCollectionListID = key
                    updateDB()
                }

                override fun onComplete() {}
            })
        }

        fun editClicked() {
            notifyDataSetChanged()
            val collection = currentCollection
            if (collection != null) {
                currentCollectionListID = collection.id.toLong()
                val title = binding.addCollectionTextInput?.text.toString()
                if (title.trim { it <= ' ' }.isNotEmpty()) {
                    collection.name = title
                    mDBViewModel.update(collection)
                    updateDB()
                } else {
                    showAlert("Error", "Please add a collection title", null)
                }
            }
        }


        private fun itemIsInCollection(entry: DocumentEntry): Int {
            if (collectionItemLists == null) return -1
            for (collectionItem in collectionItemLists!!) {
                if (collectionItem.name.equals(entry.document.displayName, ignoreCase = true)) {
                    return collectionItem.id
                }
            }
            return -1
        }

        private fun updateDB() {
            for (item in documentList) {
               val res = itemIsInCollection(item)
                //collection collectionItemList does exist in our collection nav_manuals
                // - should we delete it
                if (res != -1) {
                    if (!item.addCollectionTickSelected) {
                        mDBViewModel.deleteItemFromCollection(res)
                    }
                } else {
                    if (item.addCollectionTickSelected) {
                        val colItem = CollectionItem(0, currentCollectionListID.toInt(), item.document.displayName, item.document.address, item.manufacturer.Name, item.model.Name)
                        mDBViewModel.insert(colItem, object : CollectionItemInterface {
                            override fun onItemAdded(key: Long) {}
                            override fun onComplete() {}
                        })
                    }
                }
            }
        }

        init {
            refreshData()
            observe()
        }
    }

    companion object {
        const val REQUEST_KEY: String = "AddEditCollectionsFragment"
    }

    internal inner class AddEditCollectionsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pdfThumbnail: ShapeableImageView = itemView.findViewById(R.id.imgPdfThumbnail)
        var model: MaterialTextView = itemView.findViewById(R.id.txtModel)
        var manufacturer: MaterialTextView = itemView.findViewById(R.id.txtManufacturer)
        var category: MaterialTextView = itemView.findViewById(R.id.txtCategory)
        var nextArrow: ImageView = itemView.findViewById(R.id.nextArrow)
        var headerText: MaterialTextView = itemView.findViewById(R.id.headerText)
        var header: ConstraintLayout = itemView.findViewById(R.id.header)
    }
}