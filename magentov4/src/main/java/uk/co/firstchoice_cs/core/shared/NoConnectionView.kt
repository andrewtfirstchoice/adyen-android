package uk.co.firstchoice_cs.core.shared

import android.content.Context
import android.graphics.drawable.AnimationDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import uk.co.firstchoice_cs.firstchoice.R

class NoConnectionView : RelativeLayout {

    private var titleText: TextView? = null
    private var messageText: TextView? = null


    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }


    private fun init(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.no_internet_warning, this)

        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.NoConnectionView, 0, 0)
        val titleTxtStr = styledAttributes.getString(R.styleable.NoConnectionView_titleText)
        val messageTextStr = styledAttributes.getString(R.styleable.NoConnectionView_messageText)
        styledAttributes.recycle()

        initComponents()

        setTitleText(titleTxtStr)
        setMessageText(messageTextStr)

        val itImage = findViewById<ImageView>(R.id.image)
        val anim = itImage.drawable as AnimationDrawable
        anim.start()
    }


    private fun setTitleText(titleTxt: CharSequence?) {
        titleText?.text = titleTxt
    }


    private fun setMessageText(msg: CharSequence?) {
        messageText?.text = msg
    }


    private fun initComponents() {

        visibility = View.GONE

        titleText = findViewById(R.id.title)

        messageText = findViewById(R.id.message)
    }
}
