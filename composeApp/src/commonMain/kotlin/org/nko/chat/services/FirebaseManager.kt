package org.nko.chat.services

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

object FirebaseManager {
    val options = FirebaseOptions(
        apiKey = "AIzaSyCYi5t7Imq-AsSqb33cfEJvSEu2wZ4H2A4",
        authDomain = "proyecto-26ba8.firebaseapp.com",
        projectId = "proyecto-26ba8",
        storageBucket = "proyecto-26ba8.firebasestorage.app",
        applicationId = "1:685406200923:web:237ad0c0c2dc0da72c0b7e",
        messagingSenderId = "685406200923"
    )

    val auth by lazy { Firebase.auth }
    val firestore by lazy { Firebase.firestore }
}
