package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.simple_item.view.*
import uk.co.firstchoice_cs.firstchoice.R

class SimpleItem : ConstraintLayout {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        View.inflate(context, R.layout.simple_item, this)
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.SimpleItem, 0, 0)
        val titleTxtStr = styledAttributes.getString(R.styleable.SimpleItem_itemTitleText)
        val iconLeading = styledAttributes.getDrawable(R.styleable.SimpleItem_itemLeadingIcon)
        val iconTrailing = styledAttributes.getDrawable(R.styleable.SimpleItem_itemTrailingIcon)

        if (styledAttributes.hasValue(R.styleable.SimpleItem_itemTitleText)) {
            title.visibility = View.VISIBLE
            title.text = titleTxtStr
        }
        if (styledAttributes.hasValue(R.styleable.SimpleItem_itemTrailingIcon)) {
            check.visibility = View.VISIBLE
            check.setImageDrawable(iconTrailing)
        }
        if (styledAttributes.hasValue(R.styleable.SimpleItem_itemLeadingIcon)) {
            leading.visibility = View.VISIBLE
            leading.setImageDrawable(iconLeading)
        }
        styledAttributes.recycle()
    }
}