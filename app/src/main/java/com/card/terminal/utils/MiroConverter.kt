package com.card.terminal.utils

import android.os.Bundle

class MiroConverter {

    fun convertToPOSTFormat(cardResponse: Bundle): String {
        var eCode = -1
        when (cardResponse.get("selection")) {
            "BE-TO" -> eCode = 0
            "Liječnik" -> eCode = 3
            "Privatno" -> eCode = 4
            "Pauza" -> eCode = 5
            "Poslovno" -> eCode = 6
        }

        val str = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"hep1\"," +
                "\"CREAD\":[" +
                "{\"CN\":\"${cardResponse.get("CardCode")}\", \"GENT\":\"${
                    cardResponse.get(
                        "DateTime"
                    )
                }\", \"ECODE\":\"${eCode}\", \"DEV_B0_ID\":\"${
                    cardResponse.get(
                        "userId"
                    )
                }\"}" +
                "]" +
                "}"

        print(str)
        return str
    }

    fun convertECode(s: String): Int {
        var eCode = -1
        when (s) {
            "BE-TO" -> eCode = 0
            "Liječnik" -> eCode = 3
            "Privatno" -> eCode = 4
            "Pauza" -> eCode = 5
            "Poslovno" -> eCode = 6
        }
        return eCode
    }


}