package org.nko.chat.services

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import org.nko.chat.models.User


class ContactService : BaseService() {

    private val apiUrl = "$serverHost/api/user"


    fun getContacts(filter: Boolean, searchQuery: String): List<User> {

        val contacts = user!!.contacts ?: emptyList()

        return when (filter) {
            true -> contacts.filter { it.name!!.contains(searchQuery, ignoreCase = true) }
            false -> contacts
        }
    }

    suspend fun getNonContacts(term: String): Result<List<User>> {

            if (term.isEmpty()) Result.success(emptyList<User>())

            return try {
                val response = apiService.get("$apiUrl/search/${user!!.id}?param=$term")
                if (response.status == HttpStatusCode.OK) {
                    Result.success(response.body())
                } else {
                    Result.failure(Exception("Error al obtener usuarios"))
                }
            }catch (e: Exception) {
                Result.failure(e)
            }


    }
}