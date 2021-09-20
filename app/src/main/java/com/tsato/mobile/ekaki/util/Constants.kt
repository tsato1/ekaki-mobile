package com.tsato.mobile.ekaki.util

object Constants {

    const val USE_LOCALHOST = true
    const val HTTP_BASE_URL = ""
    const val HTTP_BASE_URL_LOCALHOST = "http://10.0.2.2:8001/"
        /*
            use this when refer to from our emulator to the ip address of our computer
            if we use http://localhost, it won't work from emulator

            if we want to use our phone, instead of emulators, use the ipv4 address from ipconfig
         */

    const val MIN_USERNAME_LENGTH = 4
    const val MAX_USERNAME_LENGTH = 12

    const val MIN_ROOMNAME_LENGTH = 4
    const val MAX_ROOMNAME_LENGTH = 16
}