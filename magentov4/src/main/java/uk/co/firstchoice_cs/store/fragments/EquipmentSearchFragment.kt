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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.equipment_search_fragment.*
import kotlinx.android.synthetic.main.equipment_type_header.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.Constants.SEARCH_DATA
import uk.co.firstchoice_cs.Constants.SEARCH_DATA_2
import uk.co.firstchoice_cs.Constants.SEARCH_TERM
import uk.co.firstchoice_cs.Constants.SEARCH_TITLE
import uk.co.firstchoice_cs.Constants.SEARCH_TYPE
import uk.co.firstchoice_cs.core.api.v4API.EquipmentCategory
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.SafetyChecks.safeString
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*
import kotlin.collections.ArrayList

class EquipmentSearchFragment : Fragment(), SearchView.OnQueryTextListener,KoinComponent {
    private var sectionedAdapter: SectionedRecyclerViewAdapter? = null
    private var mAnalyticsViewModel: AnalyticsViewModel? = null
    private var filteredList:ArrayList<EquipmentCategory> = ArrayList()
    private var all:ArrayList<EquipmentCategory> = ArrayList()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.equipment_search_fragment, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.equipment_search,menu)

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

        if(App.globalData.shouldGetTopLevel()) {
            loadEquipmentTypes()
        }
        else
        {
            buildMap()
            recyclerview?.adapter?.notifyDataSetChanged()
        }
    }



    private fun setUpToolbar(view: View) {
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
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
        var topLevelCode = ""
        for(item in filteredList)
        {

            if(item.TopLevel == topLevelCode)
                item.isHeader = false
            else
            {
                item.isHeader = true
                item.headerLetter = item.TopLevelDesc
                topLevelCode = item.TopLevel
            }

        }
    }


    private fun loadEquipmentTypes() {
        pbDownload?.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val udCodes = V4APICalls.topLevel("")
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                App.globalData.topLevels = udCodes

                withContext(Dispatchers.Main) {
                    if (pbDownload != null)
                        pbDownload.visibility = View.GONE
                    App.globalData.topLevelLoadTime = Date(System.currentTimeMillis()).time
                    buildMap()
                }
            }
            catch (ex:Exception)
            {
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    if (pbDownload != null)
                        pbDownload.visibility = View.GONE
                }
            }
        }
    }

    private fun buildMap()
    {
        all.clear()
        val categories = App.globalData.topLevels?.categories
        if (categories != null) {
            for (cat in categories.toList()) {
                val eqCats = cat.equipmentCategory?.toList()?.sortedBy { it.description }
                if (eqCats != null) {
                    for (eq in eqCats) {
                        eq.TopLevel = cat.tlc?:""
                        eq.TopLevelDesc = cat.description?:""
                        all.add(eq)
                    }
                }
            }
        }

        filteredList.clear()
        filteredList.addAll(all)

        dataChanged()
    }
    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        filteredList.clear()
        if (!TextUtils.isEmpty(newText)) {
            for (attr in all) {
                when {
                    safeString(attr.TopLevelDesc).contains(safeString(newText?.trim())) -> filteredList.add(attr)
                }
            }
        }
        else
        {
            filteredList.addAll(all)
        }
        dataChanged()
        return true
    }

    private fun dataChanged()
    {
        assignHeaders()
        recyclerview.adapter?.notifyDataSetChanged()
    }

    inner class MyAdapter : RecyclerView.Adapter<ItemViewHolder>()
    {
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val attributes: EquipmentCategory = this@EquipmentSearchFragment.filteredList[position]

            holder.tvItem.text = attributes.description
            holder.itemView.setOnClickListener {
                val b = Bundle()
                b.putString("equipment", attributes.toString())
                mAnalyticsViewModel?.mFirebaseAnalytics?.logEvent("EquipmentSelected", b)
                hideKeyboard()
                val gSon: Gson? = Gson()
                val bundle: Bundle? = bundleOf("EQUIPMENT" to gSon?.toJson(attributes))
                bundle?.putString(SEARCH_TITLE,attributes.description)
                bundle?.putString(SEARCH_TERM,"")
                bundle?.putString(SEARCH_DATA,attributes.id)
                bundle?.putString(SEARCH_DATA_2,attributes.TopLevel)
                bundle?.putString(SEARCH_TYPE, Constants.SEARCH_TYPE_EQUIPMENT_CATEGORY)
                findNavController().navigate(R.id.action_equipmentSearchFragment_to_shopPartsResultFragment, bundle, null, null)
            }
            holder.tvTitle.text = attributes.TopLevelDesc
            if(attributes.isHeader) {
                holder.header.visibility = View.VISIBLE
                holder.header.tvTitle.text =attributes.headerLetter
            }
            else {
                holder.header.visibility = View.GONE
            }

            holder.header.viewAllButton.setOnClickListener {
                val b = Bundle()
                b.putString("equipment", attributes.toString())
                mAnalyticsViewModel?.mFirebaseAnalytics?.logEvent("TopLevelSelected", b)
                hideKeyboard()
                val gSon: Gson? = Gson()
                val bundle: Bundle? = bundleOf("TOP_LEVEL" to gSon?.toJson(attributes))
                bundle?.putString(SEARCH_TITLE,attributes.headerLetter)
                bundle?.putString(SEARCH_TERM,"")
                bundle?.putString(SEARCH_DATA,attributes.TopLevel)
                bundle?.putString(SEARCH_DATA_2,attributes.TopLevel)
                bundle?.putString(SEARCH_TYPE, Constants.SEARCH_TYPE_TOP_LEVEL)
                findNavController().navigate(R.id.action_equipmentSearchFragment_to_shopPartsResultFragment, bundle, null, null)
            }



        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.equipment_item, parent, false)
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

