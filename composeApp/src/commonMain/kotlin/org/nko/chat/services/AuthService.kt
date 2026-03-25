package org.nko.chat.services


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.nko.chat.models.User
import java.net.HttpURLConnection
import java.net.URL

class AuthService : BaseService() {
    private val apiUrl = "$serverHost/api/user/register"

    suspend fun register(user: User): User? {
        return withContext(Dispatchers.IO) {
            try {

                // Convertir a JSON
                val jsonString = json.encodeToString(User.serializer(), user)

                // Crear la conexión
                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Content-Length", jsonString.length.toString())
                connection.doOutput = true

                // Enviar datos
                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                    outputStream.flush()
                }

                // Leer la respuesta
                val responseCode = connection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val registeredUser = json.decodeFromString(
                        User.serializer(),
                        connection.inputStream.bufferedReader().readText()
                    )

                    println("Usuario registrado exitosamente.")

                    // Guardar el usuario en almacenamiento local
                    saveUserLocally(registeredUser)

                    return@withContext registeredUser
                } else {
                    println("Error: $responseCode - ${connection.responseMessage}")
                }

                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error de conexión: ${e.message}")
            }

            return@withContext null
        }
    }


    fun validateNumber(s: String): Boolean {
        return true
    }

    @Serializable
    private data class GoogleIdTokenExchangeRequest(val idToken: String)

    // MVP: intercambio de ID token de Google contra el backend.
    // Nota: por ahora el cliente debe proveer un `idToken` (pegado manualmente o vía integración futura).
    suspend fun signInWithGoogle(idToken: String): User? {
        return withContext(Dispatchers.IO) {
            try {
                val endpoint = "$serverHost/api/auth/google/exchange"
                val jsonString = json.encodeToString(
                    GoogleIdTokenExchangeRequest.serializer(),
                    GoogleIdTokenExchangeRequest(idToken)
                )

                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                connection.outputStream.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                    outputStream.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val body = connection.inputStream.bufferedReader().readText()
                    val signedUser = json.decodeFromString(User.serializer(), body)
                    saveUserLocally(signedUser)
                    signedUser
                } else {
                    println("Google sign-in failed: $responseCode - ${connection.responseMessage}")
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
