package uk.co.firstchoice_cs.core.api

import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.core.api.magentoAPI.Addresse
import uk.co.firstchoice_cs.core.api.magentoAPI.CustomAttribute
import uk.co.firstchoice_cs.core.api.magentoAPI.Region


class AddressManager {

    enum class AddressTypes {
        BILLING,
        SHIPPING
    }

    data class Address(
            val city: String?,
            val company: String?,
            val country_id: String?,
            val custom_attributes: List<CustomAttribute>?,
            val customer_id: Int?,
            var default_billing: Boolean?,
            var default_shipping: Boolean?,
            var selected_billing: Boolean,
            var selected_shipping: Boolean,
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
            val email: String?,
    )


    private var customerAddresses = ArrayList<uk.co.firstchoice_cs.core.api.customerAPI.Addresse>()
    private var magentoAddresses = ArrayList<Addresse>()
    private var addedAddresses = ArrayList<AddedAddress>()
    var mergedAddresses = ArrayList<Address>()

    fun addMagentoAddresses(mAddresses:List<Addresse>)
    {
        magentoAddresses.clear()
        magentoAddresses.addAll(mAddresses)
        merge()
    }

    fun addCustomerAddresses(cAddresses:List<uk.co.firstchoice_cs.core.api.customerAPI.Addresse>?)
    {
        customerAddresses.clear()
        if (cAddresses != null) {
            customerAddresses.addAll(cAddresses)
        }
        merge()
    }
    fun clear()
    {
        customerAddresses.clear()
        magentoAddresses.clear()
        mergedAddresses.clear()
    }

    private fun merge()
    {
        mergedAddresses.clear()
        for (address in magentoAddresses)
        {
            val mergedAddress = addMagentoAddress(address)
            if(!containsAddress(mergedAddress))
                mergedAddresses.add(mergedAddress)
        }
        for (address in customerAddresses)
        {
            val mergedAddress = addCustomerAddress(address)
            if(!containsAddress(mergedAddress))
                mergedAddresses.add(mergedAddress)
        }
        for (address in addedAddresses)
        {
            val mergedAddress = addAddedAddress(address)
            if(!containsAddress(mergedAddress))
                mergedAddresses.add(mergedAddress)
        }
    }

    private fun addAddedAddress(address: AddedAddress): Address {
        val street = ArrayList<String>()
        street.add(address.addressLine1.toString())
        street.add(address.addressLine2.toString())
        val customAttributes = ArrayList<CustomAttribute>()
        val region = Region(address.city ?: "", "", -1)

        return Address(
            city = address.city,
            company = "",
            country_id = App.customer?.taxRegionCode,
            custom_attributes = customAttributes,
            customer_id = App.customer?.cust,
            default_billing = false,
            default_shipping = false,
            selected_billing = false,
            selected_shipping = false,
            firstname = address.firstname.toString(),
            id = -1,
            lastname = address.lastname.toString(),
            middlename = "",
            postcode = address.postcode.toString(),
            region = region,
            region_id = -1,
            street = street,
            telephone = address.telephone.toString(),
            fax = address.fax.toString(),
            email = address.email.toString())
    }


    private fun containsAddress(a:Address):Boolean
    {
        for (address in mergedAddresses)
        {
            if(address.id == -1 || a.id == -1) {
                if (address.postcode == a.postcode)
                    return true
            }
        }
        return false
    }

    data class AddedAddress(
        val city: String?,
        val company: String?,
        val country_id: String?,
        var default_billing: Boolean?,
        var default_shipping: Boolean?,
        var selected_billing: Boolean,
        var selected_shipping: Boolean,
        val firstname: String?,
        val lastname: String?,
        val middlename: String?,
        val postcode: String?,
        val addressLine1: String?,
        val addressLine2: String?,
        val telephone: String?,
        val fax: String?,
        val email: String?,
    )


    fun addAddress(address: AddedAddress):Address
    {
        addedAddresses.add(address)
        val res = addAddedAddress(address)
        merge()
        return res
    }

    private fun addCustomerAddress(address: uk.co.firstchoice_cs.core.api.customerAPI.Addresse): Address {
        val street = ArrayList<String>()
        val customAttributes = ArrayList<CustomAttribute>()
        val region = Region(address.county ?: "", "", -1)

        return Address(
                city = address.city,
                company = "",
                country_id = App.customer?.taxRegionCode,
                custom_attributes = customAttributes,
                customer_id = App.customer?.cust,
                default_billing = false,
                default_shipping = false,
                selected_billing = false,
                selected_shipping = false,
                firstname = address.name,
                id = -1,
                lastname = "",
                middlename = "",
                postcode = address.postcode,
                region = region,
                region_id = -1,
                street = street,
                telephone = address.telephoneNum,
                fax = address.fax,
                email = address.email
            )

    }



    private fun addMagentoAddress(address:Addresse):Address
    {
        var isDefaultBilling = false
        var isDefaultShipping = false
        val defaultBilling = App.magentoCustomer?.default_billing
        val defaultShipping = App.magentoCustomer?.default_shipping

        if(!defaultBilling.isNullOrBlank()&&defaultBilling.toInt()==address.id)
            isDefaultBilling = true
        if(!defaultShipping.isNullOrBlank()&&defaultShipping.toInt()==address.id)
            isDefaultShipping = true

        return Address(
                city = address.city,
                company = address.company,
                country_id = address.country_id,
                custom_attributes = address.custom_attributes,
                customer_id = App.customer?.cust,
                default_billing = isDefaultBilling,
                default_shipping = isDefaultShipping,
                selected_billing = isDefaultBilling,
                selected_shipping = isDefaultShipping,
                firstname = address.firstname,
                id = address.id,
                lastname = address.lastname,
                middlename = address.middlename,
                postcode = address.postcode,
                region = address.region,
                region_id = address.region_id,
                street = address.street,
                telephone = address.telephone,
            fax = address.fax,
            email = address.email,
            )
    }




    fun setDefaultBillingAddress(address:Address)
    {
        for (add in mergedAddresses) {
            add.default_billing = false
        }
        address.default_billing = true
    }

    fun setDefaultDeliveryAddress(address:Address)
    {
        for (add in mergedAddresses) {
            add.default_shipping = false
        }
        address.default_shipping = true
    }

    fun setSelectedBillingAddress(address:Address)
    {
        for (add in mergedAddresses) {
            add.selected_billing = false
        }
        address.selected_billing = true
    }

    fun setSelectedShippingAddress(address:Address)
    {
        for (add in mergedAddresses) {
            add.selected_shipping = false
        }
        address.selected_shipping = true
    }

    fun getSelectedBillingAddress(): Address? {
        for (address in mergedAddresses) {
            if (address.selected_billing)
                return address
        }
        return if(mergedAddresses.isEmpty())
            null
        else
            mergedAddresses[0]
    }

    fun getSelectedShippingAddress(): Address? {
        for (address in mergedAddresses) {
            if (address.selected_shipping)
                return address
        }
        return if(mergedAddresses.isEmpty())
            null
        else
            mergedAddresses[0]
    }

    fun getDefaultBillingAddress(): Address? {
        for (address in mergedAddresses) {
            if (address.default_billing == true)
                return address
        }
        return if(mergedAddresses.isEmpty())
            null
        else
            mergedAddresses[0]
    }

    fun getDefaultShippingAddress(): Address? {
        for (address in mergedAddresses) {
            if (address.default_shipping == true)
                return address
        }
        return if(mergedAddresses.isEmpty())
            null
        else
            mergedAddresses[0]
    }
}