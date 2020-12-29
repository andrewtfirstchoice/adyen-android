package uk.co.firstchoice_cs.store.fragments

import android.content.Context
import android.graphics.drawable.ClipDrawable.HORIZONTAL
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.shop_search_fragment.*
import kotlinx.android.synthetic.main.simple_search_item.view.*
import uk.co.firstchoice_cs.Constants.SEARCH_TERM
import uk.co.firstchoice_cs.Constants.SEARCH_TYPE
import uk.co.firstchoice_cs.SavePrefs
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.store.vm.MainActivityViewModel
import uk.co.firstchoice_cs.store.vm.SearchViewModel
import java.util.*


class ShopSearchFragment : Fragment(R.layout.shop_search_fragment), androidx.appcompat.widget.SearchView.OnQueryTextListener{
    private var recentSearches = ArrayList<String>()
    private val recentSearchesAdapter = RecentSearchesAdapter()
    private var mSearchViewModel: SearchViewModel? = null
    private var mFireBaseViewModel: FireBaseViewModel? = null
    private var listener: OnFragmentInteractionListener? = null
    lateinit var mainViewModel: MainActivityViewModel

    interface OnFragmentInteractionListener {
        fun displaySpeechRecognizer()
    }

    private fun setupViewModels() {
        mFireBaseViewModel = ViewModelProvider(this).get(FireBaseViewModel::class.java)
        mSearchViewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)
        mainViewModel.speechLiveData.observe(viewLifecycleOwner, {
            if (it != null) {
                mainViewModel.speechLiveData.removeObservers(this@ShopSearchFragment)
                mainViewModel.speechLiveData.value = null
                val results = it.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    val list = mFireBaseViewModel?.convertWords(results[0])
                    val q = list?.get(0).toString()
                    if (q.isNotBlank()) {
                        searchView.setQuery(q, false)
                        performSearch(q)
                    }
                }
            }
        })
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setupViewModels()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        clearButton.setOnClickListener{
            clearRecentSearches()
        }


        backNavButton.setOnClickListener{
            hideKeyboard()
            Handler().postDelayed({
                NavHostFragment.findNavController(this@ShopSearchFragment).navigateUp()
            }, 100)
        }

        scanButton.setOnClickListener {
            hideKeyboard()
            Handler().postDelayed({
                findNavController().navigate(
                    R.id.action_storeDetailFragment_to_scannerFragment,
                    null,
                    null,
                    null
                )
            }, 100)
        }

        speechButton.setOnClickListener {
            hideKeyboard()
            Handler().postDelayed({
                listener?.displaySpeechRecognizer()
            }, 100)
        }

      searchView.setOnQueryTextListener(this)


        setupRecentlySearchesRecycler()
        hideKeyboard()
        initData()

        super.onViewCreated(view, savedInstanceState)

    }

    private fun clearRecentSearches()
    {
        recentSearches.clear()
        SavePrefs.setArrayPrefs("RECENT_SEARCHES_STRING", recentSearches, requireContext())
        this.recentSearchesAdapter.notifyDataSetChanged()
    }

    private fun initData()
    {
        recentSearches = SavePrefs.getArrayPrefs("RECENT_SEARCHES_STRING", context)
        this.recentSearchesAdapter.notifyDataSetChanged()
    }

    private fun getRecentSearches(): ArrayList<String> {
        return recentSearches
    }

    private fun containsRecentSearch(searchText: String): Boolean {
        for (str in getRecentSearches()) {
            if (str.trim { it <= ' ' }.equals(searchText.trim { it <= ' ' }, ignoreCase = true)) return true
        }
        return false
    }


    private fun addToRecentSearches(searchText: String) {
        recentSearches = SavePrefs.getArrayPrefs(getString(R.string.recent_search_str), context)
        if (containsRecentSearch(searchText))
            return
        recentSearches.add(0, searchText.trim { it <= ' ' })
        //ensure you only have the latest in the order that they were added
        if (recentSearches.size > 20) {
            recentSearches.subList(20, recentSearches.size).clear()
        }
        context?.let { SavePrefs.setArrayPrefs(
            getString(R.string.recent_search_str),
            recentSearches,
            it
        ) }
    }

    private fun hideKeyboard() {
        val inputManager = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(
            searchView.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    private fun setupRecentlySearchesRecycler()
    {
        val layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        recycler.adapter = recentSearchesAdapter
        val itemDecor = DividerItemDecoration(this.context, HORIZONTAL)
        recycler.addItemDecoration(itemDecor)

        recycler.layoutManager = layoutManager
    }
    private fun performSearch(searchStr: String) {
        addToRecentSearches(searchStr)
        hideKeyboard()
        Handler().postDelayed({
            val bundle = Bundle()
            bundle.putString(SEARCH_TYPE, V4APICalls.SearchTypeFreeText)
            bundle.putString(SEARCH_TERM, searchStr)
            findNavController().navigate(
                R.id.action_storeDetailFragment_to_shops_parts_results_fragment,
                bundle,
                null,
                null
            )
        }, 100)
    }

    inner class RecentSearchesAdapter() : RecyclerView.Adapter<RecentSearchesAdapter.RecentSearchesViewHolder>()
    {
        override fun onBindViewHolder(holder: RecentSearchesViewHolder, position: Int) {
            val item = recentSearches[position]
            holder.mTitle.text = item
            holder.itemView.setOnClickListener {
                performSearch(item)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentSearchesViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.simple_search_item,
                parent,
                false
            )
            return RecentSearchesViewHolder(view)
        }

        override fun getItemCount(): Int = recentSearches.size

        inner class RecentSearchesViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
             val mTitle: TextView = mView.title

            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            performSearch(query.trim())
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {

        barcodeImage.visibility = if(newText.isNullOrBlank())View.VISIBLE else View.INVISIBLE
        return true
    }
}