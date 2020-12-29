package uk.co.firstchoice_cs.adyen.data.api

data class ResultData(
    val additionalData: AdditionalData,
    val amount: Amount,
    val merchantReference: String,
    val pspReference: String,
    val resultCode: String
)

data class AdditionalData(
    val cardBin: String,
    val cardIssuingCountry: String,
    val cardPaymentMethod: String,
    val cardSummary: String,
    val fundingSource: String,
    val recurringProcessingModel: String
)

data class Amount(
    val currency: String,
    val value: Int
)