package uk.co.firstchoice_cs.core.helpers

import android.text.TextUtils
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.v4API.Image
import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.api.v4API.Product

object V4APIHelper {

    fun getBearer():String
    {
        return "Bearer " +  Settings.V4_BEARER
    }

    fun productToPart(product: Product): Part {
        return Part(
            barcode = product.barcode,
            classDescription = product.classDescription,
            classId = product.classId,
            fccPart = product.fccPart,
            images = product.images,
            leadTime = product.leadTime,
            manufacturer = product.manufacturer,
            maxStock = product.stock,
            obsolete = product.obsolete,
            partDescription = product.partDescription,
            partNum = product.partNum,
            preferred = product.preferred,
            preferredFCCPart = product.preferredFccPart,
            preferredPartNum = product.preferredPartNum,
            prodCode = product.prodCode,
            stock = product.stock,
            superseded = product.superseded,
            supersededFccPart = product.supersededFccPart,
            supersededPartNum = product.supersededPartNum,
            topLevel = ArrayList(),
            priceStatus = Settings.PriceStatus.NONE
        )
    }

    fun linkedPartToPart(linkedPart: LinkedPart): Part {
        val images = ArrayList<Image>()
        images.add(Image(linkedPart.imageType.toString(),linkedPart.imageUrl.toString()))
        return Part(
            barcode = linkedPart.barcode,
            classDescription = linkedPart.classDescription,
            classId = linkedPart.classId,
            fccPart = linkedPart.fccPart,
            images = images,
            leadTime = "",
            manufacturer = linkedPart.manufacturer,
            maxStock = linkedPart.stock,
            obsolete = linkedPart.obsolete,
            partDescription = linkedPart.partDescription,
            partNum = linkedPart.partNum,
            preferred = linkedPart.preferred,
            preferredFCCPart = linkedPart.preferredFccPart,
            preferredPartNum = linkedPart.preferredPartNum,
            prodCode = linkedPart.prodCode,
            stock = linkedPart.stock,
            superseded = !linkedPart.supersededFccPart.isNullOrEmpty(),
            supersededFccPart = linkedPart.supersededFccPart,
            supersededPartNum = linkedPart.supersededPartNum,
            topLevel = ArrayList(),
            priceStatus = Settings.PriceStatus.NONE
        )
    }

    fun getImage(part: Part, useSmall: Boolean): String {
        val imageUrl = part.images?.get(0)?.url
        var path: String = imageUrl.toString()
        if (!imageUrl.isNullOrEmpty()) {
            if (useSmall) path = path.replace("large", "small")
            if (!path.startsWith("https://")) {
                path = "https://$path"
            }
        }
        return path
    }

    fun getImage(part: Part, suffix: String): String {
        var imageUrl = part.images?.get(0)?.url
        if(imageUrl!=null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                if (!imageUrl.startsWith("https://")) {
                    imageUrl = "https://$part.ImageUrl"
                }
                return imageUrl
            }
            if (imageUrl.isNotEmpty()) {
                imageUrl = imageUrl.substring(0, imageUrl.lastIndexOf('-') + 1)
                imageUrl += "$suffix.jpg"
                return imageUrl
            }
        }
        return ""
    }

    fun is360(part: Part?): Boolean {
        val imageUrl = part?.images?.get(0)?.url
        if(imageUrl!=null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                if (imageUrl.contains("360Spin")) {
                    return true
                }
            }
        }
        return false
    }

    fun is360(part: LinkedPart?): Boolean {
        val imageUrl = part?.imageUrl
        if(imageUrl!=null) {
            if (!TextUtils.isEmpty(imageUrl)) {
                if (imageUrl.contains("360Spin")) {
                    return true
                }
            }
        }
        return false
    }
}