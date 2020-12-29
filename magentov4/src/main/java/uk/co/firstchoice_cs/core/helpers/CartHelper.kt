package uk.co.firstchoice_cs.core.helpers

import uk.co.firstchoice_cs.App
import uk.co.firstchoice_cs.Constants.PAYMENT_TYPE_CARD
import uk.co.firstchoice_cs.core.api.customerAPI.Product
import uk.co.firstchoice_cs.core.api.customerAPI.ShipVia
import uk.co.firstchoice_cs.core.api.magentoAPI.Customer
import uk.co.firstchoice_cs.core.api.v4API.Part
import uk.co.firstchoice_cs.core.database.cart.CartItem
import java.text.DecimalFormat

object CartHelper {

    @JvmStatic
    fun cartItemFromPart(jsonStr: String, product: Part): CartItem {
        return CartItem(jsonStr, product.partDescription, product.manufacturer, product.partNum, product.fccPart, product.stock, product.classDescription, 1)
    }

    fun customerCostIncVat(price:Product): Double?
    {
        return price.customerCost?.plus((price.customerCost *  getTaxRate()))
    }

    fun customerExtendedCostIncVat(price:Product): Double?
    {
        return price.customerExtendedCost?.plus((price.customerExtendedCost *  getTaxRate()))
    }

    @JvmStatic
    fun getSelectedPaymentType(b2b:Boolean): String?
    {
        return if(b2b)
            PAYMENT_TYPE_CARD
        else {
            if(App.selectedPaymentType.isBlank())
                null
            else
                App.selectedPaymentType
        }
    }

    @JvmStatic
    fun getSelectedShippingMethod(): ShipVia?
    {
        val sv = App.customer?.shipVias
        if (sv != null && sv.isNotEmpty()) {
            for(shipMethod in sv) {
                if(shipMethod.selected)
                    return shipMethod
            }
            return null
        }
        return null
    }

    fun priceWithTax(basePrice:Double):Double
    {
        if(basePrice==0.0)
            return basePrice
        val tax = getTaxRate()
        return basePrice.plus((basePrice * tax))
    }


    fun basketTotalIncVat(items: List<CartItem>?): Double {
        var incVat = 0.0
        if (items != null) {
            for (item in items) {
                incVat = incVat.plus(itemPriceIncVAT(item))
            }
        }
        return incVat
    }

    private fun itemPriceIncVAT(item: CartItem): Double {
        var incVat = 0.0
        val tax = getTaxRate()
        val qty = item.qty?:1
        val price = item.fccPart?.let { App.globalData.getPriceStockFromMap(item.fccPart,qty)}
        if (price?.customerExtendedCost != null) {
            incVat = price.customerExtendedCost.times(tax).plus(price.customerExtendedCost)
        }
        return incVat
    }


    fun basketTotalExVat(items: List<CartItem>?): Double {
        var exVat = 0.0
        if (items != null) {
            for (item in items) {
                val qty = item.qty?:1
                val price = item.fccPart?.let { App.globalData.getPriceStockFromMap(it,qty) }
                if (price?.customerExtendedCost != null) {
                    exVat += price.customerExtendedCost
                }
            }
        }
        return exVat
    }

    fun isB2B(customer: Customer?): Boolean {
        val addresses = customer?.addresses
        if (addresses != null) {
            for (address in addresses) {
                for (attr in address.custom_attributes!!) {
                    if (attr.attribute_code == "ecc_erp_group_code")
                        return true
                }
            }
        }
        return false
    }


    fun customerFullName(customer: Customer): String {
        return customer.firstname + " " + customer.lastname
    }

    fun customerInitials(customer: Customer): String {
        return customer.firstname.first() + "" + customer.lastname.first()
    }

    fun getPosition(updatedItem: CartItem, items: List<CartItem>?): Int {
        if (items != null) {
            for (item in items) {
                if (item.partNum == updatedItem.partNum) {
                    return items.indexOf(item)
                }
            }
        }
        return -1
    }

    fun getTaxRate(): Double {
        return ( App.customer?.taxRate ?: return 0.0) / 100.0
    }

    private fun getCurrencySymbol():String{
        return "£"
    }

    fun getCurrencyFormatter(): DecimalFormat {
        return DecimalFormat("'" +  getCurrencySymbol() + "'0.00")
    }

    fun numberOfBasketItems(items: List<CartItem>?): Int {
        return items?.size ?: 0
    }
}