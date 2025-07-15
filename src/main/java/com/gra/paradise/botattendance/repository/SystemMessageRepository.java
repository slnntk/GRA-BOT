package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.SystemMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemMessageRepository extends JpaRepository<SystemMessage, String> {
    /**
     * Finds a SystemMessage by its guildId.
     *
     * @param guildId The ID of the guild to search for.
     * @return An Optional containing the SystemMessage if found, or empty if not.
     */
    Optional<SystemMessage> findByGuildId(String guildId);
}