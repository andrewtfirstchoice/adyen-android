package uk.co.firstchoice_cs.core.api.v4API


import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.qualifier.named
import uk.co.firstchoice_cs.Settings
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeAllBrands
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeModels
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeNCDocuments
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeNCFilters
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeNCSearchETL
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeNCTabs
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeProducts
import uk.co.firstchoice_cs.core.api.v4API.V4PageSizes.pageSizeSearch
import uk.co.firstchoice_cs.core.helpers.V4APIHelper
import java.net.URLEncoder

object V4APICalls : KoinComponent {
    private val client: OkHttpClient by inject(named(Settings.DEFAULT_SERVICE))

    val url = Settings.v4Url()
    const val SearchTypeFreeText = "SearchETL"
    private const val NC_SEARCH = "NCsearch"
    private const val NC_TABS = "NCTabs"
    private const val NC_MODELS = "NcModels"
    private const val NC_DOCUMENTS = "NCDocuments"
    private const val NC_FILTERS = "NCFilters"
    private const val NC_BRANDS = "Brands"
    private const val NC_PART_CLASS = "PartClass"
    private const val NC_TOP_LEVEL = "TopLevel"
    private const val V4_PRODUCT = "Product"
    private const val V4_BARCODE = "Barcode"
    private const val V4_MANUFACTURER = "Mfr"
    private const val V4_MANUFACTURER_FILTER = "MfrFilters"

    fun manufacturersWithManuals(): ManufacturersWithManuals? {
        var res: ManufacturersWithManuals? = null
        val url = url + V4_MANUFACTURER
        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", V4APIHelper.getBearer())
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                res = Gson().fromJson(s, ManufacturersWithManuals::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun filterManufacturersWithManuals(man: String): ManufacturersWithManuals? {
        var res: ManufacturersWithManuals? = null
        val url = "$url$V4_MANUFACTURER_FILTER/$man"
        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", V4APIHelper.getBearer())
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                res = Gson().fromJson(s, ManufacturersWithManuals::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    fun searchDocument(fileName: String): Documents? {
        var res: Documents? = null
        val url = url + "Document?request=" + fileName
        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", V4APIHelper.getBearer())
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                res = Gson().fromJson(s, Documents::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun searchLinkedParts(sku: String): Products? {
        val encodedSku = URLEncoder.encode(sku, "UTF-8")
        var res: Products? = null
        val url = url + "Product?request.parts=" + encodedSku + "&request.page=0&request.size=100"
        val request = Request.Builder()
            .url(url)
            .method("GET", null)
            .addHeader("Authorization", V4APIHelper.getBearer())
            .build()
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val s = response.body?.string()
                res = Gson().fromJson(s, Products::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    fun product(sku: String, pageStart: Int): Products? {
        val response: Response?
        val encodedSku = URLEncoder.encode(sku, "UTF-8")
        val url =
            "$url$V4_PRODUCT?request.parts=$encodedSku&request.page=$pageStart&request.size=$pageSizeProducts"
        var res: Products? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, Products::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun latestProduct(sku: String?, maxRecursion: Int): Product? {
        val products = product(URLEncoder.encode(sku, "UTF-8"), 0, 1)
        return if (products?.product?.get(0)?.supersededFccPart.isNullOrEmpty()) {
            products?.product?.get(0)
        } else {
            val maxR = maxRecursion - 1
            if (maxR == 0)
                products?.product?.get(0)
            else
                latestProduct(products?.product?.get(0)?.supersededFccPart, maxR)
        }
    }

    fun product(sku: String, pageStart: Int, pageSize: Int): Products? {
        val response: Response?
        val encodedSku = URLEncoder.encode(sku, "UTF-8")
        val url =
            "$url$V4_PRODUCT?request.parts=$encodedSku&request.page=$pageStart&request.size=$pageSize"
        var res: Products? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, Products::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun barcode(barcode: String, pageStart: Int, pageSize: Int): Products? {
        val response: Response?
        val encodedSku = URLEncoder.encode(barcode, "UTF-8")
        val url =
            "$url$V4_BARCODE?request.parts=$encodedSku&request.page=$pageStart&request.size=$pageSize"
        var res: Products? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, Products::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    fun topLevel(code: String): TopLevels? {
        val response: Response?
        val url = "$url$NC_TOP_LEVEL?request.code=$code"
        var res: TopLevels? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, TopLevels::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    fun partClass(classID: String, pageStart: Int, pageSize: Int): ClassID? {
        val response: Response?
        val url =
            "$url$NC_PART_CLASS?request.classId=$classID&request.page=$pageStart&request.size=$pageSize"
        var res: ClassID? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, ClassID::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //https://api.firstchoice-cs.co.uk/V4/Brands?request.page=0&request.size=10000
    fun brands(productCode: String, mda: Boolean, pageStart: Int, pageSize: Int): Brands? {
        val response: Response?
        val url =
            "$url$NC_BRANDS?request.prodCode=$productCode&request.mda=$mda&request.page=$pageStart&request.size=$pageSize"
        var res: Brands? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, Brands::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    fun allBrands(pageStart: Int): Brands? {
        val response: Response?
        val url = "$url$NC_BRANDS?request.page=$pageStart&request.size=$pageSizeAllBrands"
        var res: Brands? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, Brands::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //https://api.firstchoice-cs.co.uk/V4/NCFilters?request.mfr=HOB&request.equipmentCat=&request.classId=&request.search=&request.page=0&request.size=2000
    fun ncFilters(
        man: String,
        equipmentCategory: String,
        classID: String,
        topLevel: String,
        searchString: String,
        pageStart: Int
    ): NCFilter? {
        val response: Response?
        val url =
            "$url$NC_FILTERS?request.mfr=$man&request.equipmentCat=$equipmentCategory&request.classId=$classID&request.topLevel=$topLevel&request.search=$searchString&request.page=$pageStart&request.size=$pageSizeNCFilters"
        var res: NCFilter? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, NCFilter::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //https://api.firstchoice-cs.co.uk/V4/NCDocuments?request.mfr=HOB&request.page=0&request.size=100
    fun ncSearchETL(searchString: String, pageStart: Int): SearchETL? {
        val response: Response?
        val url =
            "$url$SearchTypeFreeText?request.search=$searchString&request.page=$pageStart&request.size=$pageSizeNCSearchETL"
        var res: SearchETL? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                res = Gson().fromJson(response.body?.string(), SearchETL::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //https://api.firstchoice-cs.co.uk/V4/NCDocuments?request.mfr=HOB&request.equipmentCat=&request.classId=&request.search=&request.page=0&request.size=2000
    fun ncDocuments(
        man: String,
        equipmentCategory: String,
        classID: String,
        topLevel: String,
        searchString: String,
        pageStart: Int
    ): NCDocuments? {
        val response: Response?
        val url =
            "$url$NC_DOCUMENTS?request.mfr=$man&request.equipmentCat=$equipmentCategory&request.classId=$classID&request.topLevel=$topLevel&request.search=$searchString&request.page=$pageStart&request.size=$pageSizeNCDocuments"
        var res: NCDocuments? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, NCDocuments::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }

    //https://api.firstchoice-cs.co.uk/V4/NcModels?request.mfr=HOB&request.equipmentCat=&request.classId=&request.search=&request.page=0&request.size=2000
    fun ncModels(
        man: String,
        equipmentCategory: String,
        classID: String,
        topLevel: String,
        searchString: String,
        pageStart: Int
    ): NCModels? {
        val response: Response?
        val url =
            "$url$NC_MODELS?request.mfr=$man&request.equipmentCat=$equipmentCategory&request.classId=$classID&request.topLevel=$topLevel&request.search=$searchString&request.page=$pageStart&request.size=$pageSizeModels"
        var res: NCModels? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                res = Gson().fromJson(response.body?.string(), NCModels::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    //https://api.firstchoice-cs.co.uk/V4/NcSearch?request.mfr=HOB&request.equipmentCat=&request.classId=&request.search=&request.page=0&request.size=2000
    fun ncSearch(
        man: String,
        equipmentCategory: String,
        classID: String,
        topLevel: String,
        searchString: String,
        pageStart: Int
    ): NCParts? {
        val response: Response?
        val url =
            "$url$NC_SEARCH?request.mfr=$man&request.equipmentCat=$equipmentCategory&request.classId=$classID&request.topLevel=$topLevel&request.search=$searchString&request.page=$pageStart&request.size=$pageSizeSearch"
        var res: NCParts? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                res = Gson().fromJson(body, NCParts::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }


    //https://api.firstchoice-cs.co.uk/V4/NcTabs?request.mfr=HOB&request.equipmentCat=&request.classId=&request.search=&request.page=0&request.size=2000
    fun ncTabs(
        man: String,
        equipmentCategory: String,
        classID: String,
        topLevel: String,
        searchString: String,
        pageStart: Int
    ): Stats? {
        val response: Response?
        val url =
            "$url$NC_TABS?request.mfr=$man&request.equipmentCat=$equipmentCategory&request.classId=$classID&request.topLevel=$topLevel&request.search=$searchString&request.page=$pageStart&request.size=$pageSizeNCTabs"
        var res: Stats? = null
        try {
            val request: Request = Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("Authorization", V4APIHelper.getBearer())
                .build()

            response = client.newCall(request).execute()
            if (response.isSuccessful) {
                res = Gson().fromJson(response.body?.string(), Stats::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return res
    }
}