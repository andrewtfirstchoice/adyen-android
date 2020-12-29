package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.android.synthetic.main.manuals_search_result_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.Settings.MOBILE_ONLY_CONNECTION_TEST
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer
import uk.co.firstchoice_cs.core.document.Document
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.ThumbNailManager.createPdfThumbnail
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.core.viewmodels.SearchViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.CatalogueFragmentRowBinding
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList

open class ManualsSearchResultsFragment : Fragment(R.layout.manuals_search_result_fragment),
        SearchView.OnQueryTextListener,
        KoinComponent {

    private lateinit var mViewModel: SearchViewModel
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var mAnalyticsViewModel: AnalyticsViewModel
    private lateinit var adapter: ManualsFragmentAdapter
    private var manufacturerID: String? = null
    private var prodCode: String? = null
    private var manufacturerName: String? = null
    private var type: String? = null
    private var docList: ArrayList<DocumentEntry> = ArrayList()
    private val filteredList: MutableList<DocumentEntry> = ArrayList()
    private var currentDownloadEntry: DocumentEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mViewModel = ViewModelProvider(act).get(SearchViewModel::class.java)
        val args = arguments
        manufacturerID = args?.getString(ARG_1)
        prodCode = args?.getString(ARG_2)
        type = args?.getString(ARG_3)
        manufacturerName = args?.getString(ARG_4)
        init()

    }


    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_manuals_search_fragment, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        val searchEditText = searchView.findViewById<View>(R.id.search_src_text) as EditText
        searchEditText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
        searchEditText.setHintTextColor(ContextCompat.getColor(ctx, R.color.fcLightGrey))
        searchEditText.hint = "Filter manuals"
        val searchButton: ImageView = searchView.findViewById(R.id.search_button)
        searchButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_search_white))

        val searchCloseButton: ImageView = searchView.findViewById(R.id.search_close_btn)
        searchCloseButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_close_white_24dp))

        searchCloseButton.setOnClickListener {
            searchView.setQuery("", false)
            searchView.onActionViewCollapsed()
            filteredList.clear()
            filteredList.addAll(docList)
            adapter.notifyDataSetChanged()
        }
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filteredList.clear()
        if (!TextUtils.isEmpty(newText)) {
            for (documentEntry in docList) {
                if (documentEntry.model.Name.contains(newText, true)) filteredList.add(documentEntry)
            }
        } else {
            filteredList.addAll(docList)
        }
        dataChanged()
        return true
    }

    private fun dataChanged() {
        showSubtitle()
        val sorted  = filteredList.sortedWith(compareBy(
                { it.manufacturer.Name },
                { it.model.Name }

        ))
        filteredList.clear()
        filteredList.addAll(sorted)
        buildHeader()
        recycler.adapter?.notifyDataSetChanged()
    }

    private fun buildHeader() {
        var currentLetter = "A"
        var firstRow = true
        for (doc in filteredList) {
            doc.isHeader = false
            if (!doc.model.Name.isNullOrEmpty()) {
                val letter = doc.model.Name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
                val sComp = letter.compareTo(currentLetter.toUpperCase(Locale.ENGLISH))
                if (sComp > 0 || firstRow) {
                    doc.isHeader = true
                    currentLetter = letter
                    doc.headerValue = letter
                    firstRow = false
                }
            }
        }
    }

    //region overloadable functions
    private fun initViewModels() {
        mAnalyticsViewModel = ViewModelProvider(act).get(AnalyticsViewModel::class.java)
    }


    private fun showHideEmptyView(stateIsEmpty: Boolean) {
        if (recycler != null) {
            if (stateIsEmpty) {
                recycler.visibility = GONE
                emptyView?.visibility = VISIBLE
            } else {
                recycler.visibility = VISIBLE
                emptyView?.visibility = GONE
            }
        }
    }

    private fun init() {
        initViewModels()

        initRecycler()

        mListener?.restoreFabState()

        setUpToolbar()
        if(type==null)
            type = ""
        toolbar.title = "$manufacturerName ${type?.trim()}"

        toolbar.setNavigationOnClickListener {
            NavHostFragment.findNavController(this@ManualsSearchResultsFragment).navigateUp()
        }

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@ManualsSearchResultsFragment).navigateUp()
            }
        }
        act.onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        initializeEmptyScreen()

        buildDocumentList()
    }

    private fun initializeEmptyScreen()
    {
        emptyView.visibility = GONE
        initEmpty("","View manuals to save them to your device",R.drawable.icon_manual_download,"You have no downloaded manuals")
    }

    private fun initEmpty(emptyButtonStr: String, emptyDescriptionStr: String, emptyImageResource: Int, emptyTitleStr: String) {
        emptyView.emptyTitle.text=emptyTitleStr
        emptyView.emptyDescription.text=emptyDescriptionStr
        emptyView.emptyButton.text=emptyButtonStr
        emptyView.emptyImage.setImageResource(emptyImageResource)
        if(emptyButtonStr.isBlank())
            emptyView.emptyButton.visibility = GONE
        else
            emptyView.emptyButton.visibility = VISIBLE
    }


    private fun buildDocumentList() {
        val documents = mViewModel.documents?.get(0)?.files
        val root = mViewModel.documents?.get(0)
        docList.clear()
        filteredList.clear()
        if (documents != null) {
            for (d in documents) {
                val doc = Document()
                val man = Manufacturer()
                man.Name = root?.manufacturer?:""
                val model = uk.co.firstchoice_cs.core.api.legacyAPI.models.Model()
                model.Name = d.name

                val docEntry = DocumentEntry()
                doc.address = d.url
                doc.setDisplayname(d.description)
                docEntry.category = type
                docEntry.documentType = d.docType
                docEntry.document = doc
                docEntry.manufacturer = man
                docEntry.model = model
                docList.add(docEntry)
            }
        }
        filteredList.addAll(docList)
        showHideEmptyView(filteredList.isEmpty())
        dataChanged()
    }


    fun downloadDocument(documentEntry: DocumentEntry) {
        if(AppStatus.INTERNET_CONNECTED) {
            GlobalScope.launch(Dispatchers.IO) {
                if (!documentEntry.downloading) {
                    withContext(Dispatchers.Main) {
                        processDownload(documentEntry)
                    }
                }
            }
        }
        else
        {
            Alerts.showNoInternetToast()
        }
    }

    private fun processDownload(doc: DocumentEntry) {
        if (doc.downloading) return

        if(doc.isOnDevice)
        {
            launchDocument(doc)
        }
        else {
               beginDownload(doc)
            }
    }

    private fun beginDownload(doc: DocumentEntry)
    {
        if (!AppStatus.INTERNET_CONNECTED) {
            Alerts.showAlert("Unable to download", "Your internet connection appears to be offline", null)
            return
        }
        currentDownloadEntry = doc
        if (Helpers.requestReadWriteStorage()) {
            downloadManualIfAllowed(doc)
        }
    }

    private fun showSubtitle()
    {
        toolbar.subtitle = "Showing " + filteredList.size + " results"
    }


    private fun initRecycler() {
        adapter = ManualsFragmentAdapter()
        recycler.adapter = adapter
        recycler.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })
        recycler.addItemDecoration(DividerItemDecoration(act, DividerItemDecoration.VERTICAL))
        val linearLayoutManager = LinearLayoutManager(this.context)
        recycler.layoutManager = linearLayoutManager
    }

    open fun launchDocument(doc: DocumentEntry) {

        val downloadCount = getDownloadCount()
        if(downloadCount==0) {
            //toolbar check helps prevent crashes
            if (doc.isOnDevice && toolbar != null) {
                val b = Bundle()
                b.putString("manufacturer", doc.manufacturer.Name)
                b.putString("model", doc.model.Name)
                mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("ManualViewed", b)
                val vm = ViewModelProvider(act).get(ManualDetailsViewModel::class.java)
                vm.doc = doc
                showManualsDetails()
            }
        }
    }

    open fun showManualsDetails()
    {

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    private fun logManualAnalyticEvent(doc: DocumentEntry?, type: String) {
        val bundle = Bundle()
        bundle.putString("manufacturer", doc?.manufacturer?.Name)
        bundle.putString("model", doc?.model?.Name)
        mAnalyticsViewModel.mFirebaseAnalytics?.logEvent(type, bundle)
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == Settings.MY_PERMISSIONS_REQUEST_READ_WRITE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadManualIfAllowed(currentDownloadEntry)
            }
        }
    }

    private fun downloadManualIfAllowed(documentEntry: DocumentEntry?) {
        if (documentEntry == null)
            return

        DocumentManager.instance?.updateDocument(documentEntry)

        if (!Helpers.requestReadWriteStorage()) {
            return
        }

        if (AppStatus.INTERNET_CONNECTED_WIFI && !MOBILE_ONLY_CONNECTION_TEST) {
            startDownLoad(documentEntry)
        } else if (AppStatus.INTERNET_CONNECTED_MOBILE || MOBILE_ONLY_CONNECTION_TEST) {
            GlobalScope.launch(context = Dispatchers.IO) {
                val size = getSizeofDownLoad(documentEntry)
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                if (toolbar != null) {
                    withContext(Dispatchers.Main) {
                        if (size > Settings.MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT || MOBILE_ONLY_CONNECTION_TEST) {
                            val downloadMessage = String.format(getString(R.string.mobile_allowance_warning), Helpers.generateLengthString(size))

                            this@ManualsSearchResultsFragment.context?.let {
                                AlertDialog.Builder(it)
                                        .setTitle("Mobile Data Warning")
                                        .setCancelable(false)
                                        .setMessage(downloadMessage)
                                        .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int -> startDownLoad(documentEntry); }
                                        .setNegativeButton(android.R.string.cancel) { dialog, _ ->  dialog.dismiss() }
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show()
                            }
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Please connect to the internet to download catalogue", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDownloadCount():Int
    {
        var count = 0
        for (doc in docList)
        {
            val downloadState = PRDownloader.getStatus(doc.downloadID)
            if(downloadState.name=="RUNNING")
                count++
        }
        return count
    }

    private fun setDownLoadID(documentEntry: DocumentEntry?, downloadId:Int)
    {
        if(documentEntry!=null) {
            documentEntry.downloading = true
            documentEntry.downloadID = downloadId
            DocumentManager.instance?.updateDocument(documentEntry)
        }
    }

    fun setDownloadState(documentEntry: DocumentEntry?, downloadState:Boolean)
    {
        if(documentEntry !=null) {
            documentEntry.downloading = downloadState
            if(!downloadState)
                documentEntry.downloadID = 0
            DocumentManager.instance?.updateDocument(documentEntry)
        }
    }

    private fun startDownLoad(documentEntry: DocumentEntry?) {
        if (documentEntry == null || documentEntry.downloading)
            return

        logManualAnalyticEvent(documentEntry, "ManualDownloaded")
        adapter.notifyItemChanged(docList.indexOf(documentEntry))
        setDownLoadID(documentEntry, PRDownloader.download(documentEntry.url.replace(" ", "%20"), App.docDir, documentEntry.file.name).build()
                .setOnProgressListener {
                    documentEntry.bytesDownloaded = it.currentBytes
                    documentEntry.totalBytes = it.totalBytes

                    if (documentEntry.bytesDownloaded - documentEntry.lastDownloadedBytes > 100) {
                        val diff = documentEntry.bytesDownloaded - documentEntry.lastDownloadedBytes
                        if(diff > 100) {
                            documentEntry.lastDownloadedBytes = documentEntry.bytesDownloaded
                            adapter.notifyItemChanged(docList.indexOf(documentEntry))
                        }
                    }
                }
                .setOnStartOrResumeListener {
                    documentEntry.lastDownloadedBytes = 0
                    documentEntry.bytesDownloaded = 0
                    documentEntry.totalBytes = 0
                }
                .start(object : OnDownloadListener {
                    override fun onError(error: Error?) {
                        adapter.onDocumentError(documentEntry)
                    }

                    override fun onDownloadComplete() {
                        setDownloadState(documentEntry,true)
                        adapter.onDocumentFinished(documentEntry)
                    }
                })
        )
    }


    private fun getSizeofDownLoad(doc: DocumentEntry): Int {
        return try {
            val myUrl = URL(doc.url)
            val urlConnection = myUrl.openConnection()
            urlConnection.connect()
            urlConnection.contentLength
        } catch (ignored: Exception) {
            0
        }
    }

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        toolbar.title = ""
        toolbar.subtitle = ""
        toolbar.setTitleTextColor(Color.WHITE)
        toolbar.setSubtitleTextColor(Color.WHITE)
        activity?.setSupportActionBar(toolbar)
    }

    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun restoreFabState()
        fun onScrollUp()
        fun hideNavBar()
        fun showNavBar()
        fun setSlider(pos: Int)
    }

    private inner class ManualsFragmentAdapter : RecyclerView.Adapter<ManualsFragmentAdapter.ViewHolder>() {
        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemCount(): Int {
            return filteredList.size
        }

        fun onAddNewModelSeq(documentEntry: DocumentEntry) {
            setDownloadState(documentEntry,true)
            val copy =  DocumentEntry.copy(documentEntry)
            DocumentManager.instance?.updateDocument(copy)
            adapter.notifyItemChanged(docList.indexOf(copy))
            launchDocument(copy)
        }

        fun onDocumentFinished(documentEntry: DocumentEntry) {
            setDownloadState(documentEntry,true)
            DocumentManager.instance?.updateDocument(documentEntry)
            adapter.notifyItemChanged(docList.indexOf(documentEntry))
            downloadThumb(documentEntry)
            launchDocument(documentEntry)
        }

        private fun downloadThumb(docEntry: DocumentEntry) {
            val fileUrl = App.docDir + "/" + docEntry.file.name
            val file = File(fileUrl)
            createPdfThumbnail(file)
        }

        fun onDocumentError(documentEntry: DocumentEntry) {
            setDownloadState(documentEntry,false)
            adapter.notifyItemChanged(docList.indexOf(documentEntry))
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.catalogue_fragment_row, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredList[position]

            //this is here to clear up previous download flags that the current PRDownloader no longer recognises
            if(item.downloadID !=0)
            {
                val downloadStatus = PRDownloader.getStatus(item.downloadID)
                if(downloadStatus.name=="UNKNOWN")
                {
                    setDownloadState(item,false)
                }
            }

            holder.itemView.setOnClickListener {
               downloadDocument(filteredList[position])
            }
            holder.binding.header.header.visibility = if (item.isHeader) VISIBLE else GONE
            val header = item.headerValue
            if (header != null)
                holder.binding.header.headerText.text = header.toUpperCase(Locale.ENGLISH)

            holder.binding.imgPdfThumbnail.setImageResource(R.drawable.document_thumb)
            holder.binding.downloadIndicator.visibility = GONE
            holder.binding.txtManufacturer.text = manufacturerName
            holder.binding.txtModel.text = item.model.Name
            holder.binding.txtCategory.text = item.category
            holder.binding.txtLastViewed.text = getString(R.string.parts_diagram)
            holder.binding.nextArrow.visibility = if (item.selected) VISIBLE else GONE
            //handle download status
            if (item.downloading) {
                holder.binding.txtProgress.visibility = VISIBLE
                holder.binding.pbDownload.visibility = VISIBLE
                if (item.totalBytes.toInt() > 0) {
                    val percentProgress = item.bytesDownloaded * 100 / item.totalBytes
                    item.bytesDownloaded /= 1000000
                    val downloaded = item.bytesDownloaded
                    item.totalBytes /= 1000000
                    val total = item.totalBytes
                    if (total > 0) {
                        holder.total = total
                        holder.downloaded = downloaded
                        val progressText = String.format(Locale.ENGLISH, "%d.0MB / %d.0MB", holder.downloaded, holder.total)
                        holder.binding.txtProgress.text = progressText
                        holder.binding.pbDownload.progress = percentProgress.toInt()
                    }
                }
            } else {
                holder.binding.txtProgress.visibility = GONE
                holder.binding.pbDownload.visibility = GONE
            }
        }

        inner class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {
            val binding: CatalogueFragmentRowBinding = CatalogueFragmentRowBinding.bind(view)
            var downloaded: Long = 0
            var total: Long = 0
        }
    }

    companion object {
        @JvmStatic
        val ARG_1 = "ManufacturerID"
        @JvmStatic
        val ARG_2 = "ProdCode"
        @JvmStatic
        val ARG_3 = "Type"
        @JvmStatic
        val ARG_4 = "Manufacturer"
    }
}