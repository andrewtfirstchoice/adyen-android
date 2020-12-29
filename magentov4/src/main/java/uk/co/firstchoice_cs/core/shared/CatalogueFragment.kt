package uk.co.firstchoice_cs.core.shared

import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import kotlinx.android.synthetic.main.catalogue_fragment.*
import kotlinx.android.synthetic.main.empty_layout.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED_MOBILE
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED_WIFI
import uk.co.firstchoice_cs.Settings.MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT
import uk.co.firstchoice_cs.Settings.MOBILE_ONLY_CONNECTION_TEST
import uk.co.firstchoice_cs.Settings.MY_PERMISSIONS_REQUEST_READ_WRITE
import uk.co.firstchoice_cs.core.alerts.Alerts.showAlert
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer
import uk.co.firstchoice_cs.core.api.v4API.NCDocuments
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.document.Document
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.requestReadWriteStorage
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.DocumentManager
import uk.co.firstchoice_cs.core.managers.DocumentManager.Companion.instance
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.CataloguesViewModel
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import uk.co.firstchoice_cs.core.viewmodels.ManualDetailsViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.CatalogueFragmentRowBinding
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


open class CatalogueFragment : Fragment(R.layout.catalogue_fragment), KoinComponent,
    SearchView.OnQueryTextListener {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var documentManager: DocumentManager? = null
    private var mAnalyticsViewModel: AnalyticsViewModel? = null
    private var currentDownloadEntry: DocumentEntry? = null
    private var adapter: CataloguesAdapter? = null
    private val filteredList: ArrayList<DocumentEntry> = ArrayList()
    private lateinit var mFireBaseViewModel: FireBaseViewModel
    private lateinit var mCatalogueViewModel: CataloguesViewModel
    private var filterString: String = ""
    private var isSingleDownload = false
    private lateinit var searchView: SearchView

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_catalogue_fragment, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        val searchEditText = searchView.findViewById<View>(R.id.search_src_text) as EditText
        searchEditText.setTextColor(ContextCompat.getColor(ctx, R.color.white))
        searchEditText.setHintTextColor(ContextCompat.getColor(ctx, R.color.fcLightGrey))
        searchEditText.hint = "Filter catalogues"
        val searchButton: ImageView = searchView.findViewById(R.id.search_button)
        searchButton.setImageDrawable(ContextCompat.getDrawable(ctx, R.drawable.ic_search_white))
        val searchCloseButton: ImageView = searchView.findViewById(R.id.search_close_btn)
        searchCloseButton.setImageDrawable(
            ContextCompat.getDrawable(
                ctx,
                R.drawable.ic_close_white_24dp
            )
        )
        searchCloseButton.setOnClickListener {
            filterString = ""
            searchView.setQuery(filterString, false)
            searchView.onActionViewCollapsed()
        }

        super.onCreateOptionsMenu(menu, menuInflater)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress.visibility = View.GONE
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController(this@CatalogueFragment).navigateUp()
            }
        }
        setUpToolbar()

        mFireBaseViewModel = ViewModelProvider(act).get(FireBaseViewModel::class.java)
        mCatalogueViewModel = ViewModelProvider(act).get(CataloguesViewModel::class.java)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        documentManager = instance
        mAnalyticsViewModel = ViewModelProvider(act).get(AnalyticsViewModel::class.java)
        setHasOptionsMenu(true)
        val mLayoutManager: RecyclerView.LayoutManager = LinearLayoutManager(requireActivity())
        recycler.layoutManager = mLayoutManager
        recycler.itemAnimator = DefaultItemAnimator()
        recycler.addItemDecoration(DividerItemDecoration(act, DividerItemDecoration.VERTICAL))


        adapter = CataloguesAdapter()
        recycler.adapter = adapter


        if (mCatalogueViewModel.docList.size == 0)
            getData()
        else {
            filteredList.clear()
            filteredList.addAll(mCatalogueViewModel.docList)
            recycler.adapter?.notifyDataSetChanged()
        }

        initializeEmptyScreen()
    }

    private fun initializeEmptyScreen()
    {
        emptyView.visibility = View.GONE
        emptyView.emptyTitle.text=getString(R.string.no_catalogues)
        emptyView.emptyDescription.text="View catalogues to save them to your device"
        emptyView.emptyImage.setImageResource(R.drawable.icon_manual_download)
        emptyView.emptyButton.visibility = View.GONE
    }

    private fun buildDocumentList(catalogues: NCDocuments?) {
        mCatalogueViewModel.docList.clear()
        if (catalogues != null) {
            for (catalogue in catalogues.documents) {
                val doc = Document()
                doc.address = catalogue.files?.get(0)?.url?:""
                doc.setDisplayname(catalogue.files?.get(0)?.name?:"")
                val man = Manufacturer()
                val model = uk.co.firstchoice_cs.core.api.legacyAPI.models.Model()
                model.Name = catalogue.partDescription ?: ""
                model.ModelID = catalogue.partNum ?: ""
                man.Name = catalogue.manufacturer
                val docEntry = DocumentEntry()
                docEntry.category = "Catalogues"
                docEntry.document = doc
                docEntry.manufacturer = man
                docEntry.model = model
                doc.catalogueUrl = catalogue.images?.get(0)?.url.toString()

                mCatalogueViewModel.docList.add(docEntry)
            }
        }
        filteredList.addAll(mCatalogueViewModel.docList)
        recycler.adapter?.notifyDataSetChanged()
    }

    open fun gotoManualsDetails() {

    }

    private fun launchDocument(documentEntry: DocumentEntry?) {
        if (documentEntry == null)
            return

        if (documentEntry.isOnDevice) {
            if (activity != null) {
                logManualAnalyticEvent(documentEntry, "ManualViewed")
                val vm =
                    ViewModelProvider(activity as AppCompatActivity).get(ManualDetailsViewModel::class.java)
                vm.doc = documentEntry
                gotoManualsDetails()
            }
        }
    }


    private fun getData() {
        progress.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val catalogues = V4APICalls.ncDocuments("fcc", "", "", "", "", 0)

            withContext(Dispatchers.Main) {
                progress?.visibility = View.GONE
                if (catalogues != null) {
                    buildDocumentList(catalogues)
                }
            }
        }
    }

    private fun setUpToolbar() {
        act.setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { findNavController(this@CatalogueFragment).navigateUp() }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    }

    private fun getDownloadCount(): Int {
        var count = 0
        for (doc in mCatalogueViewModel.docList) {
            val downloadState = PRDownloader.getStatus(doc.downloadID)
            if (downloadState.name == "RUNNING")
                count++
        }
        return count
    }

    private fun launchDocument(documentEntry: DocumentEntry?, fromTap: Boolean) {
        if (documentEntry != null && documentEntry.isOnDevice) {
            if (!fromTap && isSingleDownload)
                launchDocument(documentEntry)
            else if (fromTap)
                launchDocument(documentEntry)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_WRITE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadManualIfAllowed(currentDownloadEntry)
            }
        }
    }

    private fun downloadManualIfAllowed(documentEntry: DocumentEntry?) {
        if (documentEntry == null)
            return

        instance?.updateDocument(documentEntry)

        if (!requestReadWriteStorage()) {
            return
        }

        if (INTERNET_CONNECTED_WIFI && !MOBILE_ONLY_CONNECTION_TEST) {
            startDownLoad(documentEntry)
        } else if (INTERNET_CONNECTED_MOBILE || MOBILE_ONLY_CONNECTION_TEST) {
            GlobalScope.launch(context = Dispatchers.IO) {
                val size = getSizeofDownLoad(documentEntry)
                if(!lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED))
                    return@launch
                withContext(Dispatchers.Main) {
                    if (toolbar != null && size > MOBILE_ONLY_CONNECTION_FILE_SIZE_LIMIT || MOBILE_ONLY_CONNECTION_TEST) {
                        val downloadMessage = String.format(
                            getString(R.string.mobile_allowance_warning),
                            Helpers.generateLengthString(size)
                        )

                        this@CatalogueFragment.context?.let {
                            AlertDialog.Builder(it)
                                .setTitle("Mobile Data Warning")
                                .setCancelable(false)
                                .setMessage(downloadMessage)
                                .setPositiveButton(android.R.string.yes) { _: DialogInterface?, _: Int ->
                                    startDownLoad(
                                        documentEntry
                                    )
                                }
                                .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(
                context,
                "Please connect to the internet to download catalogue",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setDownloadState(documentEntry: DocumentEntry?, downloadState: Boolean) {
        if (documentEntry != null) {
            documentEntry.downloading = downloadState
            if (!downloadState)
                documentEntry.downloadID = 0
            instance?.updateDocument(documentEntry)
        }
    }

    private fun setDownLoadID(documentEntry: DocumentEntry?, downloadId: Int) {
        if (documentEntry != null) {
            documentEntry.downloading = true
            documentEntry.downloadID = downloadId
            instance?.updateDocument(documentEntry)
        }
    }

    private fun startDownLoad(documentEntry: DocumentEntry?) {
        if (documentEntry == null)
            return

        val count = getDownloadCount()
        isSingleDownload = count <= 0

        logManualAnalyticEvent(documentEntry, "ManualDownloaded")

        adapter?.notifyItemChanged(filteredList.indexOf(documentEntry))
        setDownLoadID(documentEntry,
            PRDownloader.download(
                documentEntry.url.replace(" ", "%20"),
                App.docDir,
                documentEntry.file.name
            ).build()
                .setOnProgressListener {
                    documentEntry.bytesDownloaded = it.currentBytes
                    documentEntry.totalBytes = it.totalBytes

                    if (documentEntry.bytesDownloaded - documentEntry.lastDownloadedBytes > 100) {
                        val diff = documentEntry.bytesDownloaded - documentEntry.lastDownloadedBytes
                        if (diff > 100) {
                            documentEntry.lastDownloadedBytes = documentEntry.bytesDownloaded
                            adapter?.notifyItemChanged(filteredList.indexOf(documentEntry))
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
                        setDownloadState(documentEntry, false)
                        adapter?.onDocumentError(documentEntry)
                    }

                    override fun onDownloadComplete() {
                        setDownloadState(documentEntry, false)
                        adapter?.onDocumentFinished(documentEntry)
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

    private fun logManualAnalyticEvent(doc: DocumentEntry?, type: String) {
        val bundle = Bundle()
        bundle.putString("manufacturer", doc?.manufacturer?.Name)
        bundle.putString("model", doc?.model?.Name)
        mAnalyticsViewModel?.mFirebaseAnalytics?.logEvent(type, bundle)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        filterString = newText
        filterResults(newText)
        return true
    }


    private fun buildHeader() {
        var currentLetter = "A"
        var firstRow = true
        for (doc in filteredList) {
            doc.isHeader = false
            if (doc.model.Name.isNotEmpty()) {
                val letter =
                    doc.model.Name.trim { it <= ' ' }.toUpperCase(Locale.ENGLISH).substring(0, 1)
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

    private fun showHideEmptyView(stateIsEmpty: Boolean) {
        if (stateIsEmpty) {
            recycler.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
            if (filterString.isBlank())
                initEmptyNoFilter()
            else
                initEmptyWithFilter()
        } else {
            recycler.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        }
    }

    private fun initEmpty(description: String, title: String) {
        emptyView?.emptyTitle?.text = title
        emptyView?.emptyDescription?.text = description
    }

    private fun filterResults(searchWords: String) {
        val searchTerms = searchWords.split("\\s+").toTypedArray()
        filteredList.clear()
        if (searchTerms.isEmpty()) {
            clearFilter()
            filteredList.addAll(mCatalogueViewModel.docList)
            return
        }
        for (searchWord in searchTerms) {
            for (entry in mCatalogueViewModel.docList) {
                if (entry.model.Name.contains(searchWord, ignoreCase = true)
                    || entry.manufacturer.Name.contains(searchWord, ignoreCase = true)
                ) {
                    if (!filteredList.contains(entry)) filteredList.add(entry)
                }
            }
        }

        val sorted = filteredList.sortedWith(compareBy { it.manufacturer.Name })
        filteredList.clear()
        filteredList.addAll(sorted)
        buildHeader()
        showHideEmptyView(filteredList.isEmpty())
    }

    private fun clearFilter() {
        searchView.setQuery("", false)
    }


    private fun initEmptyNoFilter() {
        initEmpty(
            "You have no downloaded catalogues",
            "View catalogues to save them to your device"
        )
    }

    private fun initEmptyWithFilter() {
        initEmpty("Try clearing your filters or broadening you search", "No Catalogues Found")
    }


    private inner class CataloguesAdapter : RecyclerView.Adapter<CataloguesAdapter.ViewHolder>() {

        override fun getItemId(i: Int): Long {
            return i.toLong()
        }

        override fun getItemCount(): Int {
            return filteredList.size
        }

        fun onDocumentFinished(documentEntry: DocumentEntry) {
            setDownloadState(documentEntry, false)
            adapter?.notifyItemChanged(filteredList.indexOf(documentEntry))
            launchDocument(documentEntry, false)
        }

        fun onDocumentError(documentEntry: DocumentEntry) {
            setDownloadState(documentEntry, false)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.catalogue_fragment_row, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val documentEntry = filteredList[position]
            //this is here to clear up previous download flags that the current PRDownloader no longer recognises
            if (documentEntry.downloadID != 0) {
                val downloadStatus = PRDownloader.getStatus(documentEntry.downloadID)
                if (downloadStatus.name == "UNKNOWN") {
                    setDownloadState(documentEntry, false)
                }
            }

            holder.itemView.setOnClickListener {
                if (documentEntry.downloading) return@setOnClickListener
                if (!documentEntry.isOnDevice) {
                    if (!INTERNET_CONNECTED) {
                        showAlert(
                            "Unable to download",
                            "Your internet connection appears to be offline",
                            null
                        )
                        return@setOnClickListener
                    }
                    currentDownloadEntry = documentEntry
                    if (requestReadWriteStorage()) {
                        downloadManualIfAllowed(documentEntry)
                    }
                } else {
                    launchDocument(documentEntry, true)
                }
            }
            holder.binding.header.header.visibility =
                if (documentEntry.isHeader) View.VISIBLE else View.GONE
            val header = documentEntry.headerValue
            if (header != null)
                holder.binding.header.headerText.text = header.toUpperCase(Locale.ENGLISH)

            Helpers.renderImage( holder.binding.imgPdfThumbnail,documentEntry.document.catalogueUrl)

            holder.binding.downloadIndicator.visibility =
                if (documentEntry.isOnDevice && !documentEntry.downloading) View.INVISIBLE else View.VISIBLE
            holder.binding.txtManufacturer.text = documentEntry.manufacturer.Name
            holder.binding.txtModel.text = documentEntry.model.Name
            holder.binding.txtCategory.text = documentEntry.category
            holder.binding.nextArrow.visibility =
                if (documentEntry.selected) View.VISIBLE else View.GONE
            //handle download status
            if (documentEntry.downloading) {
                holder.binding.txtProgress.visibility = View.VISIBLE
                holder.binding.pbDownload.visibility = View.VISIBLE
                if (documentEntry.totalBytes.toInt() > 0) {
                    val percentProgress =
                        documentEntry.bytesDownloaded * 100 / documentEntry.totalBytes
                    documentEntry.bytesDownloaded /= 1000000
                    val downloaded = documentEntry.bytesDownloaded
                    documentEntry.totalBytes /= 1000000
                    val total = documentEntry.totalBytes
                    if (total > 0) {
                        holder.total = total
                        holder.downloaded = downloaded
                        val progressText = String.format(
                            Locale.ENGLISH,
                            "%d.0MB / %d.0MB",
                            holder.downloaded, holder.total
                        )
                        holder.binding.txtProgress.text = progressText
                        holder.binding.pbDownload.progress = percentProgress.toInt()
                    }
                }
            } else {
                holder.binding.txtProgress.visibility = View.GONE
                holder.binding.pbDownload.visibility = View.GONE
            }

            Helpers.renderLastViewed(documentEntry.lastSeen, holder.binding.txtLastViewed)
        }


        private inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding: CatalogueFragmentRowBinding = CatalogueFragmentRowBinding.bind(view)
            var downloaded: Long = 0
            var total: Long = 0
        }
    }
}
