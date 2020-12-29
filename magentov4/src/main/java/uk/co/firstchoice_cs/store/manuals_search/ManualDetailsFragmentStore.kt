package uk.co.firstchoice_cs.store.manuals_search


import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.helpers.Helpers.navigateToAddPartFragmentWithSuperC
import uk.co.firstchoice_cs.core.shared.ManualDetailsFragment
import uk.co.firstchoice_cs.firstchoice.R


class ManualDetailsFragmentStore : ManualDetailsFragment()
{
    override fun gotoCollectionFragment() {
        NavHostFragment.findNavController(this@ManualDetailsFragmentStore).navigate(R.id.manual_details_fragment_to_add_to_collection_fragment_store)
    }

    override fun loadProductPage(product: Product) {

        navigateToAddPartFragmentWithSuperC(product,R.id.action_shopModelsResultFragment_to_addPartFragment,this@ManualDetailsFragmentStore)
    }
}