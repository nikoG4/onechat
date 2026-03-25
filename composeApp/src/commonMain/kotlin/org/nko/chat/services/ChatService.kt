package org.nko.chat.services


import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.nko.chat.models.Chat
import org.nko.chat.models.Message
import org.nko.chat.models.response.MessageResponse

class ChatService : BaseService() {

    private val apiUrl = "$serverHost/api"


    suspend fun getMessages(): Result<List<Chat>> {
        return try {
            val response = apiService.get("$apiUrl/chats/${user!!.id}")
            println("Response status: ${response.status}")

            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error al obtener los mensajes"))
            }
        }catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }

    }

    suspend fun sendMessage(message: Message): Result<Message> {
        return try {

            val messageResponse = MessageResponse(
                id = message.id,
                message = message.message,
                sender = message.user,
                type = message.type,
                media = message.media,
                chat = message.chat,
            )

            val response = apiService.post("$apiUrl/messages") {
                contentType(ContentType.Application.Json)
                setBody(messageResponse)
            }
            if (response.status == HttpStatusCode.OK) {
                val savedMessage: Message = response.body()
                Result.success(savedMessage)
            } else {
                Result.failure(Exception("Error al enviar el mensaje")) // Si hay un error, devolver el error
            }
        } catch (e: Exception) {
            // Manejo de excepciones de red o cualquier otro problema
            e.printStackTrace()
            Result.failure(e) // Retorna el error
        }
    }


}
