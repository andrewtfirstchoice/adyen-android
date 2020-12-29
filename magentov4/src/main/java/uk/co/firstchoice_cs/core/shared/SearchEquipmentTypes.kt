package uk.co.firstchoice_cs.core.shared

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.search_equipment.*
import kotlinx.android.synthetic.main.search_item.view.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.api.v4API.EquipmentCategory
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.viewmodels.SearchViewModel
import uk.co.firstchoice_cs.firstchoice.R

open class SearchEquipmentTypes : Fragment(R.layout.search_equipment), KoinComponent, SearchView.OnQueryTextListener {

    private var searchPhrase: String? = null
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private lateinit var mViewModel: SearchViewModel
    private val adapter = DocumentAdapter()
    private val filterList: ArrayList<EquipmentCategory> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_manuals_search_fragment, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setIconifiedByDefault(true)
        val searchEditText = searchView.findViewById<View>(R.id.search_src_text) as EditText
        searchEditText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
        searchEditText.setHintTextColor(ContextCompat.getColor(ctx, R.color.fcLightGrey))
        searchEditText.hint = "Filter equipment types"
        val searchButton: ImageView = searchView.findViewById(R.id.search_button)
        searchButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_search_white))

        val searchCloseButton: ImageView = searchView.findViewById(R.id.search_close_btn)
        searchCloseButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_close_white_24dp))

        searchCloseButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.onActionViewCollapsed()
            refreshRecycler()
        }
        initializeEmptyScreen()
        super.onCreateOptionsMenu(menu, menuInflater)
    }
    private fun initializeEmptyScreen()
    {
        emptyView.visibility = View.GONE
        initEmpty("","No Results",R.drawable.icon_search,"Search an Equipment Type")
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


    private fun refreshRecycler() {
        adapter.clearFilter()
        adapter.filterResults("")
        adapter.dataChanged()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        searchPhrase = newText
        if (newText != null) {
            adapter.filterResults(newText)
            adapter.dataChanged()
        }
        return true
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(act).get(SearchViewModel::class.java)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        adapter.initList()

        setUpToolbar()

        initComponents()
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
    }


    private fun goBack()
    {
        NavHostFragment.findNavController(this@SearchEquipmentTypes).navigateUp()
    }

    private fun initComponents() {

        toolbar.setNavigationOnClickListener { goBack() }
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this.context)
        recycler.addItemDecoration(DividerItemDecoration(this.context, DividerItemDecoration.VERTICAL))
        adapter.dataChanged()
    }


    inner class DocumentAdapter : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {

        fun initList() {
            val equipmentTypes = mViewModel.equipmentTypes
            if (equipmentTypes != null)
                filterList.addAll(equipmentTypes)
        }

        fun clearFilter() {
            filterList.clear()
            initList()
            notifyDataSetChanged()
        }

        fun filterResults(searchWords: String) {
            val man = mViewModel.equipmentTypes
            val searchTerms = searchWords.split("\\s+").toTypedArray()
            filterList.clear()
            if (searchTerms.isEmpty()) {
                clearFilter()
                if (man != null)
                    filterList.addAll(man)
                return
            }
            for (searchWord in searchTerms) {
                if (man != null)
                    for (entry in man) {
                        val desc = entry.description
                        if (desc != null) {
                            if (desc.contains(searchWord, ignoreCase = true)) {
                                if (!filterList.contains(entry)) filterList.add(entry)
                            }
                        }
                    }
            }
        }



        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
            val item = filterList[position]
            holder.title.text = item.description
            holder.itemView.setOnClickListener {
                mViewModel.selectedEquipment = item
                setFragmentResult(REQUEST_KEY, bundleOf("data" to item.id))
                goBack()
            }
        }

        fun dataChanged() {
            if(filterList.isEmpty() && searchPhrase.isNullOrEmpty())
            {
                showEmpty("There are no items that match your specific search")
            }
            else if(filterList.isEmpty() && !searchPhrase.isNullOrEmpty() && AppStatus.INTERNET_CONNECTED)
            {
                showEmpty("There are no items that match your specific filter")
            }
            else if(filterList.isEmpty() && !AppStatus.INTERNET_CONNECTED)
            {
                showEmpty("No search results - Check Internet")
            }
            else {
                recycler.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            }
            notifyDataSetChanged()
        }

        private fun showEmpty(msg:String)
        {
            emptyView.emptyDescription.text = msg
            emptyView.visibility = View.VISIBLE
            recycler.visibility = View.GONE
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
            return DocumentViewHolder(view)
        }

        override fun getItemCount(): Int {
            return filterList.size
        }

        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            var title: TextView = mView.title
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    companion object {
        const val REQUEST_KEY: String = "SearchEquipmentTypes"
    }
}