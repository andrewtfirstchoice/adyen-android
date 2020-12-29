package uk.co.firstchoice_cs.core.behaviors;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Behavior for FABs that does not support anchoring to AppBarLayout, but instead translates the FAB
 * out of the bottom in sync with the AppBarLayout collapsing towards the top.
 * <p>
 * Extends FloatingActionButton.Behavior to keep using the pre-Lollipop shadow padding offset.
 */
public class AppBarBoundFabBehavior extends FloatingActionButton.Behavior {

    public AppBarBoundFabBehavior(Context context, AttributeSet attrs) {
        super();
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            ((AppBarLayout) dependency).addOnOffsetChangedListener(new FabOffSetter(parent, child));
        }
        return dependency instanceof AppBarLayout || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        //noinspection SimplifiableIfStatement
        if (dependency instanceof AppBarLayout) {
            // if the dependency is an AppBarLayout, do not allow super to react on that
            // we don't want that behavior
            return true;
        }
        return super.onDependentViewChanged(parent, fab, dependency);
    }
}