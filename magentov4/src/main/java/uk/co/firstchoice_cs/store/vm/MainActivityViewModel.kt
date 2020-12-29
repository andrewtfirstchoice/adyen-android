package uk.co.firstchoice_cs.store.vm

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.co.firstchoice_cs.AppStatus
import uk.co.firstchoice_cs.core.api.v4API.Model

class MainActivityViewModel : ViewModel() {

    data class PartImageResult(
        val requestCode: Int,
        val resultCode: Int,
        val data: Intent?
    )

    val speechLiveData = MutableLiveData<Intent>()
    val partImageLiveData = MutableLiveData<PartImageResult>()
    val internetStatusChanged = MutableLiveData<Boolean>()

    fun setPartsImageResult(requestCode: Int, resultCode: Int, data: Intent?) {
        partImageLiveData.postValue(PartImageResult(requestCode = requestCode,resultCode = resultCode,data = data))
    }

    fun setSpeechData(data:Intent) {
       speechLiveData.postValue(data)
    }
}