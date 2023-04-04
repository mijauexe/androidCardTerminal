package com.card.terminal.log

import fr.bipi.tressence.common.formatter.Formatter
import fr.bipi.tressence.common.time.TimeStamper

class CustomLogFormatter : Formatter {
    override fun format(priority: Int, tag: String?, message: String): String {
        val timestamper = TimeStamper("MM-dd HH:mm:ss:SS")
        return timestamper.getCurrentTimeStamp(System.currentTimeMillis()) + " | " + message + "\n"
    }
}
