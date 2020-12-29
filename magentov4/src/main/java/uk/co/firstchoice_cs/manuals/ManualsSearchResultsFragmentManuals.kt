package uk.co.firstchoice_cs.manuals

import androidx.navigation.fragment.NavHostFragment
import uk.co.firstchoice_cs.core.shared.ManualsSearchResultsFragment
import uk.co.firstchoice_cs.firstchoice.R

class ManualsSearchResultsFragmentManuals : ManualsSearchResultsFragment(){

    override fun showManualsDetails()
    {
        try {
            NavHostFragment.findNavController(this@ManualsSearchResultsFragmentManuals).navigate(R.id.action_manualsSearchFragment_to_manuals_details_fragment)
        } catch (ex: IllegalArgumentException) {

        }
    }

}