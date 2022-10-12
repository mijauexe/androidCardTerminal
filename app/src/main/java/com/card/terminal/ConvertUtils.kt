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

    fun hexStringToBinaryString(hexStr: String): String {
        val output = StringBuilder("")
        var i = 0
        while (i < hexStr.length) {
            when (hexStr[i]) {
                '0' -> output.append("0000")
                '1' -> output.append("0001")
                '2' -> output.append("0010")
                '3' -> output.append("0011")
                '4' -> output.append("0100")
                '5' -> output.append("0101")
                '6' -> output.append("0110")
                '7' -> output.append("0111")
                '8' -> output.append("1000")
                '9' -> output.append("1001")
                'A', 'a' -> output.append("1010")
                'B', 'b' -> output.append("1011")
                'C', 'c' -> output.append("1100")
                'D', 'd' -> output.append("1101")
                'E', 'e' -> output.append("1110")
                'F', 'f' -> output.append("1111")
            }
            i++
        }
        return output.toString()
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

    fun hexStringToHexArray(s: String): List<String> {
        val b = mutableListOf<String>()
        for (i in 0..s.length - 1) {
            if (i % 2 != 0) {
                b.add(s[i - 1].toString() + s[i].toString())
            }
        }
        return b
    }


    fun hexArrayToHexString(list: List<String>): String {
        var str = ""
        for (i in 0..list.size) {
            str += list[i]
        }
        return str
    }
}