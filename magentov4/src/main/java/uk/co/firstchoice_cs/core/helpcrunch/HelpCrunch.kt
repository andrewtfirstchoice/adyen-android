package uk.co.firstchoice_cs.core.helpcrunch

import android.content.Context
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import com.helpcrunch.library.core.Callback
import com.helpcrunch.library.core.HelpCrunch
import com.helpcrunch.library.core.models.user.HCUser
import com.helpcrunch.library.core.options.HCOptions
import com.helpcrunch.library.core.options.HCPreChatForm
import com.helpcrunch.library.core.options.design.HCMessageAreaTheme
import com.helpcrunch.library.core.options.design.HCTheme
import com.helpcrunch.library.core.options.design.HCToolbarAreaTheme
import com.helpcrunch.library.core.options.files.FileExtension
import org.koin.core.KoinComponent
import org.koin.core.inject
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.listeners.DefaultCurrentActivityListener
import uk.co.firstchoice_cs.firstchoice.R
import java.util.*

object HelpCrunch : KoinComponent {
    private val defaultCurrentActivityListener: DefaultCurrentActivityListener by inject()
    private val act = defaultCurrentActivityListener.currentActivity as AppCompatActivity

    @JvmStatic
    fun onChat() {
        val pref = act.getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        pref?.getString("company", "")

        val hcToolbarAreaTheme = HCToolbarAreaTheme.Builder()
                .setBackgroundColor(R.color.fcBlue)
                .setAgentsTextColor(R.color.white)
                .setStatusBarColor(R.color.colorPrimaryDark)
                .build()

        val messageAreaTheme = HCMessageAreaTheme.Builder()
                .setButtonType(HCMessageAreaTheme.ButtonType.TEXT)
                .build()

        val theme = HCTheme.Builder(R.color.fcBlue, true)
                .setMessageAreaTheme(messageAreaTheme)
                .setToolbarAreaTheme(hcToolbarAreaTheme)
                .build()

        val regExPostCode = "^([A-Za-z][A-Ha-hJ-Yj-y]?[0-9][A-Za-z0-9]? ?[0-9][A-Za-z]{2}|[Gg][Ii][Rr] ?0[Aa]{2})$"
        val preChatForm = HCPreChatForm.Builder()
                .withName(true)
                .withEmail(true)
                .withField("postcode", "Postcode", true,inputType = InputType.TYPE_CLASS_TEXT,validationRegex = regExPostCode)
                .withPhone(false)
                .withCompany(true)
                .build()

        val options = HCOptions.Builder()
                .setTheme(theme)
                .setPreChatForm(preChatForm)
                .setFileExtensions(arrayOf(
                        FileExtension("PDF", "pdf"),
                        FileExtension("IMAGES", arrayOf("jpg", "png"))
                ))
                .build()



        HelpCrunch.showChatScreen(
                options = options,
                    callback = object : Callback<Any?>() {
                        override fun onError(message: String) {
                        }
                        override fun onSuccess(result: Any?) {
                        }
                    }
            )
    }
    @JvmStatic
    fun updateHelperCrunch(showHelp: Boolean) {
        val pref = act.getSharedPreferences("ClientDetails", Context.MODE_PRIVATE)
        val postCodeMap: MutableMap<String, String?> = HashMap()
        postCodeMap["postcode"] = SafetyChecks.ensureNonNullString(pref.getString("postcode", ""))
        val user = HCUser.Builder()
                .withName(SafetyChecks.ensureNonNullString(pref.getString("name", "")))
                .withEmail(SafetyChecks.ensureNonNullString(pref.getString("email", "")))
                .withCustomData(postCodeMap)
                .withPhone(SafetyChecks.ensureNonNullString(pref.getString("phone", "")))
                .withCompany(SafetyChecks.ensureNonNullString(pref.getString("company", "")))
                .build()


        HelpCrunch.updateUser(user, object : Callback<HCUser>() {
            override fun onSuccess(result: HCUser) {
                if (showHelp)
                    onChat()
            }
        })
    }
}