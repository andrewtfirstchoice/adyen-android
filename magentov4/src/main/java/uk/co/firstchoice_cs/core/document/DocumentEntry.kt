package uk.co.firstchoice_cs.core.document

import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Model
import uk.co.firstchoice_cs.core.api.v4API.DocumentX
import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import java.io.File


class DocumentEntry{
    var readCount: Int = 0
    var updatedIndex = 0
    var selected = false
    var v4Doc: DocumentX? = null
    var linkedTo: LinkedPart? = null
    var category: String? = null
    var deleteChecked = false
    var addCollectionTickSelected = false
    var fav = false
    @Deprecated(message = "This variable - isCatalogue is no longer used")
    var isCatalogue = false //deprecated
    @Deprecated(message = "This variable is - isV3 no longer used")
    var isV3 = false //deprecated
    @Deprecated(message = "This variable is - catalogueImageAsset no longer used")
    var catalogueImageAsset = "" //deprecated
    var lastSeen: Long = 0
    var manualsSearchRenderMode = false
    var lastDownloadedBytes: Long = 0
    var documentType: String? = null

    //var searchModel:uk.co.firstchoice_cs.firstchoice.magento.models.searchmodels.Model?=null

    var downloadID = 0
    var downloading = false
    var bytesDownloaded: Long = 0
    var totalBytes: Long = 0
    var isHeader: Boolean = false
    var headerValue: String? = null

    @JvmField
    var manufacturer = Manufacturer()
    @JvmField
    var model = Model()
    @JvmField
    var document = Document()

    var fileName: String = ""
        get() = document.fileName

    val file: File
        get() = document.file

    val thumb: File
        get() = document.thumbFile

    val isOnDevice: Boolean
        get() = document.isOnDevice

    fun thumbIsOnDevice(): Boolean {
        return document.thumbOnDevice()
    }

    val url: String
        get() = document.url

    val key: String
        get() = url

    @Transient
    var upgraded = false


    override fun toString(): String {
        return manufacturer.Name + " " + model.Name
    }

    override fun hashCode(): Int {
        return document.address.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return if (other is DocumentEntry) {
            document.address == other.document.address
        } else false
    }

    fun resetDownload() {
        lastDownloadedBytes = 0
        bytesDownloaded = 0
        totalBytes = 0
    }


    companion object {
        fun copy(docEntry: DocumentEntry): DocumentEntry
        {
            val newDocEntry = DocumentEntry()
            newDocEntry.selected  = docEntry.selected
            newDocEntry.v4Doc = docEntry.v4Doc
            newDocEntry.linkedTo = docEntry.linkedTo
            newDocEntry.category = docEntry.category
            newDocEntry.deleteChecked = docEntry.deleteChecked
            newDocEntry.addCollectionTickSelected = docEntry.addCollectionTickSelected
            newDocEntry.fav = docEntry.fav
            newDocEntry.upgraded = docEntry.upgraded
            newDocEntry.lastSeen = docEntry.lastSeen
            newDocEntry.isHeader = docEntry.isHeader
            newDocEntry.headerValue = docEntry.headerValue
            newDocEntry.manualsSearchRenderMode = docEntry.manualsSearchRenderMode
            newDocEntry.bytesDownloaded = docEntry.bytesDownloaded
            newDocEntry.totalBytes= docEntry.totalBytes
            newDocEntry.downloading = docEntry.downloading
            newDocEntry.lastDownloadedBytes = docEntry.lastDownloadedBytes
            newDocEntry.downloadID = docEntry.downloadID
            newDocEntry.documentType = docEntry.documentType

            val doc = Document()
            val man = Manufacturer()
            val model = Model()
            doc.address = docEntry.document.address
            doc.setDisplayname(docEntry.document.displayName)
            model.Name = docEntry.model.Name
            model.ModelID = docEntry.model.ModelID
            man.Name = docEntry.manufacturer.Name
            man.Manufacturerid = docEntry.manufacturer.Manufacturerid
            newDocEntry.document = doc
            newDocEntry.manufacturer = man
            newDocEntry.model = model
            return newDocEntry
        }
    }
}