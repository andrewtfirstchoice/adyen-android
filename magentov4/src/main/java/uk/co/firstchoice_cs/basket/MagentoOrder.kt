package uk.co.firstchoice_cs.basket

class MagentoOrder(
    val entity: Entity
)

class Entity(
    val base_currency_code: String,
    val base_discount_amount: Double,
    val base_discount_tax_compensation_amount: Double,
    val base_grand_total: Double,
    val base_shipping_amount: Double,
    val base_shipping_incl_tax: Double,
    val base_shipping_tax_amount: Double,
    val base_subtotal: Double,
    val base_subtotal_incl_tax: Double,
    val base_tax_amount: Double,
    val base_to_global_rate: Double,
    val base_to_order_rate: Double,
    val base_total_due: Double,
    val base_total_paid: Double,
    val billing_address: Address,
    val customer_email: String,
    val customer_firstname: String,
    val customer_id: Int,
    val customer_is_guest: Int,
    val customer_lastname: String,
    val customer_middlename: String,
    val discount_amount: Double,
    val discount_tax_compensation_amount: Double,
    val extension_attributes: ExtensionAttributes,
    val global_currency_code: String,
    val grand_total: Double,
    val items: List<Item>,
    val order_currency_code: String,
    val payment: Payment,
    val shipping_amount: Double,
    val shipping_description: String,
    val shipping_discount_amount: Double,
    val shipping_discount_tax_compensation_amount: Double,
    val shipping_incl_tax: Double,
    val shipping_tax_amount: Double,
    val state: String,
    val status: String,
    val store_currency_code: String,
    val store_id: Int,
    val store_to_base_rate: Double,
    val store_to_order_rate: Double,
    val subtotal: Double,
    val subtotal_incl_tax: Double,
    val tax_amount: Double,
    val total_item_count: Int,
    val total_qty_ordered: Int
)

class ExtensionAttributes(
    val converting_from_quote: Int,
    val shipping_assignments: List<ShippingAssignment>
)

class Payment(
    val amount_authorized: Double,
    val amount_ordered: Double,
    val amount_paid: Double,
    val base_amount_authorized: Double,
    val base_amount_ordered: Double,
    val base_amount_paid: Double,
    val base_shipping_amount: Double,
    val method: String,
    val po_number: String,
    val shipping_amount: Double
)

class ShippingAssignment(
    val items: List<Item>,
    val shipping: Shipping
)

class Item(
    val base_original_price: Double,
    val base_price: Double,
    val base_price_incl_tax: Double,
    val base_row_total: Double,
    val base_row_total_incl_tax: Double,
    val base_tax_amount: Double,
    val name: String,
    val original_price: Double,
    val price: Double,
    val price_incl_tax: Double,
    val product_id: Int,
    val product_type: String,
    val qty_ordered: Int,
    val row_total: Double,
    val row_total_incl_tax: Double,
    val sku: String,
    val store_id: Int,
    val tax_amount: Double,
    val tax_percent: Double
)

class Shipping(
    val address: Address,
    val method: String,
    val total: Total
)

class Address(
    val address_type: String,
    val city: String,
    val company: String,
    val country_id: String,
    val email: String,
    val fax: String,
    val firstname: String,
    val lastname: String,
    val middlename: String,
    val postcode: String,
    val region: String,
    val region_code: String,
    val region_id: Int,
    val street: List<String>,
    val telephone: String
)

class Total(
    val base_shipping_amount: Double,
    val base_shipping_discount_amount: Double,
    val base_shipping_discount_tax_compensation_amnt: Double,
    val base_shipping_incl_tax: Double,
    val base_shipping_tax_amount: Double,
    val shipping_amount: Double,
    val shipping_discount_amount: Double,
    val shipping_discount_tax_compensation_amount: Double,
    val shipping_incl_tax: Double,
    val shipping_tax_amount: Double
)