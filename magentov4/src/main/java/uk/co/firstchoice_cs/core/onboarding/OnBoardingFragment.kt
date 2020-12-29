package uk.co.firstchoice_cs.core.onboarding

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.navigation.fragment.NavHostFragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.on_boarding_fragment.*
import uk.co.firstchoice_cs.SavePrefs.onBoardingComplete
import uk.co.firstchoice_cs.firstchoice.R
import kotlin.math.abs

class OnBoardingFragment : Fragment(R.layout.on_boarding_fragment) {

    private var mListener: OnFragmentInteractionListener? = null

    interface OnFragmentInteractionListener {
        fun onBoardingComplete(complete: Boolean)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getStartedButton.setOnClickListener {
            onBoardingComplete()
            mListener?.onBoardingComplete(true)
            goBack()
        }

        val pagerAdapter: PagerAdapter = OnBoardingPagerAdapter(childFragmentManager, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
        viewPager.setPageTransformer(true, ZoomOutPageTransformer())
        viewPager.adapter = pagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                setPage(position)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        dots_indicator.setViewPager(viewPager)
        setPage(0)

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem == 0) {
                    goBack()
                } else {
                    viewPager.currentItem = viewPager.currentItem - 1
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }

    private fun goBack() {
        NavHostFragment.findNavController(this@OnBoardingFragment).navigateUp()
    }

    private fun setPage(position: Int) {
        if (position < 4) getStartedButton.visibility = View.INVISIBLE else getStartedButton.visibility = View.VISIBLE
        var titleText = ""
        when (position) {
            0 -> titleText = "Your One Stop Shop"
            1 -> titleText = "Shop From Anywhere"
            2 -> titleText = "Smarter Manuals"
            3 -> titleText = "Identify Parts"
            4 -> titleText = "My Account"
        }
        title.text = titleText

        var subTitleText = ""
        when (position) {
            0 -> subTitleText = "for Catering Spares"
            1 -> subTitleText = "with the UK's largest stock of OEM parts"
            2 -> subTitleText = "find the parts you need faster"
            3 -> subTitleText = "with help from our expert team"
            4 -> subTitleText = "track your orders & manage your account on the go"
        }
        subTitle.text = subTitleText

        var descriptionText = ""
        when (position) {
            0 -> descriptionText = "Welcome to the new First Choice App!  Browse our Store & quickly find the parts that you need"
            1 -> descriptionText = "See up-to-date prices on our whole range of available parts, including delivery options & part documents.   Checkout quickly & easily"
            2 -> descriptionText = "Search for matching parts directly when viewing an equipment manual, with a convenient search bar that lets you filter through an equipment's parts for the one that you need"
            3 -> descriptionText = "Enter a few pieces of information along with up to three photos and send it to our experienced team.  Now with extra manufacturer & model auto-suggest to help you identify your part"
            4 -> descriptionText = "With an account, you can track your orders & manage your account on the go.  Get the best app experience with us when you sign up!"
        }
        description.text = descriptionText
    }

    /**
     * A simple pager adapter that represents 5 OnBoardingFragment objects, in
     * sequence.
     */

    private class OnBoardingPagerAdapter(fm: FragmentManager?, behavior: Int) : FragmentStatePagerAdapter(fm!!, behavior) {
        override fun getItem(position: Int): Fragment {
            return OnBoardingFragmentPage(position)
        }

        override fun getCount(): Int {
            return NUM_PAGES
        }
    }

    class ZoomOutPageTransformer : ViewPager.PageTransformer {
        override fun transformPage(view: View, position: Float) {
            val pageWidth = view.width
            val pageHeight = view.height
            when {
                position < -1 -> {
                    view.alpha = 0f
                }
                position <= 1 -> {
                    val scaleFactor = MIN_SCALE.coerceAtLeast(1 - abs(position))
                    val v = pageHeight * (1 - scaleFactor) / 2
                    val h = pageWidth * (1 - scaleFactor) / 2
                    if (position < 0) {
                        view.translationX = h - v / 2
                    } else {
                        view.translationX = -h + v / 2
                    }
                    view.scaleX = scaleFactor
                    view.scaleY = scaleFactor
                    view.alpha = MIN_ALPHA +
                            (scaleFactor - MIN_SCALE) /
                            (1 - MIN_SCALE) * (1 - MIN_ALPHA)
                }
                else -> {
                    view.alpha = 0f
                }
            }
        }

        companion object {
            private const val MIN_SCALE = 0.85f
            private const val MIN_ALPHA = 0.5f
        }
    }

    companion object {
        private const val NUM_PAGES = 5
        const val REQUEST_KEY: String = "OnBoardingFragment"
    }
}