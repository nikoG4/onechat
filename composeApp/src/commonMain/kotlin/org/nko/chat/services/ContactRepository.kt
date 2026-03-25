package org.nko.chat.services

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nko.chat.models.User

class ContactRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val firestore = FirebaseManager.firestore
    private val contactQueries = DatabaseManager.db.chatDatabaseQueries

    val localContactsFlow: Flow<List<User>> = contactQueries.selectAllContacts().asFlow().mapToList(dispatcher).map { entities ->
        entities.map { entity ->
            User(
                id = entity.id,
                name = entity.name,
                email = entity.email,
                phoneNumber = entity.phoneNumber,
                avatar = entity.avatar,
                createdAt = entity.createdAt
            )
        }
    }

    suspend fun saveContact(user: User) {
        user.id?.let {
            contactQueries.insertContact(
                id = it,
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
                avatar = user.avatar,
                createdAt = user.createdAt
            )
        }
    }

    suspend fun searchAndAddContact(query: String) {
        // Search in Firestore
        val results = firestore.collection("users")
            .where { "email" equalTo query }
            .get()
            .documents
            .map { it.data<User>() }

        results.forEach { saveContact(it) }
    }
}
