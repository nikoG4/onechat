package org.nko.chat.auth

import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun signInWithGoogleFirebase(): String? = suspendCancellableCoroutine { cont ->
    try {
        val fn = window.asDynamic().firebaseSignInWithGoogle
        if (fn == undefined) {
            cont.resume(null)
            return@suspendCancellableCoroutine
        }

        val promise = fn()
        promise.then { token: String? ->
            cont.resume(token)
        }.catch { err: dynamic ->
            cont.resumeWithException(RuntimeException(err?.toString() ?: "Firebase sign-in error"))
        }
    } catch (t: Throwable) {
        cont.resumeWithException(t)
    }
}

