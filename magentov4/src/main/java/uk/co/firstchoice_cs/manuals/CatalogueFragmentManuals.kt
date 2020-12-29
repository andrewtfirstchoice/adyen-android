package uk.co.firstchoice_cs.manuals

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.CatalogueFragment
import uk.co.firstchoice_cs.firstchoice.R

@Suppress("UNCHECKED_CAST")
class CatalogueFragmentManuals : CatalogueFragment()
{
    override fun gotoManualsDetails() {
        NavHostFragment.findNavController(this@CatalogueFragmentManuals).navigate(R.id.action_catalogueFragment_to_manuals_details_fragment)
    }
}
