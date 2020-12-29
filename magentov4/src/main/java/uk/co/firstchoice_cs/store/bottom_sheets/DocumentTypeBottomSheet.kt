package uk.co.firstchoice_cs.store.bottom_sheets

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.document_types_bottom_sheet.view.*
import kotlinx.android.synthetic.main.document_types_bottom_sheet_item.view.*
import uk.co.firstchoice_cs.core.api.v4API.Document
import uk.co.firstchoice_cs.core.api.v4API.File
import uk.co.firstchoice_cs.core.helpers.Helpers
import uk.co.firstchoice_cs.core.helpers.Helpers.documentTypesImageMap
import uk.co.firstchoice_cs.core.helpers.Helpers.documentTypesMap
import uk.co.firstchoice_cs.firstchoice.R



interface DocumentTypeBottomSheetInterface {
    fun downloadManual(doc: Document?, fileX: File)
}

class DocumentTypeBottomSheet : LinearLayout {
    private var doc: Document? = null
    private var docs: ArrayList<File> = ArrayList()
    private var sheetCollapsed = true
    private val documentAdapter = DocumentAdapter()
    private var sheetBehavior: BottomSheetBehavior<*>? = null
    private var callback: DocumentTypeBottomSheetInterface? = null


    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.document_types_bottom_sheet, this)
        if (!isInEditMode) {
            initComponents()
        }
    }

    fun setSheetBehavior(sheetBehavior: BottomSheetBehavior<*>?) {
        this.sheetBehavior = sheetBehavior
    }

    fun setCallback(callback: DocumentTypeBottomSheetInterface) {
        this.callback = callback
    }

    private fun initComponents() {

        closeButton.setOnClickListener {
            hide()
        }

        recycler.adapter = documentAdapter
        recycler.layoutManager = LinearLayoutManager(this.context)
        recycler.addItemDecoration(DividerItemDecoration(recycler.context, DividerItemDecoration.VERTICAL))
    }

    private fun populateHeader() {
        val item = doc
        if (!item?.files.isNullOrEmpty()) {
            val filename = item?.files?.get(0)?.name
            Helpers.renderImage(modelImageHeader,filename)
        } else
            modelImageHeader.setImageResource(R.drawable.placeholder)

        manufacturerModelHeader.text = item?.manufacturer
        partIDModelHeader.text = item?.partNum
        when {
            item?.files.isNullOrEmpty() -> numManualsHeader.text = "0 Manuals"
            item?.files?.size == 1 -> numManualsHeader.text = "${item.files.size} Manual available"
            else -> numManualsHeader.text = "${item?.files?.size} Manual available"
        }
    }


    fun expand(document: Document) {
        sheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        sheetCollapsed = false

        Handler().postDelayed({
            doc = document
            populateHeader()
            docs.clear()
            val f = doc?.files
            if (!f.isNullOrEmpty()) {
                for (i in f) {
                    docs.add(i)
                }
            }
            recycler.adapter?.notifyDataSetChanged()
        }, 1000)
    }

    fun hide() {
        sheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        sheetCollapsed = true
    }


    inner class DocumentAdapter : RecyclerView.Adapter<DocumentAdapter.DocumentViewHolder>() {
        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
            val item = docs[position]
            val docType = documentTypesMap.getValue(item.docType)
            holder.subtitle.text = docType
            holder.viewed.visibility = View.GONE
            holder.image.setImageResource(documentTypesImageMap.getValue(item.docType))
            holder.itemView.setOnClickListener {
                hide()
                callback?.downloadManual(doc, docs[position])
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.document_types_bottom_sheet_item, parent, false)
            return DocumentViewHolder(view)
        }

        override fun getItemCount(): Int = docs.size

        inner class DocumentViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
            var image: ImageView = mView.image
            var subtitle: TextView = mView.subtitle
            var viewed: TextView = mView.viewed
            override fun toString(): String {
                return super.toString() + " '"
            }
        }
    }
}