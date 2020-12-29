package uk.co.firstchoice_cs.core.listeners

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import java.util.*

interface CurrentActivityListener {
    var currentActivity: Activity?
}

/**
 * Since many have to implement this, then create a default class
 */
class DefaultCurrentActivityListener : Application.ActivityLifecycleCallbacks, CurrentActivityListener {
    override var currentActivity: Activity? = null
    lateinit var context: Context
    private val debugName = "AppLifecycleHandler"
    private var resumed = 0
    private var started = 0
    private var isVisible = false
    private var isInForeground = false

    private var currentActivityStack: MutableList<Activity> = ArrayList()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
        context = activity
        currentActivityStack.add(activity)
    }

    /**
     * Check if the activity of the given class is running
     * @param activityClass
     * @return true if running
     */
    fun isActivityRunning(activityClass: Class<out Activity>): Boolean {
        for (activity in currentActivityStack) {
            if (activity.javaClass == activityClass) {
                return true
            }
        }
        return false
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        ++started
        Log.w(debugName, "onActivityStarted -> application is visible: " + (started > 0) + " (" + activity.javaClass + ")")
        setVisible(started > 0)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        ++resumed
        Log.w(debugName, "onActivityResumed -> application is in foreground: " + (resumed > 0) + " (" + activity.javaClass + ")")
        setForeground(resumed > 0)
    }

    override fun onActivityPaused(activity: Activity) {
        --resumed
        Log.w(debugName, "onActivityPaused -> application is in foreground: " + (resumed > 0) + " (" + activity.javaClass + ")")
        setForeground(resumed > 0)
    }

    override fun onActivityStopped(activity: Activity) {
        --started
        Log.w(debugName, "onActivityStopped -> application is visible: " + (started > 0) + " (" + activity.javaClass + ")")
        setVisible(started > 0)
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {

    }


    override fun onActivityDestroyed(activity: Activity) {
        if (activity == currentActivity) {
            currentActivity = null
        }
        currentActivityStack.remove(activity)
    }

    fun isApplicationVisible(): Boolean {
        return started > 0
    }

    fun isApplicationInForeground(): Boolean {
        return resumed > 0
    }


    private fun setVisible(visible: Boolean) {
        if (isVisible == visible) { // no change
            return
        }
        // visibility changed
        isVisible = visible
        Log.w(debugName, "App Visiblility Changed -> application is visible: $isVisible")
        // take some action on change of visibility
    }

    private fun setForeground(inForeground: Boolean) {
        if (isInForeground == inForeground) { // no change
            return
        }
        // in foreground changed
        isInForeground = inForeground
        Log.w(debugName, "App In Foreground Changed -> application is in foreground: $isInForeground")
        // take some action on change of in foreground
    }

}
