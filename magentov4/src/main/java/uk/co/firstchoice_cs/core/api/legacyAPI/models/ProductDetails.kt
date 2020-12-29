package uk.co.firstchoice_cs.core.api.legacyAPI.models

import android.text.TextUtils


class ProductDetails {
    var partName: String? = null
    var manufacturer: String? = null
    var stock = 0
    var partNumber: String? = null
    private var image: String? = null
    var barcode: String? = null
    var isObsolete = false
    var supersede: String? = null
    var fccSupersede: String? = null
        private set
    var fccPartNumber: String? = null
        private set
    var imageType: String? = null

    fun getImage(useSmall: Boolean): String? {
        var path = image
        if (!TextUtils.isEmpty(image)) {
            if (useSmall) path = path!!.replace("large", "small")
            if (!path!!.startsWith("https://")) {
                path = "https://$path"
            }
        } else {
            path = ""
        }
        return path
    }

    fun getImage(suffix: String): String? {
        if (!TextUtils.isEmpty(image)) {
            if (!image!!.startsWith("https://")) {
                image = "https://$image"
            }
        }
        if (image != null && image!!.isNotEmpty()) {
            var im = image?.lastIndexOf('-')?.plus(1)?.let { image?.substring(0, it) }
            im += "$suffix.jpg"
            return im
        }
        return ""
    }

    fun is360(): Boolean {
        if (!TextUtils.isEmpty(image)) {
            if (image!!.contains("360Spin")) {
                return true
            }
        }
        return false
    }

}