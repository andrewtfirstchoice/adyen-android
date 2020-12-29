package uk.co.firstchoice_cs.partsid

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.storage.FirebaseStorage
import com.myhexaville.smartimagepicker.ImagePicker
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.AppStatus.INTERNET_CONNECTED
import uk.co.firstchoice_cs.core.alerts.Alerts.showAlert
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Model
import uk.co.firstchoice_cs.core.helpers.SafetyChecks.ensureNonNullString
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.ProductDataMngr
import uk.co.firstchoice_cs.core.scroll_aware.ScrollAwareInterface
import uk.co.firstchoice_cs.core.viewmodels.AnalyticsViewModel
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.PartsIdFragmentBinding
import uk.co.firstchoice_cs.store.vm.MainActivityViewModel
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList

class PartsIDFragment : Fragment(R.layout.parts_id_fragment), KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity
    private var uploading = false
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var imagePicker: ImagePicker
    private lateinit var mViewModel: PartsIDViewModel
    private lateinit var mFireBaseViewModel: FireBaseViewModel
    private lateinit var sessionUID: String
    private lateinit var company: String
    private lateinit var email: String
    private lateinit var name: String
    private lateinit var phone: String
    private lateinit var postcode: String
    private lateinit var manufacturer: String
    private lateinit var model: String
    private lateinit var serial: String
    private lateinit var additional: String
    lateinit var mainViewModel: MainActivityViewModel
    lateinit var binding:PartsIdFragmentBinding

    private fun setUpToolbar() {
        val activity = activity as AppCompatActivity?
        activity?.setSupportActionBar(binding.toolbar)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.action_menu_parts_id, menu)
        super.onCreateOptionsMenu(menu, menuInflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear) {
            clear()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleFocusChange(selectedView: View?, hasFocus: Boolean) {
        if (hasFocus) {
            if (binding.partsIDManufacturerAuto == selectedView && binding.partsIDManufacturerAuto.text.isEmpty()) {
                mViewModel.modelList.clear()
                populateAdapterModels(mViewModel.modelList)
                binding.partsIDModelEditAuto.text.clear()
            }

            binding.informationBox.visibility = View.GONE
            binding.toolbar.visibility = View.GONE
            mListener?.hideNavBar()
        } else {
            binding.informationBox.visibility = View.VISIBLE
            binding.toolbar.visibility = View.VISIBLE
            mListener?.showNavBar()
        }
    }


    fun clear() {
        clearFields()
        clearAllImageViews()
    }

    private fun clearAllImageViews() {
        clearImage(1)
        clearImage(2)
        clearImage(3)
    }

    private fun clearFields() {
        binding.partsIDModelEditAuto.text.clear()
        binding.partsIDManufacturerAuto.text.clear()
        binding.partsIDSerialEdit.text?.clear()
        binding.partsIDMAdditionalEdit.text?.clear()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = PartsIdFragmentBinding.bind(view)
        sessionUID = UUID.randomUUID().toString()
        mViewModel = ViewModelProvider(requireActivity()).get(PartsIDViewModel::class.java)
        mFireBaseViewModel = ViewModelProvider(requireActivity()).get(FireBaseViewModel::class.java)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainActivityViewModel::class.java)
        mainViewModel.partImageLiveData.observe(viewLifecycleOwner, {

               imagePicker.handleActivityResult(it.resultCode, it.requestCode, it.data)
                when (mViewModel.focusedImageIndex) {
                    1 -> {
                        if (binding.image1.drawable != null) {
                            binding.download1ImageView.visibility = View.GONE
                            mViewModel.bitmap1 = (binding.image1.drawable as BitmapDrawable).bitmap
                        }
                    }
                    2 -> {
                        if (binding.image2.drawable != null) {
                            binding.download2ImageView.visibility = View.GONE
                            mViewModel.bitmap2 = (binding.image2.drawable as BitmapDrawable).bitmap
                        }
                    }
                    3 -> {
                        if (binding.image3.drawable != null) {
                            binding.download3ImageView.visibility = View.GONE
                            mViewModel.bitmap3 = (binding.image3.drawable as BitmapDrawable).bitmap
                        }
                    }
                }
        })
        binding.partsIDManufacturerAuto.threshold = 1
        binding.partsIDModelEditAuto.threshold = 1
        val focusChangeListener = OnFocusChangeListener { selectedView: View?, hasFocus: Boolean -> handleFocusChange(selectedView, hasFocus) }
        binding.partsIDPhoneEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) send()
            false
        }
        binding.questionButton.setOnClickListener { Toast.makeText(context, R.string.postcode_reason, Toast.LENGTH_LONG).show() }

        //set up all of the edit boxes to listen for focus changes so that they can hide show toolbar and navbar to make room
        binding.partsIDManufacturerAuto.onFocusChangeListener = focusChangeListener
        binding.partsIDSerialEdit.onFocusChangeListener = focusChangeListener
        binding.partsIDMAdditionalEdit.onFocusChangeListener = focusChangeListener
        binding.partsIDNameEdit.onFocusChangeListener = focusChangeListener
        binding.partsIDCompanyEdit.onFocusChangeListener = focusChangeListener
        binding.partsIDEmailEdit.onFocusChangeListener = focusChangeListener
        binding.partsIDPhoneEdit.onFocusChangeListener = focusChangeListener

        binding.partsIDManufacturerAuto.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (binding.partsIDManufacturerAuto.text.isEmpty()) {
                    mViewModel.modelList.clear()
                    populateAdapterModels(mViewModel.modelList)
                    binding.partsIDModelEditAuto.text.clear()
                }
            }

        })

        binding.progressBar.visibility = View.GONE
        loadClientDetails()
        loadManufacturers()
        loadImages()
        binding.btnHelpIDPart.setOnClickListener {
            if (INTERNET_CONNECTED) {
                send()
            } else {
                Toast.makeText(this.context, getString(R.string.offlinemsg), Toast.LENGTH_SHORT).show()
            }
        }
        binding.partsIDModelEditAuto.onFocusChangeListener = OnFocusChangeListener setOnFocusChangeListener@{ selectedView: View?, hasFocus: Boolean ->
            handleFocusChange(selectedView, hasFocus)
            if (hasFocus) {
                val selectedManufacturer = binding.partsIDManufacturerAuto.text.toString().trim { it <= ' ' }
                for (man in mViewModel.manufacturerList) {
                    if (man.Name.equals(selectedManufacturer, ignoreCase = true)) {
                        mViewModel.modelList.clear()
                        binding.partsIDModelEditAuto.setText("")
                        loadModels(man.Manufacturerid)
                        return@setOnFocusChangeListener
                    }
                }
            }
        }
        binding.imageWrapper1.setOnLongClickListener { clearImage(1) }
        binding.imageWrapper1.setOnClickListener {
            processDownload(1)
        }
        binding.imageWrapper2.setOnLongClickListener {
            clearImage(2)
        }
        binding.imageWrapper2.setOnClickListener {
            processDownload(2)
        }
        binding.imageWrapper3.setOnLongClickListener {
            clearImage(3)
        }
        binding.imageWrapper3.setOnClickListener {
            processDownload(3)
        }

        mListener?.restoreFabState()
        binding.scrollView.init(object : ScrollAwareInterface {
            override fun onScrollUp() {
                if (isAdded) mListener?.onScrollUp()
            }

            override fun onScrollDown() {
                if (isAdded) mListener?.onScrollDown()
            }
        })
        setUpToolbar()
        mListener?.setSlider(3)
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                NavHostFragment.findNavController(this@PartsIDFragment).navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        permissionsCheck()

        binding.permissionsLayout.setOnClickListener {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", act.packageName, null)
            })
        }
    }


    private fun loadImages() {
        if (mViewModel.bitmap1 != null)
            binding.image1.setImageBitmap(mViewModel.bitmap1)
        if (mViewModel.bitmap2 != null)
            binding.image2.setImageBitmap(mViewModel.bitmap2)
        if (mViewModel.bitmap3 != null)
            binding.image3.setImageBitmap(mViewModel.bitmap3)
    }

    private fun permissionsCheck() {

        val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        showStandardLayout()
                    } else {
                        showPermissionsLayout()
                    }
                }
        when {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                showStandardLayout()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                val alert = AlertDialog.Builder(ctx)
                alert.setTitle("Permissions Required")
                alert.setMessage("To be able to photograph parts and send a request we need to access the camera")
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
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    private fun showPermissionsLayout() {
        binding.permissionsLayout.visibility = View.VISIBLE
        binding.photoWrapper.visibility = View.GONE
    }

    private fun showStandardLayout() {
        binding.permissionsLayout.visibility = View.GONE
        binding.photoWrapper.visibility = View.VISIBLE
    }





    private fun clearImage(i: Int): Boolean {
        when (i) {
            1 -> {
                binding.image1.setImageResource(android.R.color.transparent)
                mViewModel.bitmap1 = null
                binding.download1ImageView.visibility = View.VISIBLE
            }
            2 -> {
                binding.image2.setImageResource(android.R.color.transparent)
                mViewModel.bitmap2 = null
                binding.download2ImageView.visibility = View.VISIBLE
            }
            3 -> {
                binding.image3.setImageResource(android.R.color.transparent)
                mViewModel.bitmap3 = null
                binding.download3ImageView.visibility = View.VISIBLE
            }
        }
        return true
    }

    private fun isValid(manufacturer: String, name: String, company: String, email: String, postcode: String): Boolean {
        if (manufacturer.isEmpty()) {
            showAlert("We need more information", "Please provide the part manufacturer", null)
            return false
        }
        if (mViewModel.bitmap1 == null && mViewModel.bitmap2 == null && mViewModel.bitmap3 == null) {
            showAlert("We need more information", "Please provide up to three photos of your part", null)
            return false
        }
        if (name.isEmpty()) {
            showAlert("We need more information", "Please provide your name", null)
            return false
        }
        if (company.isEmpty()) {
            showAlert("We need more information", "Please provide your company", null)
            return false
        }
        if (email.isEmpty()) {
            showAlert("We need more information", "Please provide your email", null)
            return false
        }
        if (!isValidEmail(email)) {
            showAlert("There was an error with your details", "Please provide a valid email address", null)
            return false
        }
        if (postcode.isEmpty()) {
            showAlert("We need more information", "Please provide your postcode", null)
            return false
        }
        return true
    }

    private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches()
    }

    private fun showError() {
        binding.progressBar.visibility = View.GONE
        uploading = false
        showAlert("Error Submitting", "Please check your internet connection and retry", null)
    }



    private fun send() {

        if (uploading) return
        if (!INTERNET_CONNECTED) {
            showError()
            return
        }
        postcode = ensureNonNullString(binding.partsIDPostCode)
        company = ensureNonNullString(binding.partsIDCompanyEdit)
        email = ensureNonNullString(binding.partsIDEmailEdit)
        name = ensureNonNullString(binding.partsIDNameEdit)
        phone = ensureNonNullString(binding.partsIDPhoneEdit)
        manufacturer = ensureNonNullString(binding.partsIDManufacturerAuto)
        model = ensureNonNullString(binding.partsIDModelEditAuto)
        serial = ensureNonNullString(binding.partsIDSerialEdit)
        additional = ensureNonNullString(binding.partsIDMAdditionalEdit)
        val valid = isValid(manufacturer, name, company, email, postcode)
        if (valid) {
            saveClientDetails()
            uploading = true
            binding.progressBar.visibility = View.VISIBLE
            processNext(0)
        }
    }


    private fun resizeImage(original: Bitmap): Bitmap {
        var scaledBitmap: Bitmap
        var targetWidth = 0
        var targetHeight = 0
        var shouldResize = false
        try {
            scaledBitmap = original
            val aspect = original.width.toDouble() / original.height.toDouble()
            if (original.width > original.height) {
                if (original.width > 1920) {
                    shouldResize = true
                    targetWidth = 1920
                    targetHeight = (targetWidth / aspect).toInt()
                }
            } else if (original.height > 1080) {
                shouldResize = true
                targetHeight = 1080
                targetWidth = (targetHeight * aspect).toInt()
            }
            if (shouldResize) {
                scaledBitmap = Bitmap.createScaledBitmap(original, targetWidth, targetHeight, false)
            }
            if (scaledBitmap != original) {
                if (!original.isRecycled) {
                    original.recycle()
                }
            }
            return scaledBitmap
        } catch (ex: Exception) {
            Log.e("FireBaseHelper", "unable to resize file $ex")
        }
        return original
    }

    //
    // Recursive call
    //
    private fun getCompressedSize(originalBitmap: Bitmap, quality: Int): ByteArrayOutputStream {
        val copy = Bitmap.createBitmap(originalBitmap)
        val baos = ByteArrayOutputStream()
        copy.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        //recycle here - ensure that it is not the same object or there will be a crash
        if (originalBitmap != copy) {
            if (!copy.isRecycled) copy.recycle()
        }
        return if (baos.toByteArray().size < BYTE_LIMIT_UPLOAD) baos else getCompressedSize(originalBitmap, quality - 5)
    }

    private fun uploadImage(act: Activity, image: Bitmap?, index: Int, sessionUUID: String?) {
        val storage = FirebaseStorage.getInstance()
        val storageReference = storage.reference
        if (image == null) {
            processNext(index + 1)
        } else {
            val bitmap = resizeImage(image)
            val ref = storageReference.child("partRequests/" + sessionUUID + " /" + UUID.randomUUID().toString())
            val data = getCompressedSize(bitmap, 100).toByteArray()

            if (!INTERNET_CONNECTED) {
                showError()
                return
            }

            ref.putBytes(data).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUrl: Uri ->
                    var urlStr = downloadUrl.toString()
                    urlStr = urlStr.replace("%2F".toRegex(), "/")
                    val start = urlStr.indexOf("partRequests/")
                    val end = urlStr.lastIndexOf("?")
                    val uuid = urlStr.substring(start, end - 1)
                    when (index) {
                        0 -> {
                            mViewModel.path1 = downloadUrl
                            mViewModel.ref1 = uuid
                        }
                        1 -> {
                            mViewModel.path2 = downloadUrl
                            mViewModel.ref2 = uuid
                        }
                        2 -> {
                            mViewModel.path3 = downloadUrl
                            mViewModel.ref3 = uuid
                        }
                    }
                    processNext(index + 1)
                }
            }
                    .addOnFailureListener { e: Exception ->
                        Toast.makeText(act, "Failed " + e.message, Toast.LENGTH_SHORT).show()
                        processNext(index + 1)
                    }
        }
    }

    private fun getCopy(bmp: Bitmap?): Bitmap? {
        return bmp?.copy(bmp.config, bmp.isMutable)
    }

    private fun processNext(index: Int) {
        when (index) {
            0 -> {
                uploadImage(requireActivity(), getCopy(mViewModel.bitmap1), 0, sessionUID)
            }
            1 -> {
                uploadImage(requireActivity(), getCopy(mViewModel.bitmap2), 1, sessionUID)
            }
            2 -> {
                uploadImage(requireActivity(), getCopy(mViewModel.bitmap3), 2, sessionUID)
            }
            else -> {
                if (!INTERNET_CONNECTED) {
                    showError()
                }
                else {
                    uploading = false
                    binding.progressBar.visibility = View.GONE

                    mViewModel.sendPartIDEnquiryToFireBase(
                        company,
                        email,
                        name,
                        phone,
                        manufacturer,
                        model,
                        serial,
                        additional,
                        postcode)
                    postAnalytics()
                    sentMessage()
                }
            }
        }
    }

    private fun sentMessage() {
        val alert = AlertDialog.Builder(requireActivity())
        alert.setTitle("Request Submitted")
        alert.setMessage("Thanks for submitting your request.  We'll contact you directly with details about your part soon")
        alert.setPositiveButton("Okay") { _: DialogInterface?, _: Int ->
            if (parentFragmentManager.backStackEntryCount > 0) {
                parentFragmentManager.popBackStack()
            }
        }
        alert.show()
    }

    private fun postAnalytics() {
        val mAnalyticsViewModel =
            ViewModelProvider(requireActivity()).get(AnalyticsViewModel::class.java)
        val b = Bundle()
        b.putString("manufacturer", manufacturer)
        b.putString("model", model)
        b.putString("serial", serial)
        mAnalyticsViewModel.mFirebaseAnalytics?.logEvent("PartsIDSubmitted", b)
    }

    private fun loadClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        binding.partsIDNameEdit.setText(pref.getString("name", ""))
        binding.partsIDCompanyEdit.setText(pref.getString("company", ""))
        binding.partsIDEmailEdit.setText(pref.getString("email", ""))
        binding.partsIDPhoneEdit.setText(pref.getString("phone", ""))
        binding.partsIDPostCode.setText(pref.getString("postcode", ""))
    }

    private fun saveClientDetails() {
        val pref = requireActivity().getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString("name", ensureNonNullString(binding.partsIDNameEdit))
        editor.putString("company", ensureNonNullString(binding.partsIDCompanyEdit))
        editor.putString("email", ensureNonNullString(binding.partsIDEmailEdit))
        editor.putString("phone", ensureNonNullString(binding.partsIDPhoneEdit))
        editor.putString("postcode", ensureNonNullString(binding.partsIDPostCode))
        editor.apply()
    }

    private fun processDownload(num: Int) {
        mViewModel.focusedImageIndex = num
        if (mViewModel.focusedImageIndex == 1) {
            imagePicker = ImagePicker(requireActivity(),
                    this
            ) { uri: Uri? -> binding.image1.setImageURI(uri) }
        }
        if (mViewModel.focusedImageIndex == 2) {
            imagePicker = ImagePicker(requireActivity(),
                    this
            ) { uri: Uri? -> binding.image2.setImageURI(uri) }
        }
        if (mViewModel.focusedImageIndex == 3) {
            imagePicker = ImagePicker(requireActivity(),
                    this
            ) { uri: Uri? -> binding.image3.setImageURI(uri) }
        }
        imagePicker.choosePicture(true)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        imagePicker.handlePermission(requestCode, grantResults)
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

    private fun loadManufacturers() {
        if (mViewModel.manufacturerList.isNotEmpty()) {
            populateAdapter(mViewModel.manufacturerList)
        } else {
            ProductDataMngr.getInstance().getManufacturers(object : ProductDataMngr.ManufacturerListener {
                override fun onCompletion(manufacturers: MutableList<Manufacturer>?) {
                    if (manufacturers != null) {
                        mViewModel.manufacturerList = manufacturers
                    }
                    populateAdapter(mViewModel.manufacturerList)
                }

                override fun onError(err: Int, msg: String?) {

                }

            }, false)
        }
    }


    private fun loadModels(manufacturerID: String) {
        if (mViewModel.modelList.isNotEmpty()) {
            populateAdapterModels(mViewModel.modelList)
        } else {


            ProductDataMngr.getInstance().getModels(manufacturerID,object : ProductDataMngr.ModelListener {
                override fun onError(err: Int, msg: String?) {}
                override fun onCompletion(models: List<Model>) {
                    populateAdapterModels(models)
                }
            },true)
        }
    }

    private fun populateAdapter(manufacturers: List<Manufacturer>) {
        val names = ArrayList<String>()
        for (i in manufacturers) {
            names.add(i.Name)
        }
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line, names)
        binding.partsIDManufacturerAuto.setAdapter(adapter)
    }

    private fun populateAdapterModels(models: List<Model>) {
        val names = ArrayList<String>()
        for (i in models) {
            names.add(i.Name)
        }
        val adapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line, names)
        binding.partsIDModelEditAuto.setAdapter(adapter)
    }


    interface OnFragmentInteractionListener {
        fun onScrollDown()
        fun onScrollUp()
        fun restoreFabState()
        fun setSlider(pos: Int)
        fun hideNavBar()
        fun showNavBar()
    }

    companion object {
        private const val BYTE_LIMIT_UPLOAD = 400000.0
    }
}