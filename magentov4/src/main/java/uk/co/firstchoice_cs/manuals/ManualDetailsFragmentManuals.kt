package uk.co.firstchoice_cs.manuals

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.helpers.Helpers.navigateToAddPartFragmentWithSuperC
import uk.co.firstchoice_cs.core.shared.ManualDetailsFragment
import uk.co.firstchoice_cs.firstchoice.R


class ManualDetailsFragmentManuals : ManualDetailsFragment()
{
    override fun gotoCollectionFragment() {
        NavHostFragment.findNavController(this@ManualDetailsFragmentManuals).navigate(R.id.manual_details_fragment_to_add_to_collection_fragment)
    }

    override fun loadProductPage(product: Product) {
        navigateToAddPartFragmentWithSuperC(product,R.id.action_manuals_details_fragment_manuals_to_addPartFragment,this@ManualDetailsFragmentManuals)
    }
}