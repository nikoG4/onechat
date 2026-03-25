package org.nko.chat.models

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String?,
    val name: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    var avatar: String? = null,
    val searchable: Boolean = false,
    val contacts: List<User>? = null,
    val createdAt: String? = null
)