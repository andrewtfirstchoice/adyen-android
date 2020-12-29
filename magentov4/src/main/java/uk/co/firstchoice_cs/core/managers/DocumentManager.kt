package uk.co.firstchoice_cs.core.managers

import com.google.gson.Gson
import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.document.DocumentEntry
import uk.co.firstchoice_cs.core.document.StoreDocument
import java.io.*

class DocumentManager private constructor() {

    private fun init() {
        store = create()
    }

    val allDocuments: List<DocumentEntry>
        get() {
            return store.documents
        }

    private fun getCountOfDocumentsWithFileName(docEntry: DocumentEntry):Int
    {
        var count = 0
        for (doc in allDocuments)
        {
            if(doc.fileName == docEntry.fileName)
                count++
        }
        return count
    }

    fun updateAllDocuments() {
        if (store.documents == null) return

        writeDocuments()
    }


    fun updateDocument(entry: DocumentEntry) {
        if (store.documents.isNullOrEmpty()) return
        val index = getDocumentIndex(entry)
        if(index!=-1)
            store.documents.removeAt(index)
        entry.lastSeen = System.currentTimeMillis()
        store.documents.add(entry)
        writeDocuments()
    }

    private fun getDocumentIndex(entry: DocumentEntry):Int
    {
        if (!store.documents.isNullOrEmpty()) {
            for (i in 0 until store.documents.size) {
                val loopDoc = store.documents[i]
                if (entry == loopDoc) {
                    return i
                }
            }
        }
        return -1
    }

    fun removeDocument(entry: DocumentEntry) {
        if (store.documents.isNullOrEmpty()) return
        val index = getDocumentIndex(entry)
        if(index!=-1) {
            store.documents.removeAt(index)
            writeDocuments()
            val doc = entry.file
            val thumb = entry.thumb
            if (getCountOfDocumentsWithFileName(entry) == 0) {
                doc.delete()
                thumb.delete()
            }
        }
    }

    private fun writeDocuments() { // Save to file system
        val directory = File(App.docDir)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(App.docDir, DOCUMENT_FILE_NAME)
        val g = Gson()
        try {
            val writer: Writer = FileWriter(file)
            val json = g.toJson(store)
            writer.write(json)
            writer.close()
        } catch (e: IOException) { // Do nothing
        }
    }


    companion object {
        private const val DOCUMENT_FILE_NAME = "documents.json"
        private var singleton: DocumentManager? = null
        private lateinit var store: StoreDocument


       // private val sortLastViewed = Comparator { one: DocumentEntry, two: DocumentEntry ->
        //    val value = two.lastSeen - one.lastSeen
       //     value.compareTo(0L)
      //  }



        @get:Synchronized
        val instance: DocumentManager?
            get() {
                if (singleton == null) {
                    singleton = DocumentManager()
                    singleton?.init()
                }
                return singleton
            }

        fun deleteFirstChoiceFolder() {
            val thumbDir = File(App.thumbnailsDirectory)
            if (thumbDir.isDirectory) {
                val children = thumbDir.list()
                if(children!=null)
                    for (child in children) {
                        File(thumbDir, child).delete()
                    }
            }
            thumbDir.delete()
            val dir = File(App.docDir)
            if (dir.isDirectory) {
                val children = dir.list()
                if(children!=null)
                    for (child in children) {
                        File(dir, child).delete()
                    }
            }
            dir.delete()
        }

        private fun create(): StoreDocument {
            val file = File(App.docDir, DOCUMENT_FILE_NAME)
            val input: InputStream
            input = try {
                FileInputStream(file)
            } catch (e: Exception) {
                return StoreDocument()
            }
            val g = Gson()
            val reader: Reader = InputStreamReader(input)
            val documents: StoreDocument
            documents = try {
                g.fromJson(reader, StoreDocument::class.java)
            } catch (err: Exception) {
                return StoreDocument()
            }
            return documents
        }
    }
}