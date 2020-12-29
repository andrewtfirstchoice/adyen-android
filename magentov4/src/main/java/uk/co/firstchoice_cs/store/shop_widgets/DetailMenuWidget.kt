package uk.co.firstchoice_cs.store.shop_widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.detail_menu_widget.view.*
import uk.co.firstchoice_cs.firstchoice.R

class DetailMenuWidget : LinearLayout {

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
        View.inflate(context, R.layout.detail_menu_widget, this)
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.DetailMenuWidget, 0, 0)
        val titleTxtStr = styledAttributes.getString(R.styleable.DetailMenuWidget_titleTextDMW)
        val subTitleTxtStr = styledAttributes.getString(R.styleable.DetailMenuWidget_subtitleTextDMW)
        val iconLeading = styledAttributes.getDrawable(R.styleable.DetailMenuWidget_itemLeadingIconDMW)
        val iconTrailing = styledAttributes.getDrawable(R.styleable.DetailMenuWidget_itemTrailingIconDMW)

        if (styledAttributes.hasValue(R.styleable.DetailMenuWidget_titleTextDMW)) {
            title.visibility = View.VISIBLE
            title.text = titleTxtStr
        }
        if (styledAttributes.hasValue(R.styleable.DetailMenuWidget_subtitleTextDMW)) {
            subtitle.visibility = View.VISIBLE
            subtitle.text = subTitleTxtStr
        }
        if (styledAttributes.hasValue(R.styleable.DetailMenuWidget_itemTrailingIconDMW)) {
            check.visibility = View.VISIBLE
            check.setImageDrawable(iconTrailing)
        }
        if (styledAttributes.hasValue(R.styleable.DetailMenuWidget_itemLeadingIconDMW)) {
            leading.visibility = View.VISIBLE
            leading.setImageDrawable(iconLeading)
        }
        styledAttributes.recycle()
    }
}