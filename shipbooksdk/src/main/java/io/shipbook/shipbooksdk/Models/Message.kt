package io.shipbook.shipbooksdk.Models

import io.shipbook.shipbooksdk.Util.toInternal
import io.shipbook.shipbooksdk.Util.toJson
import io.shipbook.shipbooksdk.Util.toListStackTraceElement
import org.json.JSONObject
import java.util.*


/*
 *
 * Created by Elisha Sterngold on 11/02/2018.
 * Copyright © 2018 ShipBook Ltd. All rights reserved.
 *
 */

internal data class Message(val tag: String,
                            val severity: Severity,
                            val message: String,
                            val stackTrace: List<StackTraceElement>? = null,
                            val throwable: Throwable? = null,
                            var exception: MessageException? = null,
                            var function: String? = null,
                            var fileName: String? = null,
                            var lineNumber: Int? = null,
                            var className: String? = null,
                            //for base class
                            override var orderId: Int = 0,
                            override val time: Date = Date(),
                            override val threadInfo: ThreadInfo = ThreadInfo()): BaseLog("message"){
    companion object {
        fun create(json: JSONObject,
                   orderId: Int,
                   time: Date,
                   threadInfo: ThreadInfo): Message {
            val tag = json.getString("tag")
            val severity = Severity.valueOf(json.getString("severity"))
            val message = json.getString("message")
            val stackTrace = json.optJSONArray("stackTrace")?.toListStackTraceElement()
            val exception = if (json.has("exception")) MessageException.create(json.optJSONObject("exception")) else null
            val function = json.getString("function")
            val fileName = json.getString("fileName")
            val lineNumber = json.getInt("lineNumber")
            val className = json.getString("className")
            return Message(tag, severity, message, stackTrace, null, exception, function, fileName, lineNumber, className, orderId, time, threadInfo)
        }
    }

    init {
        orderId = incrementOrderId(orderId)
        if (fileName == null) {

            val thread = Thread.currentThread()
            val element = thread.stackTrace.firstOrNull {
                !it.className.startsWith("dalvik.") &&
                        !it.className.startsWith("java.") &&
                        !it.className.startsWith("io.shipbook.shipbooksdk")
            }

            function = element?.methodName
            fileName = element?.fileName
            lineNumber = element?.lineNumber
            className = element?.className
        }

        if (throwable != null) {
            val stackTrace = throwable.stackTrace.toInternal()
            exception = MessageException(throwable.javaClass.name, throwable.message, stackTrace)
        }

    }

    override fun toJson(): JSONObject {
        val json = super.toJson()
        json.put("tag", tag)
        json.put("severity", severity)
        json.put("message", message)
        json.putOpt("exception", exception?.toJson())
        json.put("function", function)
        json.put("fileName", fileName)
        json.put("lineNumber", lineNumber)
        json.put("className", className)

        return json
    }

    data class MessageException(val name: String?,
                                val reason: String?,
                                val stackTrace: List<StackTraceElement>) : BaseObj {
        companion object {
            fun create(json: JSONObject): MessageException? {
                val name = json.optString("name")
                val reason = json.optString("reason")
                val stackTrace = json.getJSONArray("stackTrace").toListStackTraceElement()
                return MessageException(name, reason, stackTrace)

            }
        }

        override fun toJson(): JSONObject {
            val json = JSONObject()
            json.putOpt("name", name)
            json.putOpt("reason", reason)
            val stackTraceArray = stackTrace.toJson()
            json.put("stackTrace", stackTraceArray)
            return json
        }
    }
}
