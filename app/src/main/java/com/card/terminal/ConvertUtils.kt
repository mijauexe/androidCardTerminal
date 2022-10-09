package com.card.terminal

import kotlin.math.pow

object ConvertUtils {
    fun byteArrayToString(array: ByteArray, shift: Int = 0): String {
        val hex = StringBuilder()
        for (i in 0 until array.size - shift) {
            hex.append(String.format("%02X", array[i]))
        }
        return hex.toString()
    }

    fun convertBinaryToDecimal(bin: Long): Int {
        var num = bin
        var decimalNumber = 0
        var i = 0
        var remainder: Long

        while (num.toInt() != 0) {
            remainder = num % 10
            num /= 10
            decimalNumber += (remainder * 2.0.pow(i.toDouble())).toInt()
            ++i
        }
        return decimalNumber
    }

    fun hexStringToAscii(hexStr: String): String {
        val output = StringBuilder("")
        var i = 0
        while (i < hexStr.length) {
            val str = hexStr.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    fun hexStringToByteArray(s: String): ByteArray {
        val b = ByteArray(s.length / 2)
        for (i in b.indices) {
            val index = i * 2
            val v = s.substring(index, index + 2).toInt(16)
            b[i] = v.toByte()
        }
        return b
    }
}