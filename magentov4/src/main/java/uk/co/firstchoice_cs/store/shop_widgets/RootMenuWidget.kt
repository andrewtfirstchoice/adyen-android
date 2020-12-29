package uk.co.firstchoice_cs.store.shop_widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.card.MaterialCardView
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.RootMenuWidgetBinding

class RootMenuWidget : MaterialCardView {
    lateinit var binding:RootMenuWidgetBinding
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
        View.inflate(context, R.layout.root_menu_widget, this)
        binding = RootMenuWidgetBinding.bind(this)
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.RootMenuWidget, 0, 0)
        val titleTxtStr = styledAttributes.getString(R.styleable.RootMenuWidget_titleTextRMW)
        val subTitleTxtStr = styledAttributes.getString(R.styleable.RootMenuWidget_subtitleTextRMW)
        val iconLeading = styledAttributes.getDrawable(R.styleable.RootMenuWidget_itemLeadingIconRMW)

        if (styledAttributes.hasValue(R.styleable.RootMenuWidget_titleTextRMW)) {
            binding.title.visibility = View.VISIBLE
            binding.title.text = titleTxtStr
        }
        if (styledAttributes.hasValue(R.styleable.RootMenuWidget_subtitleTextRMW)) {
            binding.subtitle.visibility = View.VISIBLE
            binding.subtitle.text = subTitleTxtStr
        }
        if (styledAttributes.hasValue(R.styleable.RootMenuWidget_itemLeadingIconRMW)) {
            binding.leading.visibility = View.VISIBLE
            binding.leading.setImageDrawable(iconLeading)
        }
        styledAttributes.recycle()
    }
}