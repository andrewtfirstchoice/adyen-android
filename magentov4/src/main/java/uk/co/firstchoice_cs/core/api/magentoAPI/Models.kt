package uk.co.firstchoice_cs.core.api.magentoAPI

data class Customer(
        val addresses: List<Addresse>,
        val created_at: String,
        val created_in: String,
        val custom_attributes: List<CustomAttribute>,
        val default_billing: String,
        val default_shipping: String,
        val disable_auto_group_change: Int,
        val email: String,
        val extension_attributes: ExtensionAttributes,
        val firstname: String,
        val group_id: Int,
        val id: Int,
        val lastname: String,
        val middlename: String,
        val store_id: Int,
        val updated_at: String,
        val website_id: Int
)

fun getECCGroupCode(customer: Customer?): Int {
        if (customer == null) return -1
        if (customer.addresses.isEmpty()) return -1
        val firstAddress = customer.addresses[0]
        for (attr in firstAddress.custom_attributes!!) {
                if (attr.attribute_code == "ecc_erp_group_code") {
                        val split = attr.value.split('~')
                        return if(split.size > 1)
                                split[1].toInt()
                        else
                                -1
                }
        }
        return -1
}

data class Addresse(
        val city: String?,
        val company: String?,
        val country_id: String?,
        val custom_attributes: List<CustomAttribute>?,
        val customer_id: Int?,
        val default_billing: Boolean?,
        val default_shipping: Boolean?,
        val firstname: String?,
        val id: Int?,
        val lastname: String?,
        val middlename: String?,
        val postcode: String?,
        val region: Region?,
        val region_id: Int?,
        val street: List<String>?,
        val telephone: String?,
        val fax: String?,
        val email: String?
)

data class ExtensionAttributes(
        val is_subscribed: Boolean
)

data class CustomAttribute(
        val attribute_code: String,
        val value: String
)

data class Region(
        val region: String,
        val region_code: String,
        val region_id: Int
)