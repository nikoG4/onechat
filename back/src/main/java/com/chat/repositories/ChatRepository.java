package com.chat.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.chat.models.Chat;
//import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

      @Query("SELECT c FROM Chat c " +
            "JOIN c.participants u " +
            "WHERE (:searchTerm IS NULL OR " +
            "      (c.isGroup = false AND " +
            "       (u.name LIKE %:searchTerm% OR u.phoneNumber LIKE %:searchTerm%)))")
      List<Chat> findByNameOrPhoneNumber(@Param("searchTerm") String searchTerm);


      @Query("""
          SELECT c 
          FROM Chat c 
          WHERE c.createdBy.id = :userId 
             OR EXISTS (SELECT 1 FROM c.participants p WHERE p.id = :userId)
            order by c.id desc
      """)
      List<Chat> findChatsByUser(@Param("userId") Long userId);
        


}
