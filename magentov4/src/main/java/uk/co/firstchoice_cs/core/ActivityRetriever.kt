package uk.co.firstchoice_cs.core

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener

class ActivityRetriever(val defaultCurrentActivityListener: DefaultCurrentActivityListener) {

    val layoutInflater: LayoutInflater =
        LayoutInflater.from(defaultCurrentActivityListener.currentActivity)

    val context: Context = defaultCurrentActivityListener.context

    fun getActivity(): Activity? {
        return defaultCurrentActivityListener.currentActivity
    }
}