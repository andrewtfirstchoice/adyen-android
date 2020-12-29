package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.search_bar.view.*
import uk.co.firstchoice_cs.core.helpers.Helpers.hideKeyboard
import uk.co.firstchoice_cs.core.managers.KeyWordMngr
import uk.co.firstchoice_cs.firstchoice.R

class SearchBarView : LinearLayout {
    var isFocusedAndKeywordDismissed = false
    private var focusChangeListener: OnFocusChangeListener? = null
    private var requestListener: OnSearchInputListener? = null
    private val clearButtonAction = OnClickListener { etSearch.text?.clear() }
    private val voiceSearchAction = OnClickListener {
        if (requestListener != null) {
            notifySearchKeyword()
        }
    }
    private val scannerAction = OnClickListener {
        requestListener?.onScanRequest()
    }

    constructor(context: Context?) : super(context) {
        if (!isInEditMode) init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        if (!isInEditMode) init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (!isInEditMode) init()
    }

    override fun setOnFocusChangeListener(listener: OnFocusChangeListener) {
        focusChangeListener = listener
    }

    fun setOnSearchInputRequest(listener: OnSearchInputListener?) {
        requestListener = listener
    }

    private fun init() {
        View.inflate(context, R.layout.search_bar, this)
        setupSearchText()
        setupSearchButtons()
    }

    private fun setupSearchText() {
        etSearch.onFocusChangeListener = OnFocusChangeListener { view: View?, isFocused: Boolean ->
            isFocusedAndKeywordDismissed = false
            if (!isFocused) {
                hideKeyboard(etSearch)
            }
            if (focusChangeListener != null) {
                focusChangeListener!!.onFocusChange(view, isFocused)
            }
        }
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                text
            }

            override fun afterTextChanged(s: Editable) {}
        })


        etSearch.setOnBackButtonListener(object : KeyBoardEditText.BackButtonListener {
            override fun onBackButtonClick(isKeyboardShowing: Boolean): Boolean {
                if (etSearch.hasFocus()) {
                    etSearch.clearFocus()
                    return true
                }
                return false
            }
        })

        etSearch.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                notifySearchKeyword()
                return@setOnEditorActionListener true
            }
            false
        }
        etSearch.clearFocus()
    }

    private val text: Unit
        get() {
            if (!TextUtils.isEmpty(etSearch.text.toString().trim { it <= ' ' })) {
                btnSearch.isEnabled = true
                btnSearch.alpha = 1.0f
                setupClearButton()
            } else {
                btnSearch.isEnabled = false
                btnSearch.alpha = 0.3f
                setupSearchButtons()
            }
        }

    private fun notifySearchKeyword() {
        val keyword = etSearch.text.toString().trim { it <= ' ' }
        if (!TextUtils.isEmpty(keyword)) {
            val st = KeyWordMngr.SearchTerm(keyword)
            etSearch.clearFocus()
            if (requestListener != null) {
                requestListener!!.onKeywordRequest(st)
            }
        }
    }

    private fun setupSearchButtons() {
        btnSearch.setImageResource(R.drawable.ic_magnify_grey600_24dp)
        btnSearch.setOnClickListener(voiceSearchAction)
        btnScanner.setImageResource(R.drawable.ic_barcode_scan_grey600_24dp)
        btnScanner.setOnClickListener(scannerAction)
    }

    private fun setupClearButton() {
        btnScanner.setImageResource(R.drawable.ic_clear_black_18dp)
        btnScanner.setOnClickListener(clearButtonAction)
    }

    interface OnSearchInputListener {
        fun onScanRequest()
        fun onKeywordRequest(keyword: KeyWordMngr.SearchTerm?)
    }
}