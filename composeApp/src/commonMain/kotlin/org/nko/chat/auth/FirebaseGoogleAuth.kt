package org.nko.chat.auth

/**
 * MVP auth para Firebase.
 * - En `wasmJsMain` usa Firebase Auth JS.
 * - En otros targets devuelve `null` (se mantiene el input manual de idToken).
 */
expect suspend fun signInWithGoogleFirebase(): String?

