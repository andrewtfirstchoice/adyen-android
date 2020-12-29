package uk.co.firstchoice_cs.partsid

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.database.*
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Manufacturer
import uk.co.firstchoice_cs.core.api.legacyAPI.models.Model
import uk.co.firstchoice_cs.core.helpers.SafetyChecks
import uk.co.firstchoice_cs.core.viewmodels.FireBaseViewModel
import java.util.*

class PartsIDViewModel : ViewModel() {
    var focusedImageIndex = 0
    var manufacturerList: List<Manufacturer> = ArrayList()
    var modelList: ArrayList<Model> = ArrayList()
    var partRequestNumber = 0
    val requestPath: String = "partRequests/requestNumber"

    var bitmap1: Bitmap? = null
    var bitmap2: Bitmap? = null
    var bitmap3: Bitmap? = null
    var ref1: String? = null
    var ref2: String? = null
    var ref3: String? = null
    var path1: Uri? = null
    var path2: Uri? = null
    var path3: Uri? = null

    fun resetBitmaps() {
        bitmap1 = null
        bitmap2 = null
        bitmap3 = null
        ref1 = null
        ref2 = null
        ref3 = null
        path1 = null
        path2 = null
        path3 = null
    }

    private fun incrementPartRequests(): Int {
        partRequestNumber += 1
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference(requestPath)
        myRef.setValue(partRequestNumber)
        return partRequestNumber
    }

    fun clear() {
        resetBitmaps()
    }


    fun sendPartIDEnquiryToFireBase(company: String, email: String, name: String, phone: String, manufacturer: String, model: String, serial: String, additional: String, postcode: String) {
        val database = FirebaseDatabase.getInstance()
        val myRef2 = database.getReference(requestPath)
        myRef2.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) {
                    Log.e(FireBaseViewModel.TAG, "Request Number listener value received null - check the FireBase documentItem : Request number")
                } else {
                    partRequestNumber = dataSnapshot.value.toString().toInt()
                    val myRef = database.getReference("partRequests")
                    val ref = myRef.push()
                    val dict: MutableMap<String, Any> = HashMap()
                    dict["contactCompany"] = SafetyChecks.ensureNonNullString(company)
                    dict["contactEmail"] = SafetyChecks.ensureNonNullString(email)
                    dict["contactName"] = SafetyChecks.ensureNonNullString(name)
                    dict["contactPhone"] = SafetyChecks.ensureNonNullString(phone)
                    // dict.put("testing",TestingConstants.SUGGESTION_TEST_ENABLED);
                    dict["partManufacturer"] = SafetyChecks.ensureNonNullString(manufacturer)
                    dict["partModel"] = SafetyChecks.ensureNonNullString(model)
                    dict["partSerial"] = SafetyChecks.ensureNonNullString(serial)
                    dict["requestNumber"] = incrementPartRequests()
                    dict["timestamp"] = ServerValue.TIMESTAMP
                    dict["contactPostcode"] = SafetyChecks.ensureNonNullString(postcode)
                    val photos: MutableMap<String, Any> = HashMap()
                    val refs: MutableMap<String, Any> = HashMap()
                    photos["comments"] = additional
                    if (path1 != null && ref1 != null) {
                        photos["image01"] =path1.toString()
                        refs["image01"] = ref1!!
                    }
                    if (path2 != null && ref2 != null) {
                        photos["image02"] = path2.toString()
                        refs["image02"] = ref2!!
                    }
                    if (path3 != null && ref3 != null) {
                        photos["image03"] = path3.toString()
                        refs["image03"] = ref3!!
                    }
                    dict["photos"] = photos
                    dict["refs"] = refs
                    ref.setValue(dict)
                    clear()
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}