package uk.co.firstchoice_cs.core.shared

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.api.v4API.Document
import uk.co.firstchoice_cs.core.api.v4API.EquipmentCategory
import uk.co.firstchoice_cs.core.api.v4API.Manufacturer
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.LoggingHelper
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.viewmodels.SearchViewModel
import uk.co.firstchoice_cs.firstchoice.R


open class SearchFragment : Fragment(R.layout.fragment_search), KoinComponent {

    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private lateinit var mViewModel: SearchViewModel


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(act).get(SearchViewModel::class.java)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goBack()
            }
        }
        act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        initComponents()


    }

    private fun goBack()
    {
        mViewModel.selectedEquipment = null
        mViewModel.selectedManufacturer = null
        NavHostFragment.findNavController(this@SearchFragment).navigateUp()
    }

    private fun getManufacturers() {
       if(AppStatus.INTERNET_CONNECTED) {
           GlobalScope.launch(Dispatchers.IO) {
               mViewModel.manufacturers = V4APICalls.manufacturersWithManuals()
               mViewModel.manufacturers?.manufacturers
           }
       }
   }



    private fun getEquipmentTypes() {
        if (AppStatus.INTERNET_CONNECTED) {
            GlobalScope.launch(Dispatchers.IO) {
                val id = mViewModel.selectedManufacturer?.prodCode
                if (id != null) {
                    val res = V4APICalls.filterManufacturersWithManuals(id)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    withContext(Dispatchers.Main) {
                        if (res != null && !res.manufacturers.isNullOrEmpty() && toolbar != null) {
                            mViewModel.equipmentTypesResult = res
                            if (mViewModel.equipmentTypes == null)
                                mViewModel.equipmentTypes = ArrayList()
                            else
                                mViewModel.equipmentTypes?.clear()
                            val vmEquipmentTypes = mViewModel.equipmentTypes
                            if (vmEquipmentTypes!=null) {
                                val topLevels = res.manufacturers[0].topLevel
                                if (topLevels != null) {
                                    for (level in topLevels) {
                                        vmEquipmentTypes.addAll(level.equipmentCategory)
                                    }
                                }
                                mViewModel.selectedEquipment = vmEquipmentTypes[0]
                                setSelectedEquipmentType()
                            }
                        }
                    }
                }
            }
        } else {
            Alerts.showNoInternetToast()
        }
    }

    private fun getModels() {
        if (AppStatus.INTERNET_CONNECTED) {
            lifecycleScope.launch(Dispatchers.IO) {
                val prodCode = mViewModel.equipmentTypesResult?.manufacturers?.get(0)?.prodCode
                val eqCat =mViewModel.selectedEquipment?.id
                if (prodCode != null && eqCat !=null) {
                    val res = V4APICalls.ncDocuments(prodCode,eqCat,"","","",0)
                    if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                        return@launch
                    val docs = res?.documents
                    withContext(Dispatchers.Main) {
                        if (docs != null && toolbar != null) {
                            mViewModel.documents = docs
                            if(docs.isNullOrEmpty())
                                mViewModel.documents = null
                            else
                                mViewModel.selectedModel = docs[0]
                            setSelectedModel()
                        }
                    }
                }
            }
        } else {
            Alerts.showNoInternetToast()
        }
    }

    private fun clearSelectedEquipmentType()
    {
        mViewModel.equipmentTypes = null
        mViewModel.selectedEquipment = null
        equipmenttype.text?.clear()
    }

    private fun clearSelectedModelType()
    {
        mViewModel.documents = null
        mViewModel.selectedModel = null
        models.text?.clear()
    }

    private fun setSelectedManufacturer()
    {
        val man = mViewModel.selectedManufacturer
        if(man!=null) {
            manufacturer.setText(man.manufacturer)
            getEquipmentTypes()
        }
    }

    private fun setSelectedEquipmentType()
    {
        val selected = mViewModel.selectedEquipment
        if(selected!=null) {
            equipmenttype.setText(selected.description)
            getModels()
        }
    }

    private fun setSelectedModel()
    {
        val model = mViewModel.selectedModel
        if(model!=null) {
            models.setText(model.fccPart)
            showResultsButton()
        }
    }

    private fun showResultsButton()
    {
        show_results_button.isEnabled = true
    }


    private fun initComponents() {

        dismissKeyBoard()
        getManufacturers()


        manufacturer.setOnClickListener {
            if(AppStatus.INTERNET_CONNECTED) {
                val man = mViewModel.manufacturers
                if (man != null) {
                    showManufacturers()
                    clearSelectedEquipmentType()
                }
            }
            else
            {
                Toast.makeText(ctx, "Please connect to the internet to search", Toast.LENGTH_SHORT).show()
            }
        }

        equipmenttype.setOnClickListener {
            if(AppStatus.INTERNET_CONNECTED && mViewModel.selectedManufacturer!=null) {
                val eq = mViewModel.equipmentTypes
                if (eq != null) {
                    showEquipmentSearch()
                    clearSelectedModelType()
                }
            }
            else if(AppStatus.INTERNET_CONNECTED && mViewModel.selectedManufacturer==null) {
                Toast.makeText(ctx, "Please select a manufacturer", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(ctx, "Please connect to the internet to search", Toast.LENGTH_SHORT).show()
            }
        }

        models.setOnClickListener {
            if(AppStatus.INTERNET_CONNECTED && mViewModel.selectedManufacturer!=null  && mViewModel.selectedEquipment!=null) {
                val doc = mViewModel.documents
                if (doc != null) {
                    showModelSearch()
                }
            }
            else if(AppStatus.INTERNET_CONNECTED && mViewModel.selectedEquipment==null) {
                Toast.makeText(ctx, "Please select a equipment type", Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(ctx, "Please connect to the internet to search", Toast.LENGTH_SHORT).show()
            }
        }



        show_results_button.setOnClickListener{
            val man = mViewModel.selectedManufacturer
            val cat = mViewModel.selectedEquipment
            val mod = mViewModel.selectedModel
            if(man==null || cat ==null || mod == null) {
                Alerts.showAlert(ctx.getString(R.string.more_info), ctx.getString(R.string.apply_filters),null)
            }
            else
            {
                if(AppStatus.INTERNET_CONNECTED) {
                    showResultsClicked(man,cat,mod)
                }
                else
                {
                    Toast.makeText(ctx,"Please connect to the internet to perform a search", Toast.LENGTH_SHORT).show()
                }
            }
        }

        view_catalogues_button.setOnClickListener {
            gotoCatalogues()
        }

        toolbar.setNavigationOnClickListener {
            goBack()
        }
    }





    private fun showResultsClicked(manufacturer: Manufacturer, equipmentType: EquipmentCategory?,mod:Document?) {
        val bundle = Bundle()
        bundle.putString(ManualsSearchResultsFragment.ARG_1, manufacturer.prodCode)
        bundle.putString(ManualsSearchResultsFragment.ARG_4, manufacturer.manufacturer)
        if(equipmentType!=null) {
            bundle.putString(ManualsSearchResultsFragment.ARG_2, equipmentType.description)
            bundle.putString(ManualsSearchResultsFragment.ARG_3, equipmentType.id)
        }
        if(mod!=null)
        {
            LoggingHelper.debugMsg("showResultsClicked","module")
        }
        else
        {
            bundle.putString(ManualsSearchResultsFragment.ARG_2, "")
            bundle.putString(ManualsSearchResultsFragment.ARG_3, "")
        }

        gotoManualSearch(bundle)
    }




    private fun showEquipmentSearch() {
        setFragmentResultListener(SearchEquipmentTypes.REQUEST_KEY) { _, _ ->
            setSelectedEquipmentType()
        }
        gotoSearchEquipmentTypes()
    }


    private fun showManufacturers() {
        setFragmentResultListener(SearchManufacturers.REQUEST_KEY) { _, _ ->
            setSelectedManufacturer()
        }

        gotoSearchManufacturer()
    }

    private fun showModelSearch() {
        setFragmentResultListener(SearchModels.REQUEST_KEY) { _, _ ->
            setSelectedModel()
        }

        gotoSearchModels()
    }

    open fun gotoSearchModels() {
        //NavHostFragment.findNavController(this@SearchFragment).navigate(R.id.action_searchFragment_to_searchManufacturer)
    }
    open fun gotoSearchManufacturer() {
        //NavHostFragment.findNavController(this@SearchFragment).navigate(R.id.action_searchFragment_to_searchManufacturer)
    }
    open fun gotoSearchEquipmentTypes() {
       // NavHostFragment.findNavController(this@SearchFragment).navigate(R.id.action_searchFragment_to_searchEquipmentTypes)
    }
    open fun gotoManualSearch(bundle: Bundle) {
       // NavHostFragment.findNavController(this@SearchFragment).navigate(R.id.action_searchFragment_to_manualsSearchFragment, bundle)
    }
    open fun gotoCatalogues() {
        //NavHostFragment.findNavController(this@SearchFragment).navigate(R.id.action_searchFragment_to_catalogueFragment)
    }

    private val focused: EditText?
        get() {
            if (manufacturer.isFocused) return manufacturer
            return if (equipmenttype.isFocused) return equipmenttype
            else null
        }

    private fun dismissKeyBoard() {
        val focused: View? = focused
        if (focused != null) Helpers.hideKeyboard(focused)
    }

    companion object
}