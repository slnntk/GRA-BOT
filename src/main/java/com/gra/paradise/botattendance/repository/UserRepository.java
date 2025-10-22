package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Otimizado para User
 * Inclui consultas otimizadas e cache-friendly
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    // Consultas básicas otimizadas
    Optional<User> findByDiscordId(String discordId);
    List<User> findByUsernameContainingIgnoreCase(String username);
    List<User> findByNicknameContainingIgnoreCase(String nickname);

    // Consultas com paginação
    @Query("SELECT u FROM User u ORDER BY u.username")
    Page<User> findAllUsersPaged(Pageable pageable);

    // Consultas por guild
    @Query("SELECT DISTINCT u FROM User u JOIN u.schedules s WHERE s.guildId = :guildId AND s.active = true")
    List<User> findUsersByActiveGuild(@Param("guildId") String guildId);

    // Consultas de estatísticas
    @Query("SELECT COUNT(u) FROM User u")
    long countTotalUsers();

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.schedules s WHERE s.guildId = :guildId AND s.active = true")
    long countActiveUsersByGuild(@Param("guildId") String guildId);

    // Consultas para busca
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR LOWER(u.nickname) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<User> searchUsers(@Param("searchTerm") String searchTerm);

    // Consultas de limpeza
    @Query("SELECT u FROM User u WHERE u.schedules IS EMPTY")
    List<User> findUsersWithoutSchedules();

    // Consultas de performance
    @Query("SELECT u FROM User u JOIN FETCH u.schedules WHERE u.discordId = :discordId")
    Optional<User> findByDiscordIdWithSchedules(@Param("discordId") String discordId);
}
