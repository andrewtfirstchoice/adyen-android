package uk.co.firstchoice_cs.core.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import uk.co.firstchoice_cs.firstchoice.R
import uk.co.firstchoice_cs.firstchoice.databinding.FooterWidgetBinding

class FooterWidget : LinearLayout {

    lateinit var binding:FooterWidgetBinding
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
        View.inflate(context, R.layout.footer_widget, this)
        binding = FooterWidgetBinding.bind(this)
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.FooterWidget, 0, 0)
        val titleText = styledAttributes.getString(R.styleable.FooterWidget_titleTextFW)
        val subtitleText = styledAttributes.getString(R.styleable.FooterWidget_subtitleTextFW)
        val buttonText = styledAttributes.getString(R.styleable.FooterWidget_buttonTextFW)

        binding.title.text = titleText
        binding.subtitle.text = subtitleText
        binding.button.text = buttonText
        styledAttributes.recycle()
    }
}