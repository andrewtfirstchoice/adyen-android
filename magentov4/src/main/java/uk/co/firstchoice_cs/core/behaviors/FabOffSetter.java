package uk.co.firstchoice_cs.core.behaviors;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class FabOffSetter implements AppBarLayout.OnOffsetChangedListener {

    private final View parent;
    private final FloatingActionButton fab;

    // need to separate translationY on the fab that comes from this behavior
    // and one that comes from other sources
    private float fabTranslationYByThis = 0.0f;

    public FabOffSetter(@NonNull View parent, @NonNull FloatingActionButton child) {
        this.parent = parent;
        this.fab = child;
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        // fab should scroll out down in sync with the appBarLayout scrolling out up.
        // let's see how far along the way the appBarLayout is
        // (if displacementFraction == 0.0f then no displacement, appBar is fully expanded;
        //  if displacementFraction == 1.0f then full displacement, appBar is totally collapsed)
        float displacementFraction = -verticalOffset / (float) appBarLayout.getTotalScrollRange();

        // top position, accounting for translation not coming from this behavior
        float topUntranslatedFromThis = fab.getTop() + fab.getTranslationY() - fabTranslationYByThis;

        // total length to displace by (from position uninfluenced by this behavior) for a full appBar collapse
        float fullDisplacement = parent.getBottom() - topUntranslatedFromThis;

        // calculate new value for displacement coming from this behavior
        float newTranslationYFromThis = fullDisplacement * displacementFraction;

        // update translation value by difference found in this step
        fab.setTranslationY(newTranslationYFromThis - fabTranslationYByThis + fab.getTranslationY());

        // store new value
        fabTranslationYByThis = newTranslationYFromThis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FabOffSetter that = (FabOffSetter) o;

        return parent.equals(that.parent) && fab.equals(that.fab);

    }

    @Override
    public int hashCode() {
        int result = parent.hashCode();
        result = 31 * result + fab.hashCode();
        return result;
    }
}
