package org.nko.chat.models.response

import org.nko.chat.models.Chat
import org.nko.chat.models.User

data class SearchResponse (
    var users : List<User>,
    var chats : List<Chat>
)
