package com.lkps.ctApp.utils.mapper

import com.lkps.ctApp.data.model.Message
import com.lkps.ctApp.utils.mapper.core.Mapper
import org.json.JSONObject

object RemoteMessageMapper : Mapper<Map<String, String>, Message> {

    override fun map(input: Map<String, String>): Message {
        return Message(
            id = input["id"],
            senderId = input["senderId"],
            receiverId = input["receiverId"],
            isOwner = input["isOwner"].let { if (it.isNullOrEmpty()) false else it.toBoolean() },
            name = input["name"],
            fileUrl = input["fileUrl"],
            audioUrl = input["audioUrl"],
            audioFile = input["audioFile"],
            audioDuration = input["audioDuration"].let { if (it.isNullOrEmpty()) 0 else it.toLong() },
            fileExtension = input["fileExtension"],
            fileName = input["fileName"],
            text = input["text"],
            timestamp = input["timestamp"],
            readTimestamp = input["readTimestamp"],
            pdf = input["pdf"],
            photo = input["photo"],
        )
    }

    fun messageToJson(input: Message?): JSONObject {
        val messageToJson = JSONObject()
        messageToJson.put("id", input?.id ?:"")
        messageToJson.put("senderId", input?.senderId?:"")
        messageToJson.put("receiverId", input?.receiverId?:"")
        messageToJson.put("isOwner", input?.isOwner?:"")
        messageToJson.put("name", input?.name?:"")
        messageToJson.put("photoUrl", input?.fileUrl?:"")
        messageToJson.put("audioUrl", input?.audioUrl?:"")
        messageToJson.put("audioFile", input?.audioFile?:"")
        messageToJson.put("audioDuration", input?.audioDuration?:"")
        messageToJson.put("fileExtension", input?.fileExtension?:"")
        messageToJson.put("fileName", input?.fileName?:"")
        messageToJson.put("text", input?.text?:"")
        messageToJson.put("timestamp", input?.timestamp?:"")
        messageToJson.put("readTimestamp", input?.readTimestamp?: "")
        messageToJson.put("pdf", input?.pdf?: "")
        messageToJson.put("photo", input?.photo?: "")
        return messageToJson
    }
}