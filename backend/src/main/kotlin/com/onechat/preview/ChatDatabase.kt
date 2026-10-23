package com.onechat.preview

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.Locale
import java.util.UUID

class ChatDatabase(config: PreviewConfig) : AutoCloseable {
    private val ds = HikariDataSource(HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.dbUser
        password = config.dbPassword
        maximumPoolSize = 5
        minimumIdle = 1
        connectionTimeout = 10_000
        poolName = "onechat-preview"
    })

    override fun close() = ds.close()

    fun ready(): Boolean = ds.connection.use { c ->
        c.prepareStatement("SELECT 1").use { p ->
            p.executeQuery().use { rs -> rs.next() && rs.getInt(1) == 1 }
        }
    }

    fun migrate() = ds.connection.use { c ->
        c.createStatement().use { s ->
            s.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS preview_users (
                    id VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(24) NOT NULL,
                    username_key VARCHAR(24) NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    created_at BIGINT NOT NULL
                )
                """.trimIndent()
            )
            s.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS preview_conversations (
                    id VARCHAR(36) PRIMARY KEY,
                    direct_pair VARCHAR(80) NOT NULL UNIQUE,
                    created_at BIGINT NOT NULL
                )
                """.trimIndent()
            )
            s.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS preview_members (
                    conversation_id VARCHAR(36) NOT NULL REFERENCES preview_conversations(id),
                    user_id VARCHAR(36) NOT NULL REFERENCES preview_users(id),
                    PRIMARY KEY (conversation_id, user_id)
                )
                """.trimIndent()
            )
            s.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS preview_messages (
                    id VARCHAR(36) PRIMARY KEY,
                    conversation_id VARCHAR(36) NOT NULL REFERENCES preview_conversations(id),
                    sender_id VARCHAR(36) NOT NULL REFERENCES preview_users(id),
                    client_message_id VARCHAR(80) NOT NULL,
                    body VARCHAR(4000) NOT NULL,
                    created_at BIGINT NOT NULL,
                    UNIQUE (sender_id, client_message_id)
                )
                """.trimIndent()
            )
            s.executeUpdate("ALTER TABLE preview_messages ADD COLUMN IF NOT EXISTS read_at BIGINT")
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_preview_member ON preview_members(user_id)")
            s.executeUpdate(
                "CREATE INDEX IF NOT EXISTS idx_preview_timeline ON preview_messages(conversation_id, created_at DESC, id DESC)"
            )
        }
    }

    private fun <T> transaction(block: (Connection) -> T): T = ds.connection.use { c ->
        c.autoCommit = false
        try {
            val v = block(c)
            c.commit()
            v
        } catch (e: Exception) {
            c.rollback()
            throw e
        }
    }

    data class UserWithHash(val user: UserDto, val hash: String)

    fun register(username: String, hash: String): UserDto? {
        val user = UserDto(UUID.randomUUID().toString(), username)
        return try {
            ds.connection.use { c ->
                c.prepareStatement(
                    "INSERT INTO preview_users(id,username,username_key,password_hash,created_at) VALUES (?,?,?,?,?)"
                ).use { p ->
                    p.setString(1, user.id)
                    p.setString(2, username)
                    p.setString(3, username.lowercase(Locale.ROOT))
                    p.setString(4, hash)
                    p.setLong(5, System.currentTimeMillis())
                    p.executeUpdate()
                }
            }
            user
        } catch (e: SQLException) {
            if (e.sqlState == "23505") null else throw e
        }
    }

    fun findWithHash(username: String): UserWithHash? = ds.connection.use { c ->
        c.prepareStatement("SELECT id,username,password_hash FROM preview_users WHERE username_key=?").use { p ->
            p.setString(1, username.lowercase(Locale.ROOT))
            p.executeQuery().use { rs ->
                if (rs.next()) UserWithHash(UserDto(rs.getString(1), rs.getString(2)), rs.getString(3))
                else null
            }
        }
    }

    fun findUser(id: String): UserDto? = ds.connection.use { c ->
        c.prepareStatement("SELECT id,username FROM preview_users WHERE id=?").use { p ->
            p.setString(1, id)
            p.executeQuery().use { rs ->
                if (rs.next()) UserDto(rs.getString(1), rs.getString(2)) else null
            }
        }
    }

    fun findByName(username: String): UserDto? = ds.connection.use { c ->
        c.prepareStatement("SELECT id,username FROM preview_users WHERE username_key=?").use { p ->
            p.setString(1, username.lowercase(Locale.ROOT))
            p.executeQuery().use { rs ->
                if (rs.next()) UserDto(rs.getString(1), rs.getString(2)) else null
            }
        }
    }

    fun findUsers(query: String, excluding: String): List<UserDto> = ds.connection.use { c ->
        c.prepareStatement(
            "SELECT id,username FROM preview_users WHERE id<>? AND username_key LIKE ? ORDER BY username_key LIMIT 20"
        ).use { p ->
            p.setString(1, excluding)
            p.setString(2, "%" + query.lowercase(Locale.ROOT) + "%")
            p.executeQuery().use { rs ->
                buildList { while (rs.next()) add(UserDto(rs.getString(1), rs.getString(2))) }
            }
        }
    }

    fun directConversation(userA: String, userB: String): String {
        require(userA != userB)
        val pair = listOf(userA, userB).sorted().joinToString(":")
        return transaction { c ->
            val candidateId = UUID.randomUUID().toString()
            c.prepareStatement(
                "INSERT INTO preview_conversations(id,direct_pair,created_at) VALUES (?,?,?) ON CONFLICT (direct_pair) DO NOTHING"
            ).use { p ->
                p.setString(1, candidateId)
                p.setString(2, pair)
                p.setLong(3, System.currentTimeMillis())
                p.executeUpdate()
            }
            val realId = c.prepareStatement(
                "SELECT id FROM preview_conversations WHERE direct_pair=?"
            ).use { p ->
                p.setString(1, pair)
                p.executeQuery().use { rs -> check(rs.next()); rs.getString(1) }
            }
            for (user in listOf(userA, userB)) {
                c.prepareStatement(
                    "INSERT INTO preview_members(conversation_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING"
                ).use { p ->
                    p.setString(1, realId)
                    p.setString(2, user)
                    p.executeUpdate()
                }
            }
            realId
        }
    }

    fun members(conversationId: String): List<String> = ds.connection.use { c ->
        c.prepareStatement("SELECT user_id FROM preview_members WHERE conversation_id=?").use { p ->
            p.setString(1, conversationId)
            p.executeQuery().use { rs ->
                buildList { while (rs.next()) add(rs.getString(1)) }
            }
        }
    }

    fun isMember(userId: String, conversationId: String): Boolean = ds.connection.use { c ->
        c.prepareStatement(
            "SELECT 1 FROM preview_members WHERE user_id=? AND conversation_id=?"
        ).use { p ->
            p.setString(1, userId)
            p.setString(2, conversationId)
            p.executeQuery().use { rs -> rs.next() }
        }
    }

    fun conversations(userId: String): List<ConversationDto> = ds.connection.use { c ->
        c.prepareStatement(
            """
            SELECT c.id, u.id, u.username, c.created_at,
              (SELECT m.body FROM preview_messages m WHERE m.conversation_id=c.id
               ORDER BY m.created_at DESC, m.id DESC LIMIT 1) AS preview,
              (SELECT COUNT(*) FROM preview_messages unread
               WHERE unread.conversation_id=c.id AND unread.sender_id<>?
                 AND unread.read_at IS NULL) AS unread
            FROM preview_conversations c
            JOIN preview_members mine ON mine.conversation_id=c.id AND mine.user_id=?
            JOIN preview_members theirs ON theirs.conversation_id=c.id AND theirs.user_id<>?
            JOIN preview_users u ON u.id=theirs.user_id
            ORDER BY COALESCE(
              (SELECT MAX(m.created_at) FROM preview_messages m WHERE m.conversation_id=c.id),
              c.created_at
            ) DESC
            """.trimIndent()
        ).use { p ->
            p.setString(1, userId)
            p.setString(2, userId)
            p.setString(3, userId)
            p.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(ConversationDto(
                        id = rs.getString(1),
                        peer = UserDto(rs.getString(2), rs.getString(3)),
                        createdAt = rs.getLong(4),
                        preview = rs.getString(5),
                        unread = rs.getInt(6),
                    ))
                }
            }
        }
    }

    private fun readMessage(rs: ResultSet) = MessageDto(
        id = rs.getString(1),
        conversationId = rs.getString(2),
        senderId = rs.getString(3),
        clientMessageId = rs.getString(4),
        body = rs.getString(5),
        createdAt = rs.getLong(6),
        readAt = rs.getLong(7).let { if (rs.wasNull()) null else it },
    )

    data class StoredMessage(val message: MessageDto, val created: Boolean)
    class DuplicateIdConflict : RuntimeException("clientMessageId already belongs to another message")

    fun send(conversationId: String, senderId: String, clientMessageId: String, body: String): StoredMessage =
        transaction { c ->
            val newId = UUID.randomUUID().toString()
            val inserted = c.prepareStatement(
                """
                INSERT INTO preview_messages(id,conversation_id,sender_id,client_message_id,body,created_at)
                VALUES (?,?,?,?,?,?) ON CONFLICT (sender_id,client_message_id) DO NOTHING
                """.trimIndent()
            ).use { p ->
                p.setString(1, newId)
                p.setString(2, conversationId)
                p.setString(3, senderId)
                p.setString(4, clientMessageId)
                p.setString(5, body)
                p.setLong(6, System.currentTimeMillis())
                p.executeUpdate() == 1
            }
            val item = c.prepareStatement(
                """
                SELECT id,conversation_id,sender_id,client_message_id,body,created_at,read_at
                FROM preview_messages WHERE sender_id=? AND client_message_id=?
                """.trimIndent()
            ).use { p ->
                p.setString(1, senderId)
                p.setString(2, clientMessageId)
                p.executeQuery().use { rs ->
                    check(rs.next()) { "Idempotent message disappeared" }
                    readMessage(rs)
                }
            }
            if (item.conversationId != conversationId || item.body != body) throw DuplicateIdConflict()
            StoredMessage(item, inserted)
        }

    fun history(conversationId: String, limit: Int, beforeId: String?): List<MessageDto> = ds.connection.use { c ->
        val cutoff: Pair<Long, String>? = if (beforeId == null) null else {
            c.prepareStatement(
                "SELECT created_at,id FROM preview_messages WHERE conversation_id=? AND id=?"
            ).use { p ->
                p.setString(1, conversationId)
                p.setString(2, beforeId)
                p.executeQuery().use { rs -> if (rs.next()) rs.getLong(1) to rs.getString(2) else null }
            } ?: return@use emptyList()
        }
        val sql = if (cutoff == null)
            """
            SELECT id,conversation_id,sender_id,client_message_id,body,created_at,read_at
            FROM preview_messages WHERE conversation_id=?
            ORDER BY created_at DESC,id DESC LIMIT ?
            """.trimIndent()
        else
            """
            SELECT id,conversation_id,sender_id,client_message_id,body,created_at,read_at
            FROM preview_messages
            WHERE conversation_id=? AND (created_at < ? OR (created_at = ? AND id < ?))
            ORDER BY created_at DESC,id DESC LIMIT ?
            """.trimIndent()
        c.prepareStatement(sql).use { p ->
            p.setString(1, conversationId)
            if (cutoff == null) p.setInt(2, limit)
            else {
                p.setLong(2, cutoff.first)
                p.setLong(3, cutoff.first)
                p.setString(4, cutoff.second)
                p.setInt(5, limit)
            }
            p.executeQuery().use { rs ->
                buildList { while (rs.next()) add(readMessage(rs)) }.reversed()
            }
        }
    }
    /**
     * Marks only messages authored by the OTHER direct-chat member as read.
     * Called only after authentication and conversation membership validation.
     * A second identical call changes nothing and does not emit a duplicate event.
     */
    fun markRead(conversationId: String, readerId: String): ReadReceiptDto? = ds.connection.use { c ->
        val now = System.currentTimeMillis()
        c.prepareStatement(
            "UPDATE preview_messages SET read_at=? WHERE conversation_id=? AND sender_id<>? " +
                "AND read_at IS NULL RETURNING id"
        ).use { p ->
            p.setLong(1, now)
            p.setString(2, conversationId)
            p.setString(3, readerId)
            p.executeQuery().use { rs ->
                if (rs.next()) ReadReceiptDto(conversationId, readerId, now) else null
            }
        }
    }

}
