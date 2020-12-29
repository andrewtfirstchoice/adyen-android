package uk.co.firstchoice_cs.store.fragments

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.gson.Gson
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter
import kotlinx.android.synthetic.main.scanner_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.alerts.Alerts
import uk.co.firstchoice_cs.core.api.v4API.Product
import uk.co.firstchoice_cs.core.api.v4API.V4APICalls
import uk.co.firstchoice_cs.core.barcode_detection.BarCodeRecognitionInterface
import uk.co.firstchoice_cs.core.barcode_detection.BarCodeRecognitionProcessor
import uk.co.firstchoice_cs.core.camera.CameraSource
import uk.co.firstchoice_cs.core.database.DBViewModel
import uk.co.firstchoice_cs.core.database.entities.PreviousScanList
import uk.co.firstchoice_cs.core.helpers.Helpers.navigateToAddPartFragmentWithSuperC
import uk.co.firstchoice_cs.core.helpers.SwipeCallback
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.store.scanner.AddBarcodeDialog
import uk.co.firstchoice_cs.store.scanner.AddBarcodeDialogCallback
import uk.co.firstchoice_cs.store.scanner.ItemViewHolder
import uk.co.firstchoice_cs.store.scanner.ScanSection
import java.util.*

class QrCode internal constructor(var value: String?) {
    var meta: Product? = null
}

class ScannerFragment : Fragment(R.layout.scanner_fragment),
        BarCodeRecognitionInterface,
        AddBarcodeDialogCallback,
        ScanSection.ScanSectionItemInterface, ScanSection.ScanSectionHeaderItemInterface, KoinComponent {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private val searchCache = ArrayList<String?>()
    private var currentHighLightedPosition = -1
    private var previousHighLightedPosition = -1
    private var previousScanList: MutableList<PreviousScanList> = ArrayList()
    private val currentScanList: MutableList<PreviousScanList> = ArrayList()
    private var screenHeightPx = 0f
    private var lastScrollY = -1
    private lateinit var mDBViewModel: DBViewModel
    private lateinit var barCodeRecognitionProcessor: BarCodeRecognitionProcessor
    private lateinit var previousScansLiveData: LiveData<List<PreviousScanList>>
    private lateinit var sectionedAdapter: SectionedRecyclerViewAdapter
    private lateinit var currentScanSection: ScanSection
    private lateinit var previousScanSection: ScanSection
    private lateinit var mListener: OnFragmentInteractionListener
    private var cameraSource: CameraSource? = null
    private var flashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        showStandardLayout()
                    } else {
                        showPermissionsLayout()
                    }
                }
    }


    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.clear()
        menuInflater.inflate(R.menu.action_menu_scanner, menu)
        //this makes the icons white and works on older versions of android
        DrawableCompat.setTint(menu.findItem(R.id.action_keyboard).icon, ContextCompat.getColor(ctx, R.color.white))
        DrawableCompat.setTint(menu.findItem(R.id.action_torch).icon, ContextCompat.getColor(ctx, R.color.white))
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (activity != null) {
            val itemId = item.itemId // action with ID action_refresh was selected
            if (itemId == R.id.action_keyboard) {
                val newFragment = AddBarcodeDialog()
                newFragment.setCallback(this)
                newFragment.show(this@ScannerFragment.parentFragmentManager, "TAG_ADD_BAR_CODE_DIALOG")
                // action with ID action_settings was selected
            } else if (itemId == R.id.action_torch) {
                flashOn = !flashOn
                item.icon = if (flashOn) ResourcesCompat.getDrawable(this@ScannerFragment.resources,R.drawable.icon_bulb_on, null) else ResourcesCompat.getDrawable(this@ScannerFragment.resources,R.drawable.icon_bulb_off, null)
                DrawableCompat.setTint(item.icon, ContextCompat.getColor(ctx, R.color.white))
                if(cameraSource!=null)
                    cameraSource?.switchOnTorch(flashOn)
            }
        }
        return super.onOptionsItemSelected(item)
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

    private fun initRecycler() {
        sectionedAdapter = SectionedRecyclerViewAdapter()
        recycler.addItemDecoration(DividerItemDecoration(act, DividerItemDecoration.VERTICAL))
        recycler.layoutManager = LinearLayoutManager(act)
        recycler.adapter = sectionedAdapter
        val callback: ItemTouchHelper.SimpleCallback = SwipeDeleteCallback()
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recycler)
        observe()
    }

    private fun observe() {
        currentScanList.clear()
        previousScansLiveData = mDBViewModel.previousScans
        previousScansLiveData.observe(viewLifecycleOwner, Observer { previousScanLists: MutableList<PreviousScanList> ->
            previousScanList = previousScanLists
            renderData()
            previousScansLiveData.removeObservers(viewLifecycleOwner)
        })
    }

    private fun clearLists() {
        sectionedAdapter.removeAllSections()
    }

    private fun renderData() {
        clearLists()
        val previousScansHeader = getString(R.string.prev_scans)
        val currentScansHeader = getString(R.string.current_scans)
        currentScanSection = ScanSection(currentScansHeader, currentScanList, this, this)
        sectionedAdapter.addSection(currentScanSection)
        //add current scans
        previousScanSection = ScanSection(previousScansHeader, previousScanList, this, this)
        sectionedAdapter.addSection(previousScanSection)
        showSections()
        sectionedAdapter.notifyDataSetChanged()
    }

    private fun showSections() {
        when (currentScanList.size) {
            0 -> currentScanSection.isVisible = false
            else -> currentScanSection.isVisible = true
        }

        when (previousScanList.size) {
            0 -> previousScanSection.isVisible = false
            else -> previousScanSection.isVisible = true
        }

        if (currentScanList.size == 0 && previousScanList.size == 0) hasNoItems() else hasItems()
    }

    private fun hasItems() {
        noItemsLayout.visibility = View.GONE
        recycler.visibility = View.VISIBLE
    }

    private fun hasNoItems() {
        noItemsLayout.visibility = View.VISIBLE
        recycler.visibility = View.GONE
    }

    override fun talkToExpert() {
        mListener.onChat()
    }

    private fun itemInserted() {}

    override fun showProductPage(productDetails: Product) {
        if (productDetails.fccPart.isNullOrEmpty()) return
        if(AppStatus.INTERNET_CONNECTED) {
            lifecycleScope.launch(Dispatchers.IO) {
                val res = V4APICalls.product(productDetails.fccPart,0,200)
                val product = res?.product?.get(0)
                withContext(Dispatchers.Main) {
                    if (product != null && toolbar!=null) {
                       loadProductPage(product)
                    } else {
                        if (toolbar != null) {
                            Alerts.partNotFoundAlert(object : Alerts.AlertResponse {
                                override fun processPositive(output: Any?) {
                                    talkToExpert()
                                }

                                override fun processNegative(output: Any?) {}
                            })
                        }
                    }
                }
            }
        }
        else
        {
            Toast.makeText(ctx,"Please connect to the internet to view product", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadProductPage(product: Product) {
        navigateToAddPartFragmentWithSuperC(product,R.id.action_scannerFragment_to_addPartFragment,this@ScannerFragment)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.doOnLayout {
            mDBViewModel = ViewModelProvider(act)[DBViewModel::class.java]
            flashOn = false
            permissionsLayout.setOnClickListener {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", act.packageName, null)
                })
            }
            initRecycler()
            setUpToolbar()
            showPermissionsLayout()
            permissionsCheck()

            mListener.setSlider(2)
        }
    }


    private fun showStandardLayout()
    {
        showScanLayout()
        initCamera()
    }

    private fun permissionsCheck() {

        when {

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                val alert = AlertDialog.Builder(ctx)
                alert.setTitle("Permissions Required")
                alert.setMessage("To be able to scan we need to access the camera")
                alert.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                    dialog.dismiss()
                }
                alert.setCancelable(false)
                alert.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                    dialog.dismiss()
                }
                alert.show()
            }
            else -> {
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun initBarcodeRecogniser()
    {
        screenHeight
        barCodeRecognitionProcessor = BarCodeRecognitionProcessor(this)
        if(preview!=null) {
            val yOffset = screenHeightPx / 2 - preview.height
            barCodeRecognitionProcessor.previewBoundingBox[0, yOffset.toInt(), preview.width] = preview.height + yOffset.toInt()
            barCodeRecognitionProcessor.previewBoundingBox.javaClass
        }
    }


    private val screenHeight: Unit
        get() {
            val display = act.windowManager?.defaultDisplay
            val outMetrics = DisplayMetrics()
            display?.getMetrics(outMetrics)
            screenHeightPx = outMetrics.heightPixels.toFloat()
        }


    override fun movePreviousListToIndex(searchPos: Int) {
        if (searchPos != -1) {
            val sectionAdapter = sectionedAdapter.getAdapterForSection(previousScanSection)
            val scrollTo = sectionAdapter.getPositionInAdapter(searchPos)
            scrollToPosition(scrollTo)
            highLightPrevious(searchPos)
        }
    }

    private fun highLightPrevious(pos: Int) {
        val sectionAdapter = sectionedAdapter.getAdapterForSection(previousScanSection)
        val position = sectionAdapter.getPositionInAdapter(pos)
        if (position == previousHighLightedPosition) return
        previousHighLightedPosition = position
        previousScanList[pos].flash = true
        sectionedAdapter.notifyItemChanged(position)
    }

    private fun highLightCurrent(pos: Int) {
        val sectionAdapter = sectionedAdapter.getAdapterForSection(currentScanSection)
        val position = sectionAdapter.getPositionInAdapter(pos)
        if (position == currentHighLightedPosition) return
        currentHighLightedPosition = position
        currentScanList[pos].flash = true
        sectionedAdapter.getPositionInSection(position)
    }

    private fun hasMoved(scrollTo: Int): Boolean {
        return lastScrollY != scrollTo
    }

    override fun moveCurrentListToIndex(searchPos: Int) {
        if (searchPos != -1) {
            val sectionAdapter = sectionedAdapter.getAdapterForSection(currentScanSection)
            val scrollTo = sectionAdapter?.getPositionInAdapter(searchPos)
            if (scrollTo != null) {
                scrollToPosition(scrollTo)
            }
            highLightCurrent(searchPos)
        }
    }

    private fun barCodeInPrevious(barcode: String?): Int {
        for (i in previousScanList.indices) {
            if (barcode.equals(previousScanList[i].barcode, ignoreCase = true)) return i
        }
        return -1
    }

    private fun barCodeInCurrent(barcode: String?): Int {
        for (i in currentScanList.indices) {
            if (barcode.equals(currentScanList[i].barcode, ignoreCase = true)) return i
        }
        return -1
    }

    override fun barCodeInCurrent(barcode: FirebaseVisionBarcode): Int {
        return barCodeInCurrent(barcode.displayValue)
    }

    override fun barCodeInPrevious(barcode: FirebaseVisionBarcode): Int {
        return barCodeInPrevious(barcode.displayValue)
    }

    private fun scrollToPosition(scrollTo: Int) {
        if (scrollTo != -1) {
            if(recycler!=null) {
                if (hasMoved(scrollTo)) {
                    recycler.scrollToPosition(scrollTo)
                    lastScrollY = scrollTo
                }
            }
        }
    }

    private fun showPermissionsLayout() {
        cameraWrapper?.visibility = View.GONE
        instructionText?.text = getString(R.string.allow_camera)
        permissionsLayout?.visibility = View.VISIBLE
    }

    private fun showScanLayout() {
        cameraWrapper?.visibility = View.VISIBLE
        permissionsLayout?.visibility = View.GONE
        instructionText?.text = getString(R.string.place_barcode)
    }

    private fun initCamera() {
        initBarcodeRecogniser()
        createCameraSource()
    }

    private fun setUpToolbar() {
        NavigationUI.setupWithNavController(toolbar, NavHostFragment.findNavController(this))
        act.setSupportActionBar(toolbar)
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
    }



    //
    // add to db but do not refresh because we do not want to see the changes until next load
    //
    private fun addToScanList(item: PreviousScanList) {
        act.runOnUiThread {
            item.isRecent = false
            item.flash = false
            mDBViewModel.insert(item)
            item.isRecent = true
            item.flash = true
            currentScanList.add(0, item)
            showSections()
            val adapter = sectionedAdapter.getAdapterForSection(currentScanSection)
            adapter.notifyItemInserted(0)
            itemInserted()
        }
    }

    private fun isInLists(item: PreviousScanList): Boolean {
        for (curr in currentScanList) {
            if (item.barcode.equals(curr.barcode, ignoreCase = true)) return true
        }
        for (prev in previousScanList) {
            if (item.barcode.equals(prev.barcode, ignoreCase = true)) return true
        }
        return false
    }

    //
    // This removes the cached barcode - it is here to prevent repeated firing of the scanner on same object
    //
    private fun removeItemFromSearches(scan: String?) {
        for (i in searchCache.indices.reversed()) {
            if (searchCache[i].equals(scan, ignoreCase = true)) {
                searchCache.removeAt(i)
            }
        }
    }

    override fun clearRecentlyScanned() {
        clearSearchCache()
        currentScanList.clear()
        showSections()
        sectionedAdapter.notifyDataSetChanged()
    }

    override fun clearPreviouslyScanned() {
        clearSearchCache()
        previousScanList.clear()
        showSections()
        mDBViewModel.clearScans()
        sectionedAdapter.notifyDataSetChanged()
    }

    private fun scanComplete(barCode: String?, success: Boolean) {
        removeItemFromSearches(barCode)
        if (!success) {
            showNoItemsFound()
        } else {
            sectionedAdapter.notifyDataSetChanged()
        }
    }

    private fun showNoItemsFound() {
        if(mainCoordinator!=null) {
            lifecycleScope.launch(Dispatchers.Main) {
                Snackbar.make(mainCoordinator, "No products found", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearSearchCache() {
        searchCache.clear()
    }

    private fun addSearch(barCode: String?) {
        if (barCode.isNullOrEmpty())
            return
        if(!searchCache.contains(barCode))
            searchCache.add(barCode)
    }

    private fun searchBarcode(barCode: String?, possiblePartNumber: Boolean, isQr: Boolean) {

        if(AppStatus.INTERNET_CONNECTED) {
            addSearch(barCode)

            if (possiblePartNumber) {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (barCode != null && barCode.isNotEmpty()) {
                        val products = V4APICalls.product(barCode,0,2000)
                        withContext(Dispatchers.Main) {
                            processProducts(products?.product, barCode, isQr)
                        }
                    }
                }
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    if (barCode != null && barCode.isNotEmpty()) {
                        val products = V4APICalls.barcode(barCode,0,2000)
                        withContext(Dispatchers.Main) {
                            processProducts(products?.product, barCode, isQr)
                        }
                    }
                }
            }
        }
        else
        {
            Toast.makeText(ctx,"Please connect to the internet to process barcode", Toast.LENGTH_LONG).show()
        }
    }

    private fun processProducts(products: List<Product>?, barCode: String, isQr: Boolean) {
        if (products.isNullOrEmpty()) {
            scanComplete(barCode, false)
            addBarCodeResult(QrCode(barCode), null, barCode, isQr)
        } else {
            for (product in products) {
                val qr = QrCode(product.barcode)
                addBarCodeResult(qr, product, barCode, isQr)
            }
            scanComplete(barCode, true)
        }
    }


    private fun addBarCodeResult(qr: QrCode, result: Product?, barCode: String, isQr: Boolean) {

        qr.meta = result
        val item: PreviousScanList = if (result != null) {
            //insert into db
            PreviousScanList(
                    result.manufacturer?:"",
                    result.obsolete,
                    result.stock,
                    barCode,
                    result.partNum?:"",
                    if(!result.images.isNullOrEmpty()) result.images[0].url else "",
                    result.classDescription?:"",
                    if(result.fccPart.isNullOrBlank() && isQr) barCode else result.fccPart?:"",
                    result.supersededPartNum?:"",
                    result.supersededFccPart?:"")
        } else {
            PreviousScanList(
                    "",
                    false,
                    0,
                    qr.value?:"",
                    "No Results",
                    "",
                    "Can't find your part? Tap to talk to an expert advisor",
                    "","","")
        }

        if (!isInLists(item) && AppStatus.INTERNET_CONNECTED) {
            lifecycleScope.launch(Dispatchers.Main) {
                addToScanList(item)
                mListener.vibrate()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initCamera()
    }

    override fun onPause() {
        super.onPause()
        preview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(cameraSource!=null)
            cameraSource?.release()
    }

    private fun createCameraSource() {

        cameraSource = CameraSource(this.activity, overlay)
        cameraSource?.setFacing(CameraSource.CAMERA_FACING_BACK)
        cameraSource?.setMachineLearningFrameProcessor(barCodeRecognitionProcessor)
        preview.start(cameraSource, overlay)

    }



    override fun barcodeFound(barcode: FirebaseVisionBarcode, possiblePartNumber: Boolean, isQr: Boolean) {
        searchBarcode(barcode.displayValue, possiblePartNumber,isQr)
    }

    override fun barCodeInList(barcode: FirebaseVisionBarcode): Boolean {
        return barCodeInList(barcode.displayValue)
    }

    private fun barCodeInList(barcode: String?): Boolean {
        //check the search cache
        for (bc in searchCache) {
            if (bc.equals(barcode, ignoreCase = true)) return true
        }
        //check the all and previous scans
        for (prev in currentScanList) {
            if (barcode.equals(prev.barcode, ignoreCase = true)) return true
        }
        for (prev in previousScanList) {
            if (barcode.equals(prev.barcode, ignoreCase = true)) return true
        }
        return false
    }

    private fun removeItem(item: ItemViewHolder) {
        val itemIndex: Int
        val data = item.data
        if (data != null) {
            if (data.isRecent) {
                itemIndex = currentScanList.indexOf(data)
                currentScanList.remove(data)
                val sectionAdapter = sectionedAdapter.getAdapterForSection(currentScanSection)
                if (itemIndex != -1) sectionAdapter.notifyItemRemoved(itemIndex)
            } else {
                itemIndex = previousScanList.indexOf(data)
                previousScanList.remove(data)
                val previousAdapter = sectionedAdapter.getAdapterForSection(previousScanSection)
                if (itemIndex != -1) previousAdapter.notifyItemRemoved(itemIndex)
            }
        }
        removeItemFromSearches(data?.barcode)
        mDBViewModel.delete(item.data)
        showSections()
    }

    override fun onResult(barcode: String?) {
        searchBarcode(barcode, possiblePartNumber = false, isQr = false)
    }

    interface OnFragmentInteractionListener {
        fun vibrate()
        fun setSlider(pos: Int)
        fun onChat()
    }

    inner class SwipeDeleteCallback: SwipeCallback() {
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val holder = viewHolder as ItemViewHolder
            removeItem(holder)
        }
    }

    companion object
}