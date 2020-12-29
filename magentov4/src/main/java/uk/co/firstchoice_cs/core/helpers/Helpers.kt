package uk.co.firstchoice_cs.core.helpers


import android.Manifest
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.text.isDigitsOnly
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import coil.load
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior.*
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import io.github.rosariopfernandes.firecoil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.Settings.MAX_RECURSION_S_CEDED
import uk.co.firstchoice_cs.Settings.MY_PERMISSIONS_REQUEST_READ_WRITE
import uk.co.firstchoice_cs.core.api.v4API.*
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.core.managers.ThumbNailManager
import uk.co.firstchoice_cs.firstchoice.R
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object Helpers : KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context
    private val activity = defaultCurrentActivityListener.currentActivity as AppCompatActivity

    @JvmStatic
    val shippingMap = mutableMapOf<String?, String?>()

    @JvmStatic
    val brandsImageMap = mutableMapOf<String, StorageReference>()

    @JvmStatic
    val cataloguesImageMap = mutableMapOf<String, StorageReference>()

    @JvmStatic
    val documentTypesMap = mapOf(
        "Catalogue" to "Catalogue",
        "NULL" to "Parts Diagram",
        "null" to "Parts Diagram",
        "PARTSD" to "Parts Diagram",
        "PDFMODEL" to "Parts Diagram",
        "SDS" to "Safety Data Sheet",
        "TECHB" to "Technical Bulletin",
        "WIRINGD" to "Wiring Diagram",
        "UK" to "Parts Diagram",
        "PartsD" to "Parts Diagram",
        "PDFModel" to "Parts Diagram",
        "SDS" to "Safety Data Sheet",
        "TechB" to "Technical Bulletin",
        "WiringD" to "Wiring Diagram"
    )
    @JvmStatic
    val documentTypesImageMap = mapOf(
        "Catalogue" to R.drawable.partsdiagram,
        "NULL" to R.drawable.partsdiagram,
        "null" to R.drawable.partsdiagram,
        "PARTSD" to R.drawable.partsdiagram,
        "PDFMODEL" to R.drawable.partsdiagram,
        "SDS" to R.drawable.safetysheet,
        "TECHB" to R.drawable.operationmanual,
        "WIRINGD" to R.drawable.operationmanual,
        "UK" to R.drawable.partsdiagram,
        "PartsD" to R.drawable.partsdiagram,
        "PDFModel" to R.drawable.partsdiagram,
        "SDS" to R.drawable.safetysheet,
        "TechB" to R.drawable.operationmanual,
        "WiringD" to R.drawable.operationmanual
    )

    fun getSizeOfDownload(docEntry: DocumentEntry): Int {
        var size = 0
        try {
            val myUrl = URL(docEntry.url)
            val urlConnection = myUrl.openConnection()
            urlConnection.connect()
            size = urlConnection.contentLength
            return size
        } catch (ignored: java.lang.Exception) {
        }
        return size
    }

    fun getDocumentIDFromURL(url: String): String {
        var st: Int = url.reversed().indexOf('/')
        if(st==-1)
            st=0
        return url.trim().substring(url.length - st, url.trim().length - 4)
    }


    @JvmStatic
    fun requestReadWriteStorage(): Boolean {
        if (ContextCompat.checkSelfPermission(
                Objects.requireNonNull(activity),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) { // Should we show an explanation?
            @RequiresApi(Build.VERSION_CODES.M)
            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                generalError(
                    activity.getString(R.string.permissions_disabled),
                    activity.getString(R.string.message_no_permissions)
                )
            }
            else { // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_WRITE
                )
            }
            return false
        }
        return true
    }
    @JvmStatic
    fun bytesIntoHumanReadable(bytes: Long): String? {
        val kilobyte: Long = 1024
        val megabyte = kilobyte * 1024
        val gigabyte = megabyte * 1024
        val terabyte = gigabyte * 1024
        return when {
            bytes in 0 until kilobyte -> {
                "$bytes B"
            }
            bytes in kilobyte until megabyte -> {
                (bytes / kilobyte).toString() + " KB"
            }
            bytes in megabyte until gigabyte -> {
                (bytes / megabyte).toString() + " MB"
            }
            bytes in gigabyte until terabyte -> {
                (bytes / gigabyte).toString() + " GB"
            }
            bytes >= terabyte -> {
                (bytes / terabyte).toString() + " TB"
            }
            else -> {
                "$bytes Bytes"
            }
        }
    }

    @JvmStatic
    fun generalError(title: String?, message: String?) {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle(title)
        alert.setMessage(message)
        alert.setCancelable(true)
        alert.setPositiveButton(R.string.ok, null)
        alert.show()
    }
    @Suppress("DEPRECATION")
    @JvmStatic
    fun setColorFilter(drawable: Drawable, color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        } else {
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }
    }
    @JvmStatic
    fun notEmpty(strings: Array<String>): Int {
        var valid = -1
        var index = 0
        for (str in strings) {
            if (str.isEmpty()) {
                valid = index
                break
            }
            index++
        }
        return valid
    }


    @JvmStatic
    fun hideKeyboard(view: View?) {
        if (view != null) {
            val inputManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    @JvmStatic
    fun getSimpleDateTime(date: Date): String {
        val df = SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
        val dateString = df.format(date)
        val today = Date()
        val ret = dateString == df.format(today)
        if (ret) {
            val todayDf = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            return "Today at " + todayDf.format(date)
        }
        return dateString
    }

    @JvmStatic
    fun launchPhone(activity: AppCompatActivity, phoneNumber: String) {
        val callUri = Uri.parse("tel:$phoneNumber")
        val i = Intent(Intent.ACTION_VIEW, callUri)
        try {
            activity.startActivity(i)
        } catch (ignored: ActivityNotFoundException) {
        }
    }

    @JvmStatic
    fun searchWeb(activity: AppCompatActivity, keyword: String?) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH)
        intent.putExtra(SearchManager.QUERY, keyword) // query contains search string
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
        }
    }

    @JvmStatic
    fun generalNetworkError() {
        val alert = AlertDialog.Builder(ctx)
        alert.setTitle("General Network Error")
        alert.setMessage(R.string.unable_to_retrieve_data)
        alert.setCancelable(true)
        alert.setPositiveButton(R.string.ok, null)
        alert.show()
    }


    @JvmStatic
    fun launchPhone(phoneNumber: String) {
        val callUri = Uri.parse("tel:$phoneNumber")
        val i = Intent(Intent.ACTION_VIEW, callUri)
        try {
            activity.startActivity(i)
        } catch (ignored: ActivityNotFoundException) {
        }
    }

    @JvmStatic
    fun isPlayStoreInstalled(): Boolean {
        return try {
            ctx.packageManager
                    .getPackageInfo(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @JvmStatic
    fun launchEmail(emailAddress: String, body: String?, subject: String?) {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:$emailAddress")
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
        emailIntent.putExtra(Intent.EXTRA_TEXT, body)
        try {
            activity.startActivity(emailIntent)
        } catch (e: ActivityNotFoundException) {
        }
    }
    @JvmStatic
    fun generateLengthString(size: Int): String {
        return when {
            size > 1000000 -> {
                var calc = size.toDouble()
                calc /= 1000000.0
                String.format(Locale.ENGLISH, "%.01f MB", calc)
            }
            size > 1000 -> {
                var calc = size.toDouble()
                calc /= 1000.0
                String.format(Locale.ENGLISH, "%d KB", calc.toInt())
            }
            else -> {
                String.format(Locale.ENGLISH, "%d bytes", size)
            }
        }
    }
    @JvmStatic
    fun logBottomSheetState(caller: String, newState: Int) {

        var bottomSheetState = "UNKNOWN"
        if(newState==STATE_DRAGGING)
            bottomSheetState = "STATE_DRAGGING"
        if(newState==STATE_SETTLING)
            bottomSheetState = "STATE_SETTLING"
        if(newState==STATE_EXPANDED)
            bottomSheetState = "STATE_EXPANDED"
        if(newState==STATE_COLLAPSED)
            bottomSheetState = "STATE_COLLAPSED"
        if(newState==STATE_HIDDEN)
            bottomSheetState = "STATE_HIDDEN"
        if(newState==STATE_HALF_EXPANDED)
            bottomSheetState = "STATE_HALF_EXPANDED"
        LoggingHelper.debugMsg(caller, bottomSheetState)
    }

    @JvmStatic
    fun renderLastViewed(lastSeen: Long, txtLastViewed: TextView?) {
        val date = getSimpleDateTime(Date(lastSeen))
        val calendar = Calendar.getInstance()
        calendar.time = Date(lastSeen)
        val day = calendar[Calendar.DAY_OF_MONTH]
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val calendar2 = Calendar.getInstance()
        val day2 = calendar2[Calendar.DAY_OF_MONTH]
        val year2 = calendar2[Calendar.YEAR]
        val month2 = calendar2[Calendar.MONTH]
        if (day == day2 && year == year2 && month == month2) {
            txtLastViewed?.text = ctx.getString(R.string.just_now_text)
        } else {
            txtLastViewed?.text = date
        }
    }

    @JvmStatic
    fun renderImage(imageView: ImageView?, imageUrl: String?)
    {
        if (imageUrl.isNullOrBlank())
            imageView?.setImageResource(R.drawable.placeholder)
        else {
            if(imageUrl.startsWith("http", true))
                renderImageUri(imageView, imageUrl)
            else {
                imageView?.load(imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
            }
        }
    }
    // Render model image if exists
    // Render Image if that exists
    // Render empty place holder in other cases
    @JvmStatic
    fun renderModelImage(imageView: ImageView?, model: Model?)
    {
        if(model?.imageUrl.isNullOrBlank()) {
            val imageRef = brandsImageMap[model?.prodCode.toString().toUpperCase(Locale.ROOT)]
            when {
                imageRef != null -> {
                    renderBrandImage(imageView, imageRef)
                }
                else -> {
                    imageView?.load(R.drawable.placeholder)
                }
            }
        }
        else
        {
            renderImage(imageView, model?.imageUrl)
        }
    }

    @JvmStatic
    fun renderBrandImage(imageView: ImageView?, imageRef: StorageReference?)
    {
        when {
            imageRef != null -> {
                imageView?.load(imageRef) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
            }
            else -> {
                imageView?.load(R.drawable.placeholder)
            }
        }
    }

    @JvmStatic
    fun renderManufacturerName(documentEntry: DocumentEntry?, manufacturer: TextView)
    {
        if(documentEntry?.manufacturer?.Name==documentEntry?.model?.Name)
            manufacturer.text = activity.getString(R.string.first_choice_consu)
        else
            manufacturer.text = documentEntry?.manufacturer?.Name
    }

    @JvmStatic
    fun renderDocumentEntryImage(
        documentEntry: DocumentEntry?,
        imgPdfThumbnail: AppCompatImageView?
    )
    {
        if(imgPdfThumbnail==null||documentEntry==null)
            return
        val catalogueUrl = documentEntry.document.catalogueUrl
        if(catalogueUrl.isNullOrBlank() && documentEntry.manufacturer.Name == activity.getString(R.string.first_choice_consu) && documentEntry.model.Value.isDigitsOnly()) {
            renderCatalogueImage(imgPdfThumbnail, documentEntry.model.Value.toString())
        }
        else if(catalogueUrl.isNullOrBlank() && documentEntry.manufacturer.Name != "First Choice Consumables") {
            val file = ThumbNailManager.getThumbImage(documentEntry)
                renderImage(imgPdfThumbnail, file)
        }
        else
        {
            renderImage(imgPdfThumbnail, catalogueUrl)
        }
    }
    @JvmStatic
    fun renderDocumentImage(imageView: ImageView?, item: Document) {
        if(!item.prodCode.isNullOrBlank()) {
            val imageRef = brandsImageMap[item.prodCode.toString().toUpperCase(Locale.ROOT)]
            when {
                imageRef != null -> {
                    renderBrandImage(imageView, imageRef)
                }
                else -> {
                    renderImage(imageView, R.drawable.placeholder)
                }
            }
        }
        else{
            renderImage(imageView, R.drawable.placeholder)
        }
    }

    @JvmStatic
    fun renderCatalogueImage(imageView: ImageView?, catalogueID: String?)
    {
        val imageRef = cataloguesImageMap[catalogueID]
        when {
            imageRef != null -> {
                imageView?.load(imageRef) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder)
                    error(R.drawable.placeholder)
                }
            }
            else -> {
                renderImage(imageView, R.drawable.placeholder)
            }
        }
    }

    private fun renderImageUri(imageView: ImageView?, imageUrl: String?)
    {
        if (imageUrl.isNullOrBlank())
            renderImage(imageView, R.drawable.placeholder)
        else {
            val uri = Uri.parse(imageUrl)
            imageView?.load(uri) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
            }
        }
    }

    @JvmStatic
    fun renderImage(imageView: ImageView?, file: File?)
    {
        if (file==null)
            renderImage(imageView, R.drawable.placeholder)
        else
            imageView?.load(file) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
            }
    }

    @JvmStatic
    fun renderImage(imageView: ImageView?, id: Int?)
    {
        if (id==null)
            renderImage(imageView, R.drawable.placeholder)
        else
            imageView?.load(id) {
                crossfade(true)
                placeholder(R.drawable.placeholder)
                error(R.drawable.placeholder)
            }
    }

    @JvmStatic
    fun navigateToAddPartFragmentWithSuperC(product: Product, actionRes: Int, fragment: Fragment) {
        val part = V4APIHelper.productToPart(product)
        navigateToAddPartFragmentWithSuperC(part, actionRes, fragment)
    }

    @JvmStatic
    fun navigateToAddPartFragmentWithSuperC(p: Part, actionRes: Int, fragment: Fragment) {
        var part: Part = p
        var bundle: Bundle?
        if (!part.supersededFccPart.isNullOrEmpty()) {
            activity.lifecycleScope.launch(Dispatchers.IO) {
                val sku = part.fccPart
                if (sku != null) {
                    val product = V4APICalls.latestProduct(sku, MAX_RECURSION_S_CEDED)
                    if (product != null) {
                        withContext(Dispatchers.Main)
                        {
                            part = V4APIHelper.productToPart(product)
                            bundle = bundleOf("ProductArg" to Gson().toJson(part))
                            NavHostFragment.findNavController(fragment).navigate(
                                actionRes,
                                bundle,
                                null,
                                null
                            )
                        }
                    }
                }
            }
        } else {
            bundle = bundleOf("ProductArg" to Gson().toJson(part))
            NavHostFragment.findNavController(fragment).navigate(actionRes, bundle, null, null)
        }
    }
}