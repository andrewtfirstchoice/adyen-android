/*
 * Copyright (c) 2019 Adyen N.V.
 *
 * This file is open source and available under the MIT license. See the LICENSE file for more info.
 *
 * Created by arman on 9/10/2019.
 */

package uk.co.firstchoice_cs.adyen.di

import org.koin.dsl.module
import uk.co.firstchoice_cs.adyen.repositories.paymentMethods.PaymentsRepository
import uk.co.firstchoice_cs.adyen.repositories.paymentMethods.PaymentsRepositoryImpl

val repositoryModule = module {
    factory<PaymentsRepository> { PaymentsRepositoryImpl(get()) }
}
