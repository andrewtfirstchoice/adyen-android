package uk.co.firstchoice_cs.manuals

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.CollectionFragment
import uk.co.firstchoice_cs.firstchoice.R

class CollectionFragmentManuals : CollectionFragment(){


    override fun gotoAddEditCollectionsFragment()
    {
        NavHostFragment.findNavController(this@CollectionFragmentManuals).navigate(R.id.collections_fragment_to_add_edit_collections_fragment)
    }

    override fun gotoItemsInCollectionsFragment()
    {
        NavHostFragment.findNavController(this@CollectionFragmentManuals).navigate(R.id.collection_fragment_to_items_in_collection_fragment)
    }

    override fun gotoManualsDetailsFragment()
    {
        NavHostFragment.findNavController(this@CollectionFragmentManuals).navigate(R.id.collection_fragment_to_manual_details_fragment)
    }
}