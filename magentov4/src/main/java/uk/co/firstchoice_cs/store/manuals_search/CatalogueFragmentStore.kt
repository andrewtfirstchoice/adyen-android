package uk.co.firstchoice_cs.store.manuals_search

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.CatalogueFragment
import uk.co.firstchoice_cs.firstchoice.R

class CatalogueFragmentStore : CatalogueFragment()
{
    override fun gotoManualsDetails() {
        NavHostFragment.findNavController(this@CatalogueFragmentStore).navigate(R.id.action_catalogueFragment_to_manuals_details_fragment)
    }
}
