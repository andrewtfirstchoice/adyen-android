package uk.co.firstchoice_cs.core.managers

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import android.util.Log
import com.shockwave.pdfium.PdfiumCore
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


object ThumbNailManager : KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val ctx = defaultCurrentActivityListener.context

    fun createPdfThumbnail(file: File): Bitmap? {
        val pageNumber = 0
        val pdfiumCore = PdfiumCore(App.instance)
        try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    ?: return null
            val pdfDocument: com.shockwave.pdfium.PdfDocument? = pdfiumCore.newDocument(fd)
            pdfiumCore.openPage(pdfDocument, pageNumber)
            val width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber)
            val height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height)
            writeThumbnail(bmp, file)
            pdfiumCore.closeDocument(pdfDocument) // important!
            return bmp
        } catch (e: java.lang.Exception) {
            Log.e("CreatePDFThumbNail", e.message?:"")
        }
        return null
    }


    private fun manualsThumbName(file: File): String {
        var fileName = file.name
        fileName = fileName.replace("pdf", "png")
        return fileName
    }

    fun getThumbImage(documentEntry: DocumentEntry): File? {
        return File(getThumbPath(documentEntry))
    }

    fun getBitmapFromAssets(fileName: String): Bitmap? {
        val assetManager: AssetManager = ctx.assets
        val inputStream: InputStream = assetManager.open(fileName)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        return bitmap
    }

    private fun writeThumbnail(image: Bitmap, file: File) {
        val directory = File(App.thumbnailsDirectory)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val thumbName = manualsThumbName(file)
        val thumbnailFile = File(directory, thumbName)
        try {
            val fos = FileOutputStream(thumbnailFile)
            image.compress(Bitmap.CompressFormat.PNG, 70, fos)
            fos.close()
        } catch (e: Exception) {
            Log.e("writeThumbnail", "Write thumbnail exception" + e.message)
        }
    }


    private fun getThumbPath(documentEntry: DocumentEntry): String {
        val fileName = App.thumbnailsDirectory + documentEntry.url.substring(documentEntry.url.lastIndexOf("/") + 1)
        return fileName.replace("pdf", "png")
    }
}