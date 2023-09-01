package com.card.terminal.utils.adamUtils

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.EOFException
import java.io.IOException
import java.io.StringReader

class Adam6050D(ip: String, username: String, password: String) {
    companion object {
        const val DO_COUNT = 6
        const val DI_COUNT = 12
    }

    private val requestor: Requestor = Requestor(ip, username, password)

    fun output(digitalOutput: DigitalOutput? = null): Any {
            if (digitalOutput != null) {
            val currentState = requestor.output()
                if(currentState == "")
                    throw AdamException("nema konekšna")
            println(currentState)
            val currentDO = DigitalOutput(xmlString = currentState)
            for ((key, value) in digitalOutput.asDict()) {
                println("$key -> $value")
                val intKey = key.replace("DO", "").toInt()
                if (value != null) {
                    currentDO[intKey] = value
                }
            }

            val response = requestor.output(currentDO.asDict())

            try {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(StringReader(response)) // pass input whatever xml you have
                var eventType = xpp.eventType

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        if (xpp.name == "ADAM-6050") {
                            val status = xpp.getAttributeValue(null, "status")
                            if (status != "OK") throw AdamException("status=$status") //TODO CHECK THIS SHIT
                            else break
                        }
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
            return true
        } else {
            val response = requestor.output(digitalOutput)
            return DigitalOutput(xmlString = response)
        }
    }

    fun input(digitalInputId: Int? = null): DigitalInput {
        val response = requestor.input(digitalInputId)
        return DigitalInput(response)
    }

    fun on(): Any {
        val doArray = List(DO_COUNT) { 1 }
        val digitalOutput = DigitalOutput(array = doArray)
        return output(digitalOutput)
    }

    fun off(): Any {
        val doArray = List(DO_COUNT) { 1 }
        val digitalOutput = DigitalOutput(array = doArray)
        return output(digitalOutput)
    }
}