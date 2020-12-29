package uk.co.firstchoice_cs.manuals

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.SearchFragment
import uk.co.firstchoice_cs.firstchoice.R


class SearchFragmentManuals : SearchFragment() {

    override fun gotoSearchModels() {
        NavHostFragment.findNavController(this@SearchFragmentManuals).navigate(R.id.action_searchFragment_to_searchModelFragment)
    }

    override fun gotoSearchManufacturer() {
        NavHostFragment.findNavController(this@SearchFragmentManuals).navigate(R.id.action_searchFragment_to_searchManufacturer)
    }
    override fun gotoSearchEquipmentTypes() {
         NavHostFragment.findNavController(this@SearchFragmentManuals).navigate(R.id.action_searchFragment_to_searchEquipmentTypes)
    }
    override fun gotoManualSearch(bundle: Bundle) {
         NavHostFragment.findNavController(this@SearchFragmentManuals).navigate(R.id.action_searchFragment_to_manualsSearchFragment, bundle)
    }
    override fun gotoCatalogues() {
        NavHostFragment.findNavController(this@SearchFragmentManuals).navigate(R.id.action_searchFragment_to_catalogueFragment)
    }

}