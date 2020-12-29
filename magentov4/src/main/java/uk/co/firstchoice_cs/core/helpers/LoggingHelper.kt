package uk.co.firstchoice_cs.core.helpers

import android.util.Log
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.firstchoice.BuildConfig

object LoggingHelper {
    @JvmStatic
    fun debugMsg(caller:String?,msg:String?)
    {
        if(BuildConfig.DEBUG)
            Log.d(caller.toString(),msg.toString())
    }
    @JvmStatic
    fun errorMsg(caller:String?,msg:String?)
    {
        if(BuildConfig.DEBUG)
            Log.e(caller.toString(),msg.toString())
    }

}