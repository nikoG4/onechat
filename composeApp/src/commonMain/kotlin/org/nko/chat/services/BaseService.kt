package org.nko.chat.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.nko.chat.models.User
import org.nko.chat.models.response.SearchResponse
import org.nko.chat.util.getSettings
import java.net.HttpURLConnection
import java.net.URL

open class BaseService {
    protected var settings = getSettings()
    protected var serverHost = settings.getStringOrNull("serverHost")
    protected val json = Json { ignoreUnknownKeys = true }
    protected val user = getUserLocally()

    protected val apiService = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun testServer() : Boolean{
        return withContext(Dispatchers.IO) {
            if (serverHost == null) return@withContext false
            try {
                val url = URL("$serverHost/api/check")
                val connection = url.openConnection() as HttpURLConnection
                return@withContext connection.responseCode == 200
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            }

        }
    }

    suspend fun search(search: String): List<SearchResponse>{
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$serverHost/search?param=$search")
                val connection = url.openConnection() as HttpURLConnection
                return@withContext json.decodeFromString(connection.inputStream.bufferedReader().readText())
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext emptyList()
            }
        }
    }

    fun saveServerHost(host: String){
        settings.putString("serverHost", host)
        serverHost = host
    }

    fun _getServerHost(): String?{
        return settings.getStringOrNull("serverHost")
    }

    fun saveUserLocally(user: User) {
        val userJson = json.encodeToString(User.serializer(), user)
        settings.putString("user", userJson)
        println("Usuario guardado localmente.")
    }

    fun getUserLocally(): User? {
        val userJson = settings.getStringOrNull("user") ?: return null
        return json.decodeFromString(User.serializer(), userJson)
    }


}