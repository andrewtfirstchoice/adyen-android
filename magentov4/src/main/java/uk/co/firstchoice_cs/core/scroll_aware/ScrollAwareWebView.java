package uk.co.firstchoice_cs.core.scroll_aware;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class ScrollAwareWebView extends WebView {

    private ScrollAwareInterface callback;

    public ScrollAwareWebView(Context context) {
        super(context);
    }

    public ScrollAwareWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollAwareWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

        if (scrollY > oldScrollY) {
            callback.onScrollUp();

        } else if (scrollY < oldScrollY) {
            callback.onScrollDown();
        }
    }

    public void init(ScrollAwareInterface callback) {

        this.callback = callback;
    }
}