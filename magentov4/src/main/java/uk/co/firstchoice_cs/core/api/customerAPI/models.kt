package uk.co.firstchoice_cs.core.api.customerAPI

data class Auth(
    val result: String?
)

data class Customer(
    val customer: List<CustomerX>?
)

data class CustomerX(
    val billingAddress: CustomerAddres?,
    val cust: Int?,
    val custId: String?,
    val checkDuplicatePo: Boolean?,
    val customerAddress: List<CustomerAddres>?,
    val emailAddress: String?,
    val name: String?,
    val shipVias: List<ShipVia>?,
    val taxRate: Double?,
    val taxRegionCode: String?
)

data class CustomerAddres(
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val country: String?,
    val countryNum: Int?,
    val county: String?,
    val name: String?,
    val postCode: String?
)

data class ShipVia(
        val carrier: String?,
        val description: String?,
        val miscInfo: List<MiscInfo>?,
        val shipViaCode: String?,
        @Transient
        var selected: Boolean = false
)
data class MiscInfo(
        val MiscAmt: Double?,
        val MiscCode: String?
)


data class PriceStock(
    val product: List<Product>?
)

data class Product(
        val avgLeadTime: Double?,
        val barcode: String?,
        val customerCost: Double?,
        val customerExtendedCost: Double?,
        val discountAmt: Double?,
        val discountPercentage: Double?,
        val imageUrl: String?,
        val manufacturer: String?,
        val obsolete: Boolean?,
        val partDescription: String?,
        val partNumber: String?,
        val prodCode: String?,
        val prodCode_SupersededBy: String?,
        val qty: Int?,
        val stock: Int?,
        val supersededBy: String?,
        val unitPrice: Double?,
        @Transient
        var time: Long = 0L
)


data class Addresses(
    val addresses: List<Addresse>?
)

data class Addresse(
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val country: String?,
    val countryNum: Int?,
    val county: String?,
    val name: String?,
    val postcode: String?,
    val telephoneNum: String?,
    val fax: String?,
    val email: String?
)


data class Orders(
    val orders: List<Order>
)

data class Order(
    val eccOrderNum: String?,
    val estimatedDelivery: String?,
    val lines: Int?,
    val orderDate: String?,
    val orderNum: Int?,
    val orderTotal: Double?,
    val poNum: String?,
    val status: String?
)

data class OrderDetails(
    val orderDetails: List<OrderDetail>?
)

data class OrderDetail(
    val Shipments: List<Shipment>?,
    val eccOrderNum: String?,
    val lineTotal: Double?,
    val lines: List<LineX>?,
    val miscCharges: Double?,
    val oneTimeShipAddress: List<OneTimeShipAddres>?,
    val orderNetAmount: Double?,
    val orderNum: Int?,
    val orderOpen: Boolean?,
    val orderTotal: Double?,
    val poNum: String?,
    val readyToProcess: Boolean?,
    val shipToAddress: List<ShipToAddres>?,
    val shipViaCode: String?,
    val termsCode: String?,
    val useOTS: Boolean?,
    val vatAmount: Double?,
    val voidOrder: Boolean?
)

data class Shipment(
    val lines: List<Line>?,
    val packNum: Int?,
    val shipStatus: String?,
    val shipVia: String?,
    val shipdate: String?,
    val trackingNumber: String?,
    val trackingUrl: String?
)

data class LineX(
    val Hazardous: Boolean?,
    val cost: Double?,
    val customerPrice: Double?,
    val description: String?,
    val discountAmount: Double?,
    val discountPercent: Double?,
    val line: Int?,
    val `open`: Boolean?,
    val partNum: String?,
    val qty: Double?,
    val stats: List<Stat>?
)

data class OneTimeShipAddres(
    val OTSPhoneNum: String?,
    val otsAddress1: String?,
    val otsAddress2: String?,
    val otsAddress3: String?,
    val otsCity: String?,
    val otsContact: String?,
    val otsCountryNum: Int?,
    val otsName: String?,
    val otsState: String?,
    val otsZip: String?
)

data class ShipToAddres(
    val address1: String?,
    val address2: String?,
    val address3: String?,
    val city: String?,
    val country: String?,
    val name: String?,
    val phoneNum: String?,
    val shipToNum: String?,
    val state: String?,
    val zip: String?
)

data class Line(
    val OurInventoryShipQty: Double?,
    val TotalNetWeight: Double?,
    val invoiced: Boolean?,
    val lineDesc: String?,
    val orderLine: Int?,
    val partNum: String?,
    val shipCmpl: Boolean?,
    val wum: String?
)

data class Stat(
    val allocated: String?,
    val picking: String?,
    val shipped: String?
)