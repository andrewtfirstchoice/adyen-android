package uk.co.firstchoice_cs.store.bottom_sheets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.core.api.v4API.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FilterBottomSheetBinding
import java.util.*
import kotlin.collections.ArrayList


interface FilterBottomSheetInterface {
    fun cancelFilters()
    fun applyFilters(selectedFiltersMan: java.util.ArrayList<Manufacturer>, selectedFiltersClass: java.util.ArrayList<Clas>, selectedFiltersEq: ArrayList<EquipmentCategory>)
}

class FilterBottomSheet : RelativeLayout,SearchView.OnQueryTextListener {

    private var sectionedAdapter: SectionedRecyclerViewAdapter? = null
    private var sheetCollapsed = true
    private var ncFilters: NCFilter? = null
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    private var callback: FilterBottomSheetInterface? = null
    private var searchTerm = ""
    private var searchTermEncoded = ""
    private var searchType = ""
    private var searchData = ""
    private var searchTitle = ""
    private var selectedFiltersMan:ArrayList<Manufacturer> = ArrayList()
    private var selectedFiltersClass:ArrayList<Clas> = ArrayList()
    private var allListManufacturer:ArrayList<Manufacturer> = ArrayList()
    private var allListClass:ArrayList<Clas> = ArrayList()
    private var filteredListManufacturer:ArrayList<Manufacturer> = ArrayList()
    private var filteredListClass:ArrayList<Clas> = ArrayList()
    private var selectedFiltersEq:ArrayList<EquipmentCategory> = ArrayList()
    private var filteredListEqCat:ArrayList<EquipmentCategory> = ArrayList()
    private var allListEqCat:ArrayList<EquipmentCategory> = ArrayList()
    private var currentMode: FilterMode = FilterMode.Manufacturer
    private var currentSearch:String = ""
    lateinit var binding:FilterBottomSheetBinding
    enum class FilterMode {
        Manufacturer, Equipment, Class
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }


    private fun init() {
        View.inflate(context, R.layout.filter_bottom_sheet, this)
        binding = FilterBottomSheetBinding.bind(this)
        if(!isInEditMode) {
            initComponents()
        }
    }



    fun setSheetBehavior(sheetBehavior: BottomSheetBehavior<*>?) {
        this.sheetBehavior = sheetBehavior
    }

    fun setCallback(callback: FilterBottomSheetInterface) {
        this.callback = callback
    }

    private fun showSubPanel(titleStr:String)
    {
        binding.title.text = titleStr
        binding.subFilterScreen.visibility = View.VISIBLE
        binding.mainFilterScreen.visibility = View.GONE
        binding.closeButton.icon = ContextCompat.getDrawable( binding.closeButton.context,R.drawable.ic_baseline_arrow_back_24)
    }

    private fun applySearchFilter()
    {
        filteredListManufacturer.clear()
        filteredListClass.clear()
        filteredListEqCat.clear()

        if(currentSearch.isBlank()) {
            filteredListManufacturer.addAll(allListManufacturer)
            filteredListClass.addAll(allListClass)
            filteredListEqCat.addAll(allListEqCat)
        }
        else
        {
            filteredListManufacturer.addAll(allListManufacturer.filter {
                !it.manufacturer.isNullOrEmpty() && it.manufacturer.contains(currentSearch,true)
            })
            filteredListClass.addAll(allListClass.filter { it.description.contains(currentSearch,true)  })
            filteredListEqCat.addAll(allListEqCat.filter { it.tlc.toString().contains(currentSearch,true)  })
        }
        val adapter: MyAdapter =   binding.recycler.adapter as MyAdapter
        adapter.dataChanged()

    }

    private fun showMainPanel()
    {
        binding.title.text = context.getString(R.string.filters)
        binding.subFilterScreen.visibility = View.GONE
        binding.mainFilterScreen.visibility = View.VISIBLE
        binding.closeButton.icon = ContextCompat.getDrawable( binding.closeButton.context,R.drawable.ic_close_black_24dp)
    }

    private fun initComponents() {
        binding.searchView.setOnQueryTextListener(this)
        showMainPanel()
        sectionedAdapter = SectionedRecyclerViewAdapter()
        val layoutManager = LinearLayoutManager(context)

        val dividerItemDecoration = DividerItemDecoration( binding.recycler.context, layoutManager.orientation)
        dividerItemDecoration.let {  binding.recycler.addItemDecoration(it) }
        binding.recycler.layoutManager = layoutManager
        binding.recycler.adapter = MyAdapter()

        binding.applyButton.setOnClickListener{

            if(selectedFiltersMan.isEmpty()&&selectedFiltersClass.isEmpty()&&selectedFiltersEq.isEmpty())
            {
                callback?.cancelFilters()
            }
            else {
                callback?.applyFilters(selectedFiltersMan, selectedFiltersClass, selectedFiltersEq)
            }
            hide()
        }

        binding.eqItem.setOnClickListener{
            currentMode = FilterMode.Equipment
            binding.searchView.queryHint = "Search by Equipment Type"
            showSubPanel("Equipment Types")
            applySearchFilter()
        }

        binding.manItem.setOnClickListener{
            currentMode = FilterMode.Manufacturer
            binding.searchView.queryHint = "Search by Manufacturer"
            showSubPanel("Manufacturers")
            applySearchFilter()
        }

        binding.classItem.setOnClickListener{
            currentMode = FilterMode.Class
            binding.searchView.queryHint = "Search by Category"
            showSubPanel("Part Categories")
            applySearchFilter()
        }

        binding.backClicker.setOnClickListener {
            if( binding.mainFilterScreen.visibility==View.VISIBLE)
                hide()
            else {
                showMainPanel()
                setCounts()
                renderCounts()
            }
        }

        binding.clearClicker.setOnClickListener {
            clearCounts()
            setCounts()
            renderCounts()
            if(binding.mainFilterScreen.visibility == View.GONE)
                showMainPanel()
        }
    }

    private fun clearCounts() {
        clearSelectedFilters()

        allListManufacturer.forEach { it.selected=false }
        allListClass.forEach { it.selected=false }
        allListEqCat.forEach { it.selected=false }
    }

    private fun clearSelectedFilters()
    {
        selectedFiltersMan.clear()
        selectedFiltersClass.clear()
        selectedFiltersEq.clear()
    }

    private fun setCounts() {
        clearSelectedFilters()
        allListManufacturer.forEach { if (it.selected) selectedFiltersMan.add(it) }
        allListClass.forEach { if (it.selected) selectedFiltersClass.add(it) }
        allListEqCat.forEach { if (it.selected) selectedFiltersEq.add(it) }
    }

    private fun renderCounts() {
        binding.classDescription.text = context.getString(R.string.filters_selected,selectedFiltersClass.size)
        binding.manDescription.text = context.getString(R.string.filters_selected,selectedFiltersMan.size)
        binding.eqDescription.text = context.getString(R.string.filters_selected,selectedFiltersEq.size)
    }


    fun expand() {
        sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        sheetCollapsed = false
    }

    fun hide() {
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        sheetCollapsed = true
    }

    fun initFilter(searchData: String, searchTerm: String,searchTermEncoded: String,searchTitle: String, searchType: String): Boolean {
        this.searchData = searchData
        this.searchTerm = searchTerm
        this.searchTermEncoded = searchTermEncoded
        this.searchTitle = searchTitle
        this.searchType = searchType
        filter()
        return true
    }

    private fun filter() {
        binding.manItem.visibility =   View.GONE
        binding.classItem.visibility = View.GONE
        binding.eqItem.visibility =   View.GONE
        binding.progress.visibility = View.VISIBLE

        val filterMan = if(searchType == Constants.SEARCH_TYPE_MANUFACTURER) searchData else ""
        val filterEq  = if(searchType == Constants.SEARCH_TYPE_EQUIPMENT_CATEGORY) searchData else ""
        val filterCl  = if(searchType == Constants.SEARCH_TYPE_CLASS_ID) searchData else ""
        val topLevel  = if(searchType == Constants.SEARCH_TYPE_TOP_LEVEL) searchData else ""

        filterParts(filterMan,filterEq,filterCl,topLevel, searchTerm)
    }

    private fun filterParts(manufacturer:String,equipment:String,classID:String,topLevel:String,search:String)
    {
        GlobalScope.launch(context = Dispatchers.IO) {

            try {
                ncFilters = V4APICalls.ncFilters(manufacturer,equipment,classID,topLevel, search,0)
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                    hideShowAsRequired()
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progress.visibility = View.GONE
                }
            }
        }
    }


    private fun buildMap()
    {
        allListEqCat.clear()
        val categories = ncFilters?.filters?.get(0)?.topLevel
        if (categories != null) {
            for (cat in categories.toList()) {
                val eqCats = cat.equipmentCategory.toList().sortedBy { it.description }
                for (eq in eqCats) {
                    eq.TopLevel = cat.tlc
                    eq.TopLevelDesc = cat.description
                    allListEqCat.add(eq)
                }
            }
        }

        filteredListEqCat.clear()
        filteredListEqCat.addAll(allListEqCat)
    }




    private fun hideShowAsRequired()
    {
        if(!ncFilters?.filters.isNullOrEmpty()) {

            allListEqCat.clear()
            allListClass.clear()
            allListManufacturer.clear()

            ncFilters?.filters?.get(0)?.manufacturer?.let { allListManufacturer.addAll(it) }
            ncFilters?.filters?.get(0)?.`class`?.let { allListClass.addAll(it) }
            buildMap()

            applySearchFilter()
        }
        binding.manItem.visibility =  if(allListManufacturer.size > 0) View.VISIBLE else View.GONE
        binding.classItem.visibility =  if(allListClass.size > 0) View.VISIBLE else View.GONE
        binding.eqItem.visibility =  if(allListEqCat.size > 0) View.VISIBLE else View.GONE
    }


    inner class MyAdapter : RecyclerView.Adapter<ItemViewHolder>()
    {
        fun dataChanged()
        {
            assignHeaders()
            notifyDataSetChanged()
        }

        private fun assignHeaders()
        {
            var currentLetter = ""
            when (currentMode) {
                FilterMode.Manufacturer -> {
                    for (item in filteredListManufacturer) {
                        val manufacturer = item.manufacturer
                        if(!manufacturer.isNullOrBlank()) {
                            val letter: String = manufacturer.trim().toUpperCase(Locale.ENGLISH).substring(0, 1)
                            if (letter != currentLetter.toUpperCase(Locale.ENGLISH)) {
                                currentLetter = letter.toUpperCase(Locale.ENGLISH)
                                item.headerLetter = currentLetter
                            } else {
                                item.headerLetter = ""
                            }
                        }
                    }
                }
                FilterMode.Class -> {
                    for (item in filteredListClass) {
                        val letter: String = item.description.trim().toUpperCase(Locale.ENGLISH).substring(0, 1)
                        if (letter != currentLetter.toUpperCase(Locale.ENGLISH)) {
                            currentLetter = letter.toUpperCase(Locale.ENGLISH)
                            item.headerLetter = currentLetter
                        } else {
                            item.headerLetter = ""
                        }
                    }
                }
                FilterMode.Equipment -> {
                    for (item in filteredListEqCat) {
                        val letter: String = item.TopLevelDesc.trim()
                        if (letter != currentLetter) {
                            currentLetter = letter
                            item.headerLetter = currentLetter
                        } else {
                            item.headerLetter = ""
                        }
                    }
                }
            }
        }


        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            when (currentMode) {
                FilterMode.Manufacturer -> {
                    val manufacturer: Manufacturer = filteredListManufacturer[position]
                    holder.tvItem.text = manufacturer.manufacturer
                    holder.itemView.setOnClickListener {
                        manufacturer.selected=!manufacturer.selected
                        holder.trailing.visibility = if(manufacturer.selected)View.VISIBLE else View.INVISIBLE
                    }
                    if (manufacturer.headerLetter.isNullOrEmpty())
                        holder.header.visibility = View.GONE
                    else
                        holder.header.visibility = View.VISIBLE
                    holder.tvTitle.text = manufacturer.headerLetter
                    holder.trailing.visibility = if(manufacturer.selected)View.VISIBLE else View.INVISIBLE
                }
                FilterMode.Class -> {
                    val clas: Clas = filteredListClass[position]
                    holder.tvItem.text = clas.description
                    holder.itemView.setOnClickListener {
                        clas.selected=!clas.selected
                        holder.trailing.visibility = if(clas.selected)View.VISIBLE else View.INVISIBLE
                    }
                    if (clas.headerLetter.isNullOrEmpty())
                        holder.header.visibility = View.GONE
                    else
                        holder.header.visibility = View.VISIBLE
                    holder.tvTitle.text = clas.headerLetter
                    holder.trailing.visibility = if(clas.selected)View.VISIBLE else View.INVISIBLE
                }
                FilterMode.Equipment -> {
                    val topLevel: EquipmentCategory = filteredListEqCat[position]
                    holder.tvItem.text = topLevel.description
                    holder.itemView.setOnClickListener {
                        topLevel.selected=!topLevel.selected
                        holder.trailing.visibility = if(topLevel.selected)View.VISIBLE else View.INVISIBLE
                    }
                    if (topLevel.headerLetter.isEmpty())
                        holder.header.visibility = View.GONE
                    else
                        holder.header.visibility = View.VISIBLE
                    holder.tvTitle.text = topLevel.headerLetter
                    holder.trailing.visibility = if(topLevel.selected)View.VISIBLE else View.INVISIBLE
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.filter_item, parent, false)
            return ItemViewHolder(view)
        }


        override fun getItemCount(): Int {
            return when (currentMode) {
                FilterMode.Manufacturer -> {
                    filteredListManufacturer.count()
                }
                FilterMode.Class -> {
                    filteredListClass.count()
                }
                else -> {
                    filteredListEqCat.count()
                }
            }
        }
    }

    class ItemViewHolder internal constructor(rootView: View) : RecyclerView.ViewHolder(rootView) {
        val tvItem: TextView = rootView.findViewById(R.id.tvItem)
        val tvTitle: TextView = rootView.findViewById(R.id.tvTitle)
        val header: ConstraintLayout = rootView.findViewById(R.id.header)
        val trailing: MaterialButton = rootView.findViewById(R.id.check)
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            currentSearch = newText
            applySearchFilter()
        }

        return true
    }
}