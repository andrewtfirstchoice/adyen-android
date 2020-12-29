package uk.co.firstchoice_cs.more

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.whats_new_fragment.*
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.WhatsNewListItemBinding
import java.util.*

class WhatsNewFragment : Fragment() {
    private var mAdapter: WhatsNewAdapter? = null
    private val moreList: MutableList<WhatsNewDataItem> = ArrayList()
    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { NavHostFragment.findNavController(this@WhatsNewFragment).navigateUp() }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_parts_id, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        populateData()
    }

    class WhatsNewDataItem {
        var numVersion = 0.0
        var version = ""
        var name = ""
        var key = ""
        var value = ""
        var isKey = false

       fun setCurrentVersion(version: String) {
            this.version = version
            convertVersion()
        }


        private fun convertVersion() {
            //input should be v2.2.0
            if (version.isEmpty()) return
            var cleanedVersion = version.replace("v", "") //v2.2.0 to 2.2.0
            cleanedVersion = cleanedVersion.replace(".", "") //2.2.0 to 220
            cleanedVersion = StringBuilder(cleanedVersion).insert(1, ".").toString()
            numVersion = cleanedVersion.toDouble()
            if (numVersion > latestVersionNumber) latestVersionNumber = numVersion
        }
    }

    private fun populateData() {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("android/whatsNew")
        moreList.clear()
        myRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (child in dataSnapshot.children) {
                    val moreHeader = WhatsNewDataItem()
                    moreHeader.name = child.key.toString()
                    moreHeader.isKey = true
                    for (children in child.children) {
                        when {
                            children.key.equals("numVersion", ignoreCase = true) -> moreHeader.numVersion = children.value as Double
                            children.key.equals("version", ignoreCase = true) -> moreHeader.setCurrentVersion(children.value as String)
                            else -> {
                                val moreChild = WhatsNewDataItem()
                                moreChild.name = moreHeader.name
                                moreChild.isKey = false
                                moreChild.key = children.key.toString()
                                moreChild.value = children.value as String
                                moreList.add(0, moreChild)
                            }
                        }
                    }
                    moreList.add(0, moreHeader) //add key
                }
                mAdapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.whats_new_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@WhatsNewFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        recycler.setHasFixedSize(true)
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this@WhatsNewFragment.context)
        recycler.layoutManager = mLayoutManager
        mAdapter = WhatsNewAdapter()
        recycler.adapter = mAdapter
        setUpToolbar()
    }

    private inner class WhatsNewAdapter internal constructor() : RecyclerView.Adapter<WhatsNewAdapter.WhatsNewViewHolder?>() {
        private val mInflater: LayoutInflater = LayoutInflater.from(this@WhatsNewFragment.context)
        private val blue: Int = ContextCompat.getColor(requireContext(),R.color.fcBlue)

        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): WhatsNewViewHolder {
            val itemView = mInflater.inflate(R.layout.whats_new_list_item, viewGroup, false)
            return WhatsNewViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: WhatsNewViewHolder, position: Int) {
            val first = moreList[0]
            val current = moreList[position]
            val isFirst = first.name == current.name
            holder.binding.description.text = current.value //Fast scanner added for improved performance
            holder.binding.title.text = current.key //Improved Scanner
            holder.binding.version.text = current.version //v2.3.9
            holder.binding.version.visibility = if (!current.isKey) View.GONE else View.VISIBLE
            holder.binding.card.visibility = if (current.isKey) View.GONE else View.VISIBLE
            holder.binding.card.setCardBackgroundColor(if (isFirst) blue else Color.WHITE)
            holder.binding.description.setTextColor(if (isFirst) Color.WHITE else Color.BLACK)
            holder.binding.title.setTextColor(if (isFirst) Color.WHITE else Color.BLACK)
            holder.binding.version.setTextColor(if (isFirst) Color.BLACK else Color.BLACK)
            //give padding on first
            holder.binding.paddingView.visibility = if (position == 0) View.VISIBLE else View.GONE
        }

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemCount(): Int {
            return moreList.size
        }

        internal inner class WhatsNewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val binding: WhatsNewListItemBinding = WhatsNewListItemBinding.bind(itemView)
        }
    }

    companion object {
        private var latestVersionNumber = 0.0
    }
}