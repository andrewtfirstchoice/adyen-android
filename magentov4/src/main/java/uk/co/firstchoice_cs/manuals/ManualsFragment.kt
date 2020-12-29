package uk.co.firstchoice_cs.manuals

import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.database.entities.CollectionItem
import uk.co.firstchoice_cs.core.database.entities.CollectionList
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.shared.*
import uk.co.firstchoice_cs.core.viewmodels.*
import uk.co.firstchoice_cs.firstchoice.R


class ManualsFragmentManuals : ManualsFragment(){

    override fun showCollectionView(collectionList: CollectionList) {
        val vm = ViewModelProvider(act).get(CollectionViewModel::class.java)
        vm.collectionList = collectionList
        NavHostFragment.findNavController(this@ManualsFragmentManuals).navigate(R.id.manuals_fragment_to_collection_fragment)
    }

    override fun showItemsInCollectionFragment(collectionItem: CollectionItem?, documentEntry: DocumentEntry) {
        val vm = ViewModelProvider(act).get(ItemInCollectionsViewModel::class.java)
        vm.collectionItem = collectionItem
        vm.focusedDocument = documentEntry
        NavHostFragment.findNavController(this@ManualsFragmentManuals).navigate(R.id.manuals_fragment_to_items_in_collections_fragment)
    }
    override fun showAddCollectionsFragment() {
        setFragmentResultListener(AddEditCollectionsFragment.REQUEST_KEY) { _, _ ->

        }
        try {
            val mViewModel = ViewModelProvider(act).get(AddEditCollectionsViewModel::class.java)
            mViewModel.currentCollection = null
            mViewModel.isEditMode = false
            NavHostFragment.findNavController(this@ManualsFragmentManuals).navigate(R.id.manuals_fragment_to_add_edit_collection_fragment)
        }catch (ex:Exception) {}
    }

    override fun showSearchFragment() {
        val mViewModel: SearchViewModel = ViewModelProvider(requireActivity()).get(SearchViewModel::class.java)
        mViewModel.selectedManufacturer = null
        mViewModel.selectedEquipment = null
        try {
            val bundle = Bundle()
            NavHostFragment.findNavController(this@ManualsFragmentManuals).navigate(R.id.action_manuals_fragment_to_searchFragment, bundle)
        }catch (ex:Exception) {}
    }

    override fun launchDocument(doc: DocumentEntry) {
        if (doc.isOnDevice) {
            doc.readCount++
            val b = Bundle()
            b.putString("manufacturer", doc.manufacturer.Name)
            b.putString("model", doc.model.Name)
            mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("ManualViewed", b)
            val vm = ViewModelProvider(requireActivity()).get(ManualDetailsViewModel::class.java)
            doc.v4Doc = null
            vm.doc = doc

            NavHostFragment.findNavController(this@ManualsFragmentManuals
            ).navigate(R.id.manuals_fragment_to_manual_details)
        }
    }
}