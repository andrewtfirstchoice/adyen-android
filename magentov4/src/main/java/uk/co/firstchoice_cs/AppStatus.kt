package uk.co.firstchoice_cs

object AppStatus {
    @JvmField
    var INTERNET_CONNECTED = true //I set this to true initially to not show the internet warning on the home view which shows up briefly if not
    @JvmField
    var INTERNET_CONNECTED_MOBILE = false
    @JvmField
    var INTERNET_CONNECTED_WIFI = false
}