package uk.co.firstchoice_cs.core.onboarding

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.on_boarding_fragment_page.*
import uk.co.firstchoice_cs.firstchoice.R

class OnBoardingFragmentPage(private var position: Int) : Fragment(R.layout.on_boarding_fragment_page) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when (position) {
            0 -> mainImage.setImageDrawable( ContextCompat.getDrawable(requireContext(),R.drawable.screen1))
            1 -> mainImage.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.screen2))
            2 -> mainImage.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.screen3))
            3 -> mainImage.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.screen5))
            4 -> mainImage.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.screen6))
        }
    }
}