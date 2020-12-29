package uk.co.firstchoice_cs.core.helpers

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView

object SafetyChecks {
    @JvmStatic
    fun isNullOrEmpty(str: String?): Boolean {
        return str?.equals("", ignoreCase = true) ?: true
    }

    @JvmStatic
    fun safeArg(bundle: Bundle?, argName: String?): String {
        if (bundle == null) return ""
        return if (bundle.containsKey(argName)) bundle[argName] as String else ""
    }
    @JvmStatic
    fun safeString(str: String?): String {
        return str ?: ""
    }
    @JvmStatic
    fun safeString(textView: TextView?): String {
        if (textView == null) return ""
        val ret = textView.text.toString()
        return ret.trim { it <= ' ' }
    }
    @JvmStatic
    fun ensureNonNullString(str: String?): String {
        return if(str.isNullOrBlank())
            ""
        else
            str.trim()
    }
    @JvmStatic
    fun ensureNonNullString(editText: EditText?): String {
        if (editText == null) return ""
        return if (editText.text == null) "" else editText.text.toString().trim { it <= ' ' }
    }

}