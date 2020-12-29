package uk.co.firstchoice_cs.core.api.v4API

import uk.co.firstchoice_cs.Settings

data class Brands(
        val brands: List<Brand>
)

data class Brand(
        val description: String,
        val mda: Boolean,
        val prodCode: String,
        @Transient
        var headerLetter: String = "",
)

data class ClassID(
        val partClass: List<PartClass>
)

data class PartClass(
        val classDescription: String,
        val classId: String,
        @Transient
        var headerLetter: String = "",
)

data class TopLevels(
        val categories: List<Category>?
)

data class Category(
        val description: String?,
        val equipmentCategory: List<EquipmentCategory>?,
        val tlc: String?,
        val id: String?
)

data class EquipmentCategory(
        var description: String?,
        var tlc: String?,
        val id: String?,
        @Transient
        var headerLetter: String = "",
        @Transient
        var isHeader: Boolean = false,
        @Transient
        var TopLevel: String,
        @Transient
        var TopLevelDesc: String,
        @Transient
        var selected: Boolean = false
)

data class Products(
        val product: List<Product>
)

data class Product(
        val categories: List<Category>?,
        val barcode: String?,
        val classDescription: String?,
        val classId: String?,
        val documents: List<DocumentProd>?,
        val fccPart: String?,
        val fitsModel: List<FitsModel>?,
        val haz_ClassNumber: String?,
        val haz_GovernmentId: String?,
        val haz_SubRisk: String?,
        val haz_TechnicalName: String?,
        val hazardous: Boolean?,
        val height: Double?,
        val images: List<Image>?,
        val isModel: Int?,
        val isPart: Int?,
        val leadTime: String?,
        val length: Double?,
        val linkedParts: List<LinkedPart>?,
        val manufacturer: String?,
        val obsolete: Boolean?,
        val partDescription: String?,
        val partNum: String?,
        val preferred: Boolean?,
        val preferredFccPart: String?,
        val preferredPartNum: String?,
        val prodCode: String?,
        val soldInMultiples: Int?,
        val stock: Int?,
        val superseded: Boolean?,
        val supersededFccPart: String?,
        val supersededPartNum: String?,
        val weight: Double?,
        val width: Double?
)

data class Part(
        val barcode: String?,
        val classDescription: String?,
        val classId: String?,
        val fccPart: String?,
        val images: List<Image>?,
        val leadTime: String?,
        val manufacturer: String?,
        val maxStock: Int?,
        val obsolete: Boolean?,
        val partDescription: String?,
        val partNum: String?,
        val preferred: Boolean?,
        val preferredFCCPart: String?,
        val preferredPartNum: String?,
        val prodCode: String?,
        val stock: Int?,
        val superseded: Boolean?,
        val supersededFccPart: String?,
        val supersededPartNum: String?,
        val topLevel: List<TopLevel>?,
        var priceStatus: Settings.PriceStatus = Settings.PriceStatus.NONE
)


data class DocumentProd(
        val description: String?,
        val docType: String?,
        val name: String?,
        val url: String?
)


data class FitsModel(
        val barcode: String?,
        val Categories: List<Category>?,
        val classDescription: String?,
        val classId: String?,
        val fccPart: String?,
        val imageType: String?,
        val imageUrl: String?,
        val manufacturer: String?,
        val partDescription: String?,
        val partNum: String?,
        val prodCode: String?,
        val topLevel: List<TopLevel>?
)

data class TopLevel(
        val description: String,
        val equipmentCategory: List<EquipmentCategory>,
        val tlc: String
)


data class Image(
        val type: String,
        val url: String
)

//get price stock
data class LinkedPart(
        val partName: String?, //for v3
        val isModel: Int?,
        val isPart: Int?,
        val barcode: String?,
        val categories: List<Category>?,
        val classDescription: String?,
        val classId: String?,
        val fccPart: String?,
        val haz_ClassNumber: String?,
        val haz_GovermentId: String?,
        val haz_SubRisk: String?,
        val haz_TechnicalName: String?,
        val hazardous: Boolean?,
        val imageType: String?,
        val imageUrl: String?,
        val manufacturer: String?,
        val obsolete: Boolean,
        val partDescription: String?,
        val partNum: String?,
        val preferred: Boolean?,
        val preferredFccPart: String?,
        val preferredPartNum: String?,
        val prodCode: String?,
        val stock: Int?,
        val supersededFccPart: String?,
        val supersededPartNum: String?,
        var priceStatus: Settings.PriceStatus = Settings.PriceStatus.NONE
)

data class NCFilter(
        val filters: List<Filter>?
)

data class Filter(
        val `class`: List<Clas>,
        val manufacturer: List<Manufacturer>,
        val topLevel: List<NCTopLevel>
)

data class Clas(
        val description: String,
        val id: String,
        @Transient
        var headerLetter: String? = "",
        @Transient
        var selected: Boolean = false
)


data class NCTopLevel(
        val equipmentCategory: List<EquipmentCategory>,
        val tlc: String,
        val description: String,
        @Transient
        var headerLetter: String? = "",
        @Transient
        var selected: Boolean = false
)


data class NCDocuments(
        val documents: List<Document>
)

data class File(
        val url: String,
        val description: String,
        val name: String,
        val docType: String
)


data class NCModels(
        val models: List<Model>
)


data class Model(
        val barcode: String?,
        val fccPart: String?,
        val imageType: String?,
        val imageUrl: String?,
        val linkedParts: Int?,
        val manufacturer: String?,
        val partDescription: String?,
        val partNum: String?,
        val prodCode: String?,
        val topLevel: List<TopLevel>?
){
        var isFavourite: Boolean = false
}




data class NCParts(
        val parts: List<Part>?
)


data class Stats(
        val stats: StatsX
)

data class StatsX(
        val documents: Int,
        val models: Int,
        val parts: Int
)

data class SearchETL(
        val classId: String?,
        val equipmentCategory: String?,
        val manufacturer: String?,
        val Page: Int?,
        val search: String?,
        val size: Int?,
        val topLevel: Int?
)

// This is the model for the document search for manufacturer
data class ManufacturersWithManuals(
        val manufacturers: List<Manufacturer>?
)


data class Manufacturer(
        val manufacturer: String?,
        val prodCode: String?,
        val topLevel: List<TopLevel>?,
        @Transient
        var headerLetter: String? = "",
        @Transient
        var selected: Boolean = false
)


//Documents call
data class Documents(
        val document: List<DocumentX>?
)

data class DocumentX(
        val description: String?,
        val docType: String?,
        val linkedTo: List<LinkedPart>?,
        val name: String?,
        val url: String?
)

data class Document(
        val classDescription: String?,
        val classId: String?,
        val equipmentCategory: List<EquipmentCategory>?,
        val fccPart: String?,
        val files: List<File>?,
        val images: List<Image>?,
        val isModel: Int?,
        val isPart: Int?,
        val manufacturer: String?,
        val partDescription: String?,
        val partNum: String?,
        val prodCode: String?
)
