package uk.co.firstchoice_cs.store.manuals_search

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.ManualsSearchResultsFragment
import uk.co.firstchoice_cs.firstchoice.R


class ManualsSearchResultsFragmentStore : ManualsSearchResultsFragment(){

    override fun showManualsDetails()
    {
        NavHostFragment.findNavController(this@ManualsSearchResultsFragmentStore).navigate(R.id.action_manualsSearchFragment_to_manuals_details_fragment)
    }
}