package org.nko.chat.services

import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.nko.chat.models.User

class UserRepository {
    private val auth = FirebaseManager.auth
    private val firestore = FirebaseManager.firestore

    val currentUserFlow: Flow<User?> = auth.authStateChanged.map { firebaseUser ->
        firebaseUser?.let {
            User(
                id = it.uid,
                email = it.email,
                name = it.displayName,
                avatar = it.photoURL
            )
        }
    }

    suspend fun signInWithGoogleCredential(idToken: String, accessToken: String? = null): User? {
        val credential = GoogleAuthProvider.credential(idToken, accessToken)
        val result = auth.signInWithCredential(credential)
        val user = result.user?.let {
            User(
                id = it.uid,
                email = it.email,
                name = it.displayName,
                avatar = it.photoURL
            )
        }

        user?.let { syncUserProfile(it) }
        return user
    }

    suspend fun signOut() {
        auth.signOut()
    }

    private suspend fun syncUserProfile(user: User) {
        user.id?.let { uid ->
            firestore.collection("users").document(uid).set(user, SetOptions.Merge)
        }
    }
    
    suspend fun searchUsers(query: String): List<User> {
        // Simple search by email or name in Firestore
        return firestore.collection("users")
            .where { "email" equalTo query }
            .get()
            .documents
            .map { it.data() }
    }
}
