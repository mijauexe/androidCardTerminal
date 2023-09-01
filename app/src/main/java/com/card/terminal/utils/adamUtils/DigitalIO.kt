package com.card.terminal.utils.adamUtils

import com.card.terminal.utils.adamUtils.Adam6050D.Companion.DO_COUNT
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.EOFException
import java.io.IOException
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

@OptIn(ExperimentalStdlibApi::class)
class DigitalOutput(quantity: Int = 6, array: List<Int>? = null, xmlString: String? = null) {

    private val digitalOutput: MutableMap<String, Int?>
    private val TAG = "DigitalOutput"

    init {
        digitalOutput = if (xmlString != null) {
            parse(xmlString)
        } else {
            (0..<quantity).associate { "DO$it" to null }.toMutableMap()
        }
        array?.let { array(it) }
    }

    fun parse(xmlString: String): MutableMap<String, Int?> {
        var idList = mutableListOf<String>()
        var valueList = mutableListOf<Int>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xmlString)) // pass input whatever xml you have
            var eventType = xpp.eventType
            var status = ""

            var next = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                } else if (eventType == XmlPullParser.START_TAG) {

                    if (xpp.name == "ADAM-6050") {
                        status = xpp.getAttributeValue(null, "status")
                        if (status != "OK") throw AdamException("status=$status")
                    } else if (xpp.name == "ID") {
                        next = "ID"
                    } else if (xpp.name == "VALUE") {
                        next = "VALUE"
                    }

                } else if (eventType == XmlPullParser.END_TAG) {
                } else if (eventType == XmlPullParser.TEXT) {
                    if (next == "ID") {
                        idList.add("DO" + xpp.text)
                    } else if (next == "VALUE") {
                        valueList.add(xpp.text.toInt())
                    }
                    next = ""
                }
                try {
                    eventType = xpp.next();
                } catch (e : EOFException) {
                    break
                }
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return mutableMapOf<String, Int?>().apply {
            idList.zip(valueList).forEach { (key, value) -> put(key, value) }
        }
    }

    operator fun set(doId: Int, value: Int) {
        if (value !in listOf(0, 1)) {
            throw IllegalArgumentException("digital output only accepts integer 0 or 1")
        }
        if (doId !in 0..DO_COUNT-1) {
            throw IllegalArgumentException("digital output id should be in the range of 0 to ${DO_COUNT-1}")
        }
        digitalOutput["DO$doId"] = value
    }

    fun array(array: List<Int?>) {
        require(array.size == digitalOutput.size) { "quantity and initial array sizes are different for digital output" }
        array.forEachIndexed { index, value ->
            digitalOutput["DO$index"] = value
        }
    }

    fun clear() {
        array(List(digitalOutput.size) { null })
    }

    fun asDict(): Map<String, Int> {
        return digitalOutput.filterValues { it != null }.mapValues { it.value!! }
    }

    operator fun invoke(): Map<String, Int> {
        return asDict()
    }

    operator fun get(doId: Int): Int? {
        return digitalOutput["DO$doId"]
    }

    operator fun iterator(): Iterator<Map.Entry<String, Int?>> {
        return digitalOutput.entries.iterator()
    }

    override fun toString(): String {
        return digitalOutput.entries.joinToString("\n") { (key, value) -> "DO[$key]=$value" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DigitalOutput) return false
        if (digitalOutput != other.digitalOutput) return false
        return true
    }

    override fun hashCode(): Int {
        return digitalOutput.hashCode()
    }
}

class DigitalInput(xmlString: String) {
    private val di: MutableMap<String, Int> = mutableMapOf()

    init {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val inputSource = InputSource(StringReader(xmlString))
        val document = builder.parse(inputSource)
        val root = document.documentElement
        val status = root.getAttribute("status")
        if (status != "OK") {
            throw Exception("something wrong with the response, status is: $status")
        }

        val diNodes: NodeList = root.getElementsByTagName("DI")
        for (i in 0 until diNodes.length) {
            val diElement = diNodes.item(i) as Element
            val id = diElement.getElementsByTagName("ID").item(0).textContent
            val value = diElement.getElementsByTagName("VALUE").item(0).textContent.toInt()
            di["DI$id"] = value
        }
    }

    operator fun get(diId: Int): Int {
        if (diId !is Int) {
            throw IllegalArgumentException("digital input id should be integer")
        }
        return di["DI$diId"] ?: throw NoSuchElementException("Digital input not found")
    }

    operator fun iterator(): Iterator<Map.Entry<String, Int>> {
        return di.entries.iterator()
    }

    override fun toString(): String {
        return di.entries.joinToString("\n") { (key, value) -> "DI[$key]=$value" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DigitalInput) return false
        if (di != other.di) return false
        return true
    }

    override fun hashCode(): Int {
        return di.hashCode()
    }
}
