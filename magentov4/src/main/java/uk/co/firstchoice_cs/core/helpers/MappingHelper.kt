package uk.co.firstchoice_cs.core.helpers

import uk.co.firstchoice_cs.core.api.v4API.LinkedPart
import uk.co.firstchoice_cs.core.api.v4API.Product


fun getProductImageType(prod: Product):String
{
        val type = prod.images?.get(0)?.type
        return if(type.isNullOrBlank()) "Part Diagram" else type
}

object MappingHelper {

    fun mapProductToLinkedPart(prod: Product): LinkedPart {
        return LinkedPart(
                fccPart = prod.fccPart?:"",
                partDescription = prod.partDescription?:"",
                partNum = prod.partNum?:"",
                barcode = prod.barcode?:"",
                stock = prod.stock?:0,
                manufacturer = prod.manufacturer?:"",
                categories = prod.categories,
                classDescription = prod.classDescription?:"",
                classId = prod.classId?:"",
                prodCode = prod.prodCode?:"",
                haz_ClassNumber = prod.haz_ClassNumber?:"",
                haz_GovermentId = prod.haz_GovernmentId?:"",
                haz_TechnicalName = prod.haz_TechnicalName?:"",
                haz_SubRisk = prod.haz_SubRisk?:"",
                preferred = prod.preferred?:false,
                preferredPartNum = prod.preferredPartNum?:"",
                preferredFccPart = prod.preferredFccPart?:"",
                supersededFccPart = prod.supersededFccPart?:"",
                supersededPartNum = prod.supersededPartNum?:"",
                hazardous = prod.hazardous?:false,
                imageUrl = prod.images?.get(0)?.url ?:"",
                imageType = getProductImageType(prod),
                obsolete = prod.obsolete?:false,
                isPart = prod.isPart?:0,
                isModel = prod.isModel?:0,
                partName = prod.partDescription?:"")
    }
}
