package com.onechat.client

/**
 * Mirrors the public /v1/auth/register validation contract.
 * Password length is measured in UTF-8 bytes, not visual characters.
 */
object SignupValidation {
    private val usernamePattern = Regex("[A-Za-z][A-Za-z0-9_]{2,23}")

    fun validUsername(username: String): Boolean = usernamePattern.matches(username.trim())

    fun passwordBytes(password: String): Int = password.encodeToByteArray().size

    fun validPassword(password: String): Boolean = passwordBytes(password) in 8..72

    fun error(username: String, password: String, invitationCode: String): String? = when {
        !validUsername(username) ->
            "Usuario inválido: usá entre 3 y 24 caracteres; empezá con una letra y usá solo letras, números o _."
        !validPassword(password) ->
            "La contraseña debe tener entre 8 y 72 bytes UTF-8. Usá al menos 8 caracteres para empezar."
        invitationCode.isBlank() ->
            "Ingresá el código de invitación que configuraste en Render."
        else -> null
    }
}
