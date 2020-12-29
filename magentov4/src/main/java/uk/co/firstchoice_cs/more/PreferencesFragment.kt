package uk.co.firstchoice_cs.more

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.preferences_fragment.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.SavePrefs
import uk.co.firstchoice_cs.SavePrefs.clearUserAndLogout
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.PrefsSectionedListDetailsBinding
import uk.co.firstchoice_cs.firstchoice.databinding.SectionedListItemHeaderBinding
import java.util.*

class PreferencesFragment : Fragment(R.layout.preferences_fragment) , KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private var mListener: OnFragmentInteractionListener? = null
    private var sectionAdapter = SectionedRecyclerViewAdapter()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mListener?.restoreFabState()
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@PreferencesFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        recycler.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })
        recycler.layoutManager = LinearLayoutManager(this@PreferencesFragment.context)
        sectionAdapter = SectionedRecyclerViewAdapter()
        recycler.adapter = sectionAdapter
        recycler.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        renderLists(sectionAdapter)
        setUpToolbar(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_preferences, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    private fun clearLists() {
        sectionAdapter.removeAllSections()
    }

    private fun renderLists(sectionAdapter: SectionedRecyclerViewAdapter) {
        clearLists()
        val chatItems = ArrayList<PrefsItem>()
        chatItems.add(PrefsItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.profileicon, "Edit Details"))
        sectionAdapter.addSection(PrefsSectionItem("CHAT", chatItems))
        val loginItems = ArrayList<PrefsItem>()
        loginItems.add(PrefsItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_clear_all_black_24dp, "Clear Cache"))
        loginItems.add(PrefsItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.ic_delete_forever_grey600_24dp, "Clear Downloads"))
        loginItems.add(PrefsItem(ContextCompat.getColor(ctx, R.color.lp_dark_gray_1), R.drawable.sign_out_icon, "Force Logout"))
        sectionAdapter.addSection(PrefsSectionItem("SUPPORT", loginItems))
        sectionAdapter.notifyDataSetChanged()
    }

    private fun setUpToolbar(view: View) {
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        toolbar.title = "Preferences"
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.setSubtitleTextColor(Color.WHITE)
        toolbar.setNavigationOnClickListener { NavHostFragment.findNavController(this@PreferencesFragment).navigateUp() }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun clearData() {
        requestPermission()
    }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    delete()
                } else {
                    Toast.makeText(ctx, "This is unavailable if storage permission is disallowed", Toast.LENGTH_SHORT).show()
                }
            }


    private fun requestPermission() {
        when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                delete()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                val alert = AlertDialog.Builder(ctx)
                alert.setTitle("Permissions Required")
                alert.setMessage("We need to be able to write to storage so that manuals and photos can be stored.  We only store what the user asks for")
                alert.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    dialog.dismiss()
                }
                alert.setCancelable(false)
                alert.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                alert.show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun delete() {
        warnUserOfDelete()
    }

    private fun warnUserOfDelete() {
        AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.clear_downloads))
                .setMessage(getString(R.string.clear_downloads_warning))
                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                    DocumentManager.deleteFirstChoiceFolder()
                    Toast.makeText(requireActivity(), "Downloaded files deleted", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
    }

    private fun navigateToContactDetails() {
        val bundle = bundleOf("update" to false)
        NavHostFragment.findNavController(this@PreferencesFragment).navigate(R.id.action_preferences_fragment_to_contactDetailsFragmentPrefs, bundle)
    }


    interface OnFragmentInteractionListener {
        fun onScrollUp()
        fun onScrollDown()
        fun restoreFabState()
    }

    class PrefsItem internal constructor(var tint: Int, var drawable: Int, var text: String)

    internal inner class PrefsSectionItem(private val title: String, private val list: List<PrefsItem>) : Section(SectionParameters.builder()
            .itemResourceId(R.layout.prefs_sectioned_list_details)
            .headerResourceId(R.layout.sectioned_list_item_header)
            .build()) {
        override fun getContentItemsTotal(): Int {
            return list.size
        }

        override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
            return ItemViewHolder(view)
        }

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemHolder = holder as ItemViewHolder
            itemHolder.binding.nav.setOnClickListener(null)
            itemHolder.itemView.setOnClickListener(null)
            itemHolder.binding.text.text = list[position].text
            itemHolder.binding.icon.setImageResource(list[position].drawable)
            val tint = list[position].tint
            val listener = View.OnClickListener {
                navigateToContactDetails()
            }

            if (tint != NO_TINT) itemHolder.binding.icon.setColorFilter(tint)
            if (title.equals("CHAT", ignoreCase = true)) {
                itemHolder.binding.toggle.visibility = View.GONE
                itemHolder.binding.button.visibility = View.GONE
                itemHolder.binding.nav.visibility = View.VISIBLE
                itemHolder.binding.nav.setOnClickListener(listener)
                itemHolder.itemView.setOnClickListener(listener)
            } else if (title.equals("SUPPORT", ignoreCase = true)) {
                if (position == 0) {
                    itemHolder.binding.toggle.visibility = View.GONE
                    itemHolder.binding.button.visibility = View.VISIBLE
                    itemHolder.binding.button.text = getString(R.string.clearWebCache)
                    itemHolder.binding.nav.visibility = View.GONE
                    itemHolder.binding.button.setOnClickListener { fixAndClear() }
                }
                if (position == 1) {
                    itemHolder.binding.toggle.visibility = View.GONE
                    itemHolder.binding.button.visibility = View.VISIBLE
                    itemHolder.binding.button.text = getString(R.string.clearDownloads)
                    itemHolder.binding.nav.visibility = View.GONE
                    itemHolder.binding.button.setOnClickListener { clearData() }
                }
                if (position == 2) {
                    itemHolder.binding.toggle.visibility = View.GONE
                    itemHolder.binding.button.visibility = View.VISIBLE
                    itemHolder.binding.button.text = getString(R.string.sync)
                    itemHolder.binding.nav.visibility = View.GONE
                    itemHolder.binding.button.setOnClickListener { sync() }
                }
            }
        }

        private fun sync() {
            logout()
        }

        private fun logout() {
            SavePrefs.setLoggedIn(Settings.LoginState.NONE)
        }


        private fun fixAndClear() {
            clearUserAndLogout()
            Toast.makeText(requireActivity(), "Website data cleared", Toast.LENGTH_SHORT).show()
        }

        override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
            return HeaderViewHolder(view)
        }

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
            val headerHolder = holder as HeaderViewHolder
            headerHolder.binding.moreHeaderText.text = title
        }

        private inner class HeaderViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            val binding: SectionedListItemHeaderBinding = SectionedListItemHeaderBinding.bind(view)
        }

        internal inner class ItemViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            val binding: PrefsSectionedListDetailsBinding = PrefsSectionedListDetailsBinding.bind(view)
        }
    }

    companion object {
        private const val NO_TINT = -1
    }
}
