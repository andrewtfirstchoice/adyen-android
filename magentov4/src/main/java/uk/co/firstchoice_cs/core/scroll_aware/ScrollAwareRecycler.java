package uk.co.firstchoice_cs.core.scroll_aware;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

public class ScrollAwareRecycler extends RecyclerView {

    public ScrollAwareRecycler(@NonNull Context context) {
        super(context);
    }

    public ScrollAwareRecycler(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollAwareRecycler(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(ScrollAwareInterface callback) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > oldScrollY) {
                    callback.onScrollUp();

                } else if (scrollY == oldScrollY) {
                    callback.onScrollDown();

                } else {
                    callback.onScrollDown();
                }
            });
        }
        else {

            addOnScrollListener(new OnScrollListener() {
                @Override
                public void onScrolled(@NotNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dy > 0)
                        callback.onScrollUp();
                    else if (dy < 0)
                        callback.onScrollDown();
                }
            });
        }
    }
}
