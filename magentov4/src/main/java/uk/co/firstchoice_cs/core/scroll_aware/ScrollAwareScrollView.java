package uk.co.firstchoice_cs.core.scroll_aware;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ScrollAwareScrollView extends ScrollView {

    private ScrollAwareInterface callback;

    public ScrollAwareScrollView(Context context) {
        super(context);
    }

    public ScrollAwareScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollAwareScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
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
