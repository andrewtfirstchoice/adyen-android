package uk.co.firstchoice_cs.store.fragments

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
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
import com.google.android.material.chip.Chip
import com.google.gson.Gson
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.manufacturer_search_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.Constants.MANUFACTURER_ARG
import uk.co.firstchoice_cs.Constants.SEARCH_DATA
import uk.co.firstchoice_cs.Constants.SEARCH_TERM
import uk.co.firstchoice_cs.Constants.SEARCH_TITLE
import uk.co.firstchoice_cs.Constants.SEARCH_TYPE
import uk.co.firstchoice_cs.core.api.v4API.Brand
import uk.co.firstchoice_cs.core.api.v4API.Brands
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*
import kotlin.collections.ArrayList

class ManufacturerSearchFragment : Fragment(R.layout.manufacturer_search_fragment),  SearchView.OnQueryTextListener, KoinComponent {
    private var sectionedAdapter: SectionedRecyclerViewAdapter? = null
    private var mAnalyticsViewModel: AnalyticsViewModel? = null
    private var filteredList:ArrayList<Brand> = ArrayList()
    private var all:ArrayList<Brand> = ArrayList()


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.manufacturer_search,menu)

        val item:MenuItem = menu.findItem(R.id.action_filter)
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

        setUpToolbar()

        if(App.globalData.shouldGetBrands()) {
            loadAllBrands()
        }
        else
        {
            buildMap()
            recyclerview?.adapter?.notifyDataSetChanged()
        }
    }




    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun loadAllBrands() {
        pbDownload?.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {

            try {
                val manufacturers = V4APICalls.allBrands(0)
                App.globalData.brands = manufacturers
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                buildMap()
                withContext(Dispatchers.Main) {
                    App.globalData.brandLoadTime = Date(System.currentTimeMillis()).time
                    pbDownload?.visibility = View.GONE
                    recyclerview?.adapter?.notifyDataSetChanged()
                }
            }
            catch (ex:Exception)
            {
                buildMap()
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    pbDownload?.visibility = View.GONE
                }
                Log.e("Load Manufacturers",ex.message?:"")
            }
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
            val letter:String = item.description.trim().toUpperCase(Locale.ENGLISH).substring(0,1)
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
        if (App.globalData.manufacturersMap == null)
            App.globalData.manufacturersMap = fetchManufacturerMap(App.globalData.brands)
        App.globalData.manufacturersMap?.forEach { (_, value) ->
            if (value.isNotEmpty()) {
                for (manufacturer in value) {
                    all.add(manufacturer)
                }
            }
        }
        filteredList.clear()
        filteredList.addAll(all)
        assignHeaders()
    }



    private fun fetchManufacturerMap(manufacturers: Brands?): Map<String, List<Brand>>? {
        val map: MutableMap<String, List<Brand>> = LinkedHashMap()
        for (element in getAlphaNumeric()) {
            val filteredManufacturers = getManufacturersWithLetter(manufacturers?.brands, element)
            if (filteredManufacturers.isNotEmpty()) {
                map[element.toString()] = filteredManufacturers
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

    private fun getManufacturersWithLetter(manufacturers: List<Brand>?, letter: Char): List<Brand> {
        val manufacturersList: MutableList<Brand> = ArrayList()
        if (manufacturers != null) {
            for (manufacturer in manufacturers) {
                if (manufacturer.description.isNotEmpty() && manufacturer.description[0] == letter) {
                    manufacturersList.add(manufacturer)
                }
            }
        }
        return manufacturersList
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        val qText =  newText?.trim()
        filteredList.clear()
        if (!TextUtils.isEmpty(qText)) {
            for (manufacturer in all) {
                if (manufacturer.description.toLowerCase(Locale.getDefault()).contains(qText?.toLowerCase(Locale.getDefault()).toString())) {
                    filteredList.add(manufacturer)
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
            val manufacturer: Brand = this@ManufacturerSearchFragment.filteredList[position]

            val master = manufacturer.mda
            holder.masterDistributor.visibility = if (master) View.VISIBLE else View.GONE
            holder.tvItem.text = manufacturer.description
            holder.itemView.setOnClickListener {
                val b = Bundle()
                b.putString("manufacturer", manufacturer.toString())
                mAnalyticsViewModel?.mFirebaseAnalytics?.logEvent("ManufacturerSelected", b)
                hideKeyboard()
                val gson: Gson? = Gson()
                val bundle: Bundle? = bundleOf(MANUFACTURER_ARG to gson?.toJson(manufacturer))
                bundle?.putString(SEARCH_TITLE,manufacturer.description)
                bundle?.putString(SEARCH_TERM,"")
                bundle?.putString(SEARCH_DATA,manufacturer.prodCode)
                bundle?.putString(SEARCH_TYPE, Constants.SEARCH_TYPE_MANUFACTURER)
                findNavController().navigate(R.id.action_manufacturerSearchFragment_to_shops_part_result_fragment, bundle, null, null)
            }
            if(manufacturer.headerLetter.isEmpty())
                holder.header.visibility = View.GONE
            else
                holder.header.visibility = View.VISIBLE
            holder.tvTitle.text = manufacturer.headerLetter
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.manufacturer_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int = filteredList.count()

    }

    class ItemViewHolder internal constructor(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val tvItem: TextView = rootView.findViewById(R.id.tvItem)
        val masterDistributor: Chip = rootView.findViewById(R.id.masterDistributor)
        val approvedPartner: Chip = rootView.findViewById(R.id.approvedPartner)
        val tvTitle: TextView = rootView.findViewById(R.id.tvTitle)
        val header: ConstraintLayout = rootView.findViewById(R.id.header)

    }
}

