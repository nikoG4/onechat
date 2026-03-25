package com.chat.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.chat.models.Message;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
   // List<Message> findByChatIdOrderBySentAtAsc(Long chatId);
   @Query("SELECT m FROM Message m WHERE m.chat.id = :chatId ORDER BY m.date ASC")
   List<Message> findByChatId(@Param("chatId") Long chatId);
}