package uk.co.firstchoice_cs.store.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.database.recent_searches.RecentSearchItem
import uk.co.firstchoice_cs.core.database.recent_searches.RecentSearchViewModel
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.RootCardRecentlyViewedBinding
import uk.co.firstchoice_cs.firstchoice.databinding.ShopRootFragmentBinding
import java.util.*


class ShopRootFragment : Fragment(R.layout.shop_root_fragment) {

    private val recentlyViewedList: ArrayList<RecentSearchItem> = ArrayList()
    private val recentlyViewedAdapter = RecentlyViewedRecyclerViewAdapter()

    lateinit var binding:ShopRootFragmentBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ShopRootFragmentBinding.bind(view)

        binding.documentSearchButton.binding.card.setOnClickListener{
            NavHostFragment.findNavController(this@ShopRootFragment).navigate(R.id.action_shopRootFragment_to_searchFragment)
        }
        binding.manufacturerSearchButton.binding.card.setOnClickListener{
            goToManufacturerSearch()
        }
        binding.categorySearchButton.binding.card.setOnClickListener{
            goToPartCategorySearch()
        }
        binding.equipmentSearchButton.binding.card.setOnClickListener{
            goToEquipmentSearch()
        }

        setupRecentlyViewedRecycler()

        binding.clicker.setOnClickListener{
            goToDetail()
        }


        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@ShopRootFragment).navigateUp()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        val recentSearchViewModel = ViewModelProvider(requireActivity()).get(RecentSearchViewModel::class.java)
        recentSearchViewModel.allSearches.observe(requireActivity(), {
            val searches = it
            recentlyViewedList.clear()
            recentlyViewedList.addAll(searches.reversed())
            recentlyViewedAdapter.notifyDataSetChanged()

            if (recentlyViewedList.isEmpty())
                binding.noRecentlyViewedText.visibility = View.VISIBLE
            else
                binding.noRecentlyViewedText.visibility = View.GONE
        })

        Helpers.hideKeyboard(binding.searchView)
    }

    private fun setupRecentlyViewedRecycler()
    {
        val layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
        binding.recentlyViewedRecycler.adapter = recentlyViewedAdapter
        binding.recentlyViewedRecycler.layoutManager = layoutManager
    }


    private fun goToEquipmentSearch() {
        findNavController().navigate(
            R.id.action_store_fragment_to_equipmentSearchFragment,
            null,
            null,
            null
        )
    }

    private fun goToManufacturerSearch() {
        findNavController().navigate(
            R.id.action_store_fragment_to_manufacturerFragment,
            null,
            null,
            null
        )
    }

    private fun goToPartCategorySearch() {
        findNavController().navigate(
            R.id.action_store_fragment_to_partCategorySearchFragment,
            null,
            null,
            null
        )
    }

    private fun goToDetail() {
        findNavController().navigate(
            R.id.action_store_fragment_to_storeDetailFragment,
            null,
            null,
            null
        )
    }


    inner class RecentlyViewedRecyclerViewAdapter() : RecyclerView.Adapter<RecentlyViewedRecyclerViewAdapter.RecentViewHolder>()
    {
        override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
            val item = recentlyViewedList[position]
            val product = Gson().fromJson(item.data, Part::class.java)
            holder.itemView.setOnClickListener{
                Helpers.navigateToAddPartFragmentWithSuperC(
                    product,
                    R.id.action_shopRootFragment_to_addPartFragment,
                    requireParentFragment()
                )
            }

            holder.binding.manufacturerText.text = product.manufacturer
            holder.binding.descriptionText.text = product.partDescription
            holder.binding.partNumberText.text = product.partDescription

            Helpers.renderImage(holder.binding.image,product.images?.get(0)?.url)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.root_card_recently_viewed,
                parent,
                false
            )
            return RecentViewHolder(view)
        }

        override fun getItemCount(): Int = recentlyViewedList.size

        inner class RecentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            val binding:RootCardRecentlyViewedBinding = RootCardRecentlyViewedBinding.bind(mView)
        }
    }
}
