package uk.co.firstchoice_cs

import org.koin.core.KoinComponent
import uk.co.firstchoice_cs.Settings.brandExpiryMS
import uk.co.firstchoice_cs.Settings.partClassesExpiryMS
import uk.co.firstchoice_cs.Settings.topLevelExpiryMS
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.api.magentoAPI.getECCGroupCode
import uk.co.firstchoice_cs.core.api.v4API.*

import java.util.*
import kotlin.collections.HashMap

class GlobalAppData : Any(), KoinComponent {
    var topLevels: TopLevels? = null
    var brands: Brands? = null
    var partClasses: ClassID? = null
    var manufacturersMap: Map<String, List<Brand>>? = null
    var partCategoriesMap: Map<String, List<PartClass>>? = null
    private var priceStockMap: HashMap<String, Product>? = HashMap()
    private var useTestingEnvironment = false



    fun custNumFor(override: Settings.CustomerOverride): Int {
        if (override == Settings.CustomerOverride.IrelandNoVAT)
            return 8833
        if (override == Settings.CustomerOverride.UnitedKingdom)
            return 4753
        if (override == Settings.CustomerOverride.UnitedStates)
            return 25490
        if (override == Settings.CustomerOverride.Jersey)
            return 5828
        return 0
    }

    val websiteEnvironment: String
        get() = if (useTestingEnvironment)
            "http://porkins.firstchoice-cs.co.uk/"  //ERPAPPSERVER
        else
            "https://www.firstchoice-cs.co.uk/" //YADDLE

    private val nonB2BCustomerNumber: Int
        get() = if (useTestingEnvironment)
            20638
        else
            25829

    val nonB2BCustID: String
        get() = if (useTestingEnvironment)
            "FIRST011" //ERPAPPSERVER TEST
        else
            "FCWEBB2C" //YADDLE LIVE

    val customerNumber: Int
        get() = if (getECCGroupCode(App.magentoCustomer)!=-1)
            getECCGroupCode(App.magentoCustomer)
        else
            App.globalData.nonB2BCustomerNumber



    //
    // This returns the price data if it has not expired
    //
    fun getPriceStockFromMap(sku: String, qty: Int): Product? {
        val key = sku + "qty=" + qty
        val price = priceStockMap?.get(key)
        return if(price!=null && !priceExpired(price))
        {
            price
        }
        else
        {
            priceStockMap?.remove(key)
            null
        }
    }

    private fun priceExpired(product:Product): Boolean {
        val now = Date(System.currentTimeMillis()).time
        return now - product.time > Settings.priceExpiryMS
    }
    //
    // This should be the only way to access the price stock map
    //
    fun addPriceStock(sku:String,qty:Int,priceStock:Product)
    {
        priceStock.time = Date(System.currentTimeMillis()).time
        App.globalData.priceStockMap?.set(sku+"qty="+qty, priceStock)
    }

    private fun hasExpired(saveTime:Long, expiryTime:Long):Boolean
    {
        val now = Date(System.currentTimeMillis()).time
        if(now-saveTime > expiryTime)
            return true
        return false
    }

    var brandLoadTime:Long = 0L
    fun shouldGetBrands(): Boolean {
        return brands==null || hasExpired(brandLoadTime,brandExpiryMS)
    }
    var topLevelLoadTime:Long = 0L
    fun shouldGetTopLevel(): Boolean {
        return topLevels==null || hasExpired(topLevelLoadTime, topLevelExpiryMS)
    }
    var partClassesLoadTime:Long = 0L
    fun shouldGetEPartClasses(): Boolean {
        return partClasses==null || hasExpired(partClassesLoadTime, partClassesExpiryMS)
    }
}
