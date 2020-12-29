/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 10/10/2019.
 */

package uk.co.firstchoice_cs.adyen.data.api.model.paymentsRequest

import com.adyen.checkout.components.model.payments.Amount

@Suppress("MagicNumber")
data class PaymentMethodsRequest(
    val merchantAccount: String,
    val shopperReference: String,
    val amount: Amount,
    val countryCode: String = "GB",
    val shopperLocale: String = "en_US",
    val channel: String = "android",
    val telephoneNumber: String = "",
    val dateOfBirth: String = "",
    val shopperEmail: String = "",
    val shopperName: ShopperName = ShopperName(),
    val billingAddress: Address = Address(),
    val deliveryAddress: Address = Address()
) {
}

@SuppressWarnings("MemberName")
data class ShopperName(val firstName: String = "", val lastName: String = "", val gender: String = "")

@SuppressWarnings("MemberName")
data class Address(
    val country: String = "NL",
    val city: String = "Capital",
    val houseNumberOrName: String = "1",
    val postalCode: String = "1012 DJ",
    val stateOrProvince: String = "DC",
    val street: String = "Main St"
)
