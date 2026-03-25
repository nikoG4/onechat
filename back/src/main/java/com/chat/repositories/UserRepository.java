package com.chat.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.chat.models.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String username);
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByPhoneNumberOrEmail(String q, String q2);

    @Query("SELECT u FROM User u " +
        "WHERE u.searchable = true AND " +
        "u.id NOT IN (SELECT c.id FROM User usr JOIN usr.contacts c WHERE usr.id = :userId) AND " +
        "u.id != :userId AND " +
        "(LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
        "LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
        "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) ")
    List<User> findBySearchableAndQuery(@Param("query") String query, @Param("userId") Long userId);


    @Query("SELECT c FROM User u JOIN u.contacts c " +
        "WHERE u.id = :userId AND " +
        "(LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
        "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
        "LOWER(c.email) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) "+
        "ORDER BY c.name ASC")
    List<User> findContactsBySearchTerm(@Param("userId") Long userId, @Param("searchTerm") String searchTerm);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.contacts WHERE u.id = :userId")
    Set<User> findUserWithContacts(@Param("userId") Long userId);

    @Query("SELECT u.contacts FROM User u WHERE u.id = :userId")
    Set<User> findContacts(@Param("userId") Long userId);


}
