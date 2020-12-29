package uk.co.firstchoice_cs.store.fragments

import android.os.Bundle
import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.api.v4API.Model
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.shared.AddPartFragment
import uk.co.firstchoice_cs.firstchoice.R


class AddPartFragmentStore : AddPartFragment() {

    override fun goTo360View(product: Part) {
        val bundle = Bundle()
        val url = "https://360spin.firstchoice-cs.co.uk/spin/part/e/h/" + product.barcode
        bundle.putString("url", url)
        NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_addPartFragment_to_three_sixty_fragment_store, bundle, null, null)
    }

    override fun navigateToModel(model:Model)
    {
        NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_addPartFragment_to_shop_models_fragment_store, null, null, null)
    }

    override fun navigateToStore()
    {
        NavHostFragment.findNavController(requireParentFragment()).navigate(R.id.action_addPartFragment_to_manuals_details_fragment_store, null, null, null)
    }

}
