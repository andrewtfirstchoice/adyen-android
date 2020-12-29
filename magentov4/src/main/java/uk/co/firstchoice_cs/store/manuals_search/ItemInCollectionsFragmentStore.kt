package uk.co.firstchoice_cs.store.manuals_search

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.ItemInCollectionsFragment
import uk.co.firstchoice_cs.firstchoice.R

class ItemInCollectionsFragmentStore : ItemInCollectionsFragment() {

    override fun gotoAddEditCollectionsFragment() {
        NavHostFragment.findNavController(this@ItemInCollectionsFragmentStore).navigate(R.id.item_in_collections_fragment_to_add_edit_collections_fragment_store)
    }
}