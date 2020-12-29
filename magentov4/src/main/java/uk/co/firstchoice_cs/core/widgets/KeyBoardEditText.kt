package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText

class KeyBoardEditText : AppCompatEditText {
    var listener: BackButtonListener? = null

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                if (listener!!.onBackButtonClick(imm.isAcceptingText)) {
                    return true
                }
            }
        return super.onKeyPreIme(keyCode, event)
    }

    fun setOnBackButtonListener(listener: BackButtonListener?) {
        this.listener = listener
    }
    interface BackButtonListener {
        fun onBackButtonClick(isKeyboardShowing: Boolean): Boolean
    }
}