package uk.co.firstchoice_cs.core.helpers

import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Constants
import uk.co.firstchoice_cs.basket.*
import uk.co.firstchoice_cs.core.api.AddressManager
import uk.co.firstchoice_cs.core.api.customerAPI.ShipVia
import uk.co.firstchoice_cs.core.database.cart.CartItem


object CheckoutHelper {
    @JvmStatic
    fun buildExtensionAttributes(shippingAssignments: List<ShippingAssignment>): ExtensionAttributes {
        return ExtensionAttributes(
            converting_from_quote = 0,
            shipping_assignments = shippingAssignments
        )
    }

    @JvmStatic
    fun buildPayment(
        baseGrandTotal: Double,
        baseSubTotal: Double,
        baseShippingAmount: Double,
        pONumber:String
    ): Payment {
        return Payment(
            amount_authorized = baseGrandTotal,
            amount_ordered = baseGrandTotal,
            amount_paid = baseGrandTotal,
            base_amount_authorized = baseGrandTotal,
            base_amount_ordered = baseSubTotal,
            base_amount_paid = 0.0,
            base_shipping_amount = baseShippingAmount,
            method = if (App.selectedPaymentType == Constants.PAYMENT_TYPE_ACCOUNT) "pay" else "adyen_cc",
            po_number = pONumber,
            shipping_amount = baseShippingAmount
        )
    }
    @JvmStatic
    fun buildShipping(
        shippingAddress: Address,
        flatRateCode: String?,
        shippingTotal: Total
    ): Shipping {
        return Shipping(
            address = shippingAddress,
            method = flatRateCode ?: "",
            total = shippingTotal
        )
    }

    @JvmStatic
    fun buildShippingTotal(
        baseShippingAmount: Double,
        baseShippingInclTax: Double,
        baseShippingTaxAmount: Double
    ): Total {
        return Total(
            base_shipping_amount = baseShippingAmount,
            base_shipping_discount_amount = 0.0,
            base_shipping_discount_tax_compensation_amnt = 0.0,
            base_shipping_incl_tax = baseShippingInclTax,
            base_shipping_tax_amount = baseShippingTaxAmount,
            shipping_amount = baseShippingAmount,
            shipping_discount_amount = 0.0,
            shipping_discount_tax_compensation_amount = 0.0,
            shipping_incl_tax = baseShippingInclTax,
            shipping_tax_amount = baseShippingTaxAmount
        )
    }

    @JvmStatic
    fun buildItem(
        originalPrice: Double,
        baseSubTotal: Double,
        baseRowTotalIncTax: Double,
        basePrice: Double,
        baseTaxAmount: Double,
        cartItem: CartItem,
        baseSubTotalIncVat: Double,
        baseItemTaxAmount: Double
    ): Item {
        return Item(
            base_original_price = originalPrice,
            base_price = baseSubTotal,
            base_price_incl_tax = baseRowTotalIncTax,
            base_row_total = basePrice,
            base_row_total_incl_tax = baseRowTotalIncTax,
            base_tax_amount = baseTaxAmount,
            name = cartItem.partDescription.toString(),
            original_price = originalPrice,
            price = basePrice,
            price_incl_tax = baseSubTotalIncVat,
            product_id = 1,
            product_type = "simple",
            qty_ordered = cartItem.qty ?: 1,
            row_total = basePrice,
            row_total_incl_tax = baseSubTotalIncVat,
            sku = cartItem.partNum.toString(),
            store_id = 1,
            tax_amount = baseItemTaxAmount,
            tax_percent = CartHelper.getTaxRate()
        )
    }

    @JvmStatic
    fun buildEntity(
        baseGrandTotal: Double,
        baseShippingAmount: Double,
        baseShippingInclTax: Double,
        baseShippingTaxAmount: Double,
        baseSubTotal: Double,
        baseSubTotalIncVat: Double,
        billingAddress: Address,
        email: String,
        firstName: String,
        customerId: Int?,
        lastName: String,
        middleName: String,
        extensionAttributes: ExtensionAttributes,
        items: MutableList<Item>,
        payment: Payment,
        selectedShippingMethod: ShipVia?,
        baseTaxAmount: Double
    ): Entity {
        return Entity(
            base_currency_code = "GBP",//Should always be 'GBP'
            base_discount_amount = 0.0,//0
            base_discount_tax_compensation_amount = 0.0,//0
            base_grand_total = baseGrandTotal,
            base_shipping_amount = baseShippingAmount,
            base_shipping_incl_tax = baseShippingInclTax,
            base_shipping_tax_amount = baseShippingTaxAmount,
            base_subtotal = baseSubTotal,
            base_subtotal_incl_tax = baseSubTotalIncVat,
            base_tax_amount = 14.1,
            base_to_global_rate = 1.0,
            base_to_order_rate = 1.0,
            base_total_due = baseGrandTotal,
            base_total_paid = baseGrandTotal,
            billing_address = billingAddress,
            customer_email = email,
            customer_firstname = firstName,
            customer_id = customerId ?: 0,
            customer_is_guest = 0,
            customer_lastname = lastName,
            customer_middlename = middleName,
            discount_amount = 0.0,
            discount_tax_compensation_amount = 0.0,
            extension_attributes = extensionAttributes,
            global_currency_code = "GBP",
            grand_total = baseGrandTotal,
            items = items,
            order_currency_code = "GBP",
            payment = payment,
            shipping_amount = baseShippingInclTax,
            shipping_description = selectedShippingMethod?.description.toString(),
            shipping_discount_amount = 0.0,
            shipping_discount_tax_compensation_amount = 0.0,
            shipping_incl_tax = baseShippingInclTax,
            shipping_tax_amount = 2.5,
            state = "processing",
            status = "processing",
            store_currency_code = "GBP",
            store_id = 1,
            store_to_base_rate = 0.0,
            store_to_order_rate = 0.0,
            subtotal = baseSubTotal,
            subtotal_incl_tax = baseSubTotalIncVat,
            tax_amount = baseTaxAmount,
            total_item_count = items.size,
            total_qty_ordered = items.size
        )
    }


    @JvmStatic
    fun getSelectedShippingMethod(): ShipVia? {
        val sv = App.customer?.shipVias
        if (sv != null) {
            for (ship in sv) {
                if (ship.selected)
                    return ship
            }
        }
        return null
    }
    @JvmStatic
    fun getEmail():String
    {
        val mag =  App.magentoCustomer
        return if (mag == null) {
            val billingAddress = App.addresses.getSelectedBillingAddress()
            billingAddress?.email.toString()
        } else {
            mag.email
        }
    }
    @JvmStatic
    fun getFirstName():String
    {
        val mag =  App.magentoCustomer
        return if (mag == null) {
            val billingAddress = App.addresses.getSelectedBillingAddress()
            billingAddress?.firstname.toString()
        } else {
            mag.firstname
        }
    }
    @JvmStatic
    fun getLastName():String
    {
        val mag =  App.magentoCustomer
        return if (mag == null) {
            val billingAddress = App.addresses.getSelectedBillingAddress()
            billingAddress?.lastname.toString()
        } else {
            mag.lastname
        }
    }
    @JvmStatic
    fun getMiddleName():String
    {
        val mag =  App.magentoCustomer
        return if (mag == null) {
            val billingAddress = App.addresses.getSelectedBillingAddress()
            billingAddress?.middlename.toString()
        } else {
            mag.middlename
        }
    }
    @JvmStatic
    fun getCustomerID():Int?
    {
        return App.magentoCustomer?.id
    }

    fun shippingAddress(selectedShippingAddress: AddressManager.Address?): Address {
        return Address(
            address_type = "shipping",
            city = selectedShippingAddress?.city.toString(),
            company = selectedShippingAddress?.company.toString(),
            country_id = "GB",
            email = selectedShippingAddress?.email.toString(),
            fax = selectedShippingAddress?.fax.toString(),
            firstname = selectedShippingAddress?.firstname.toString(),
            lastname = selectedShippingAddress?.lastname.toString(),
            middlename = selectedShippingAddress?.middlename.toString(),
            postcode = selectedShippingAddress?.postcode.toString(),
            region = selectedShippingAddress?.region?.region.toString(),
            region_code = selectedShippingAddress?.region?.region_code.toString(),
            region_id = selectedShippingAddress?.region?.region_id ?: 0,
            street = selectedShippingAddress?.street ?: listOf(),
            telephone = selectedShippingAddress?.telephone.toString(),
        )
    }

    fun buildBillingAddress(selectedBillingAddress: AddressManager.Address?): Address {
        return Address(
            address_type = "billing",
            city = selectedBillingAddress?.city.toString(),
            company = selectedBillingAddress?.company.toString(),
            country_id = "GB",
            email = selectedBillingAddress?.email.toString(),
            fax = selectedBillingAddress?.fax.toString(),
            firstname = selectedBillingAddress?.firstname.toString(),
            lastname = selectedBillingAddress?.lastname.toString(),
            middlename = selectedBillingAddress?.middlename.toString(),
            postcode = selectedBillingAddress?.postcode.toString(),
            region = selectedBillingAddress?.region?.region.toString(),
            region_code = selectedBillingAddress?.region?.region_code.toString(),
            region_id = selectedBillingAddress?.region?.region_id?:0,
            street = selectedBillingAddress?.street?: listOf(),
            telephone = selectedBillingAddress?.telephone.toString(),
        )
    }

    fun buildShippingAssignments(shipping: Shipping, items: MutableList<Item>): List<ShippingAssignment> {
        val shippingAssignment = ShippingAssignment(
            shipping = shipping,
            items = items
        )
        val shippingAssignments = mutableListOf<ShippingAssignment>()
        shippingAssignments.add(shippingAssignment)
        return shippingAssignments
    }
}