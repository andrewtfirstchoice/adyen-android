package uk.co.firstchoice_cs.store.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.part_classes_search_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.App.Companion.globalData
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.Constants.SEARCH_DATA
import uk.co.firstchoice_cs.Constants.SEARCH_TERM
import uk.co.firstchoice_cs.Constants.SEARCH_TITLE
import uk.co.firstchoice_cs.Constants.SEARCH_TYPE
import uk.co.firstchoice_cs.core.api.v4API.ClassID
import uk.co.firstchoice_cs.core.api.v4API.PartClass
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*
import kotlin.collections.ArrayList


class PartCategoriesSearchFragment : Fragment(),  SearchView.OnQueryTextListener , KoinComponent {


    private var sectionedAdapter: SectionedRecyclerViewAdapter? = null
    private var mAnalyticsViewModel: AnalyticsViewModel? = null
    private var filteredList:ArrayList<PartClass> = ArrayList()
    private var all:ArrayList<PartClass> = ArrayList()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.part_classes_search_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.part_category_search,menu)

        val item:MenuItem = menu.findItem(R.id.action_search)
        val searchView:SearchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAnalyticsViewModel = activity?.let { ViewModelProvider(it).get(AnalyticsViewModel::class.java) }
        pbDownload?.visibility = View.GONE

        sectionedAdapter = SectionedRecyclerViewAdapter()
        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration(recyclerview.context, layoutManager.orientation)
        dividerItemDecoration.let { recyclerview.addItemDecoration(it) }
        recyclerview.layoutManager = layoutManager
        recyclerview.adapter = MyAdapter()

        setUpToolbar(view)
        loadAllPartClasses()
    }




    private fun setUpToolbar(view: View) {
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadAllPartClasses() {
        if(!globalData.shouldGetEPartClasses())
        {
            buildMap()
            recyclerview.adapter?.notifyDataSetChanged()
        }
        else {
            pbDownload?.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    globalData.partClasses = V4APICalls.partClass("",0,2000)
                    buildMap()
                    withContext(Dispatchers.Main) {
                        hideDownload()
                        if (recyclerview != null) {
                            recyclerview.adapter?.notifyDataSetChanged()
                        }
                        globalData.partClassesLoadTime = Date(System.currentTimeMillis()).time
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        hideDownload()
                    }
                }
            }
        }
    }


    private fun hideDownload()
    {
        if (pbDownload != null) {
            pbDownload.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private fun hideKeyboard() {
        if (activity != null)
            Helpers.hideKeyboard(toolbar)
    }


    private fun assignHeaders()
    {
        var currentLetter = ""

        for (item in filteredList) {
            val letter:String = item.classDescription.trim().toUpperCase(Locale.ENGLISH).substring(0,1)
            if(letter!=currentLetter.toUpperCase(Locale.ENGLISH)) {
                currentLetter = letter.toUpperCase(Locale.ENGLISH)
                item.headerLetter = currentLetter
            }
            else
            {
                item.headerLetter = ""
            }
        }
    }
    private fun buildMap() {
        all.clear()
        globalData.partCategoriesMap = globalData.partClasses?.let { fetchPartClassesMap(it) }
        globalData.partCategoriesMap?.forEach { (_, value) ->
            if (value.isNotEmpty()) {
                for (partClass in value.sortedWith(compareBy { it.classDescription })) {
                    all.add(partClass)
                }
            }
        }
        filteredList.addAll(all)
        assignHeaders()
    }


    private fun fetchPartClassesMap(classId: ClassID): Map<String, List<PartClass>> {
        val map: MutableMap<String, List<PartClass>> = LinkedHashMap()
        for (element in getAlphaNumeric()) {
            val filteredPartCategories = getPartCategoriesWithLetter(classId.partClass, element)
            if (filteredPartCategories.isNotEmpty()) {
                map[element.toString()] = filteredPartCategories
            }
        }
        return map
    }


    private fun getAlphaNumeric(): String {
        var alphaNumeric = "0123456789"
        var letter = 'A'
        while (letter <= 'Z') {
            alphaNumeric += letter
            letter++
        }
        return alphaNumeric
    }

    private fun getPartCategoriesWithLetter(partclasses: List<PartClass>, letter: Char): List<PartClass> {
        val partClassesList: MutableList<PartClass> = ArrayList()
        for (partClass in partclasses) {
            if (partClass.classDescription.isNotEmpty() && partClass.classDescription[0] == letter) {
                partClassesList.add(partClass)
            }
        }
        return partClassesList
    }


    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        filteredList.clear()
        if (!TextUtils.isEmpty(newText)) {
            for (contact in all) {
                if (contact.classDescription.toLowerCase(Locale.getDefault()).contains(newText?.toLowerCase(Locale.getDefault()).toString())) {
                    filteredList.add(contact)
                }
            }
        }
        else
        {
            filteredList.addAll(all)
        }
        assignHeaders()
        recyclerview.adapter?.notifyDataSetChanged()
        return true
    }

    inner class MyAdapter : RecyclerView.Adapter<ItemViewHolder>()
    {
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val partClass: PartClass = this@PartCategoriesSearchFragment.filteredList[position]
            
            holder.tvItem.text = partClass.classDescription
            holder.itemView.setOnClickListener {
                val b = Bundle()
                b.putString("partclass", partClass.toString())
                mAnalyticsViewModel?.mFirebaseAnalytics?.logEvent("PartClassSelected", b)
                hideKeyboard()
                val gson: Gson? = Gson()
                val bundle: Bundle? = bundleOf("CATEGORY" to gson?.toJson(partClass))
                bundle?.putString(SEARCH_TITLE,partClass.classDescription)
                bundle?.putString(SEARCH_TERM,"")
                bundle?.putString(SEARCH_DATA,partClass.classId)
                bundle?.putString(SEARCH_TYPE, Constants.SEARCH_TYPE_CLASS_ID)
                findNavController().navigate(R.id.action_partClassesSearchFragment_to_shopPartsResultFragment, bundle, null, null)
            }
            if(partClass.headerLetter.isNullOrEmpty())
                holder.header.visibility = View.GONE
            else
                holder.header.visibility = View.VISIBLE
            holder.tvTitle.text = partClass.headerLetter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.part_class_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int = filteredList.count()

    }

    class ItemViewHolder internal constructor(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val tvItem: TextView = rootView.findViewById(R.id.tvItem)
        val tvTitle: TextView = rootView.findViewById(R.id.tvTitle)
        val header: ConstraintLayout = rootView.findViewById(R.id.header)
    }
}

