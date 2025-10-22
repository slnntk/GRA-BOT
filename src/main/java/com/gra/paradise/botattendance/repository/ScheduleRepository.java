package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository Otimizado para Schedule
 * Inclui consultas otimizadas, paginação e operações em lote
 */
@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // Consultas básicas otimizadas
    List<Schedule> findByActiveTrue();
    List<Schedule> findByActiveTrueAndGuildId(String guildId);
    long countByActiveTrueAndGuildId(String guildId);
    Optional<Schedule> findByMessageIdAndChannelId(String messageId, String channelId);

    // Consultas com JOIN FETCH para evitar N+1 queries
    @Query("SELECT s FROM Schedule s JOIN FETCH s.crewMembers WHERE s.id = :scheduleId AND s.guildId = :guildId")
    Optional<Schedule> findByIdAndGuildIdWithCrew(@Param("scheduleId") Long scheduleId, @Param("guildId") String guildId);

    @Query("SELECT s FROM Schedule s WHERE s.id = :scheduleId AND s.guildId = :guildId")
    Optional<Schedule> findByIdAndGuildId(@Param("scheduleId") Long scheduleId, @Param("guildId") String guildId);

    // Consultas otimizadas com paginação
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.crewMembers WHERE s.active = true AND s.guildId = :guildId")
    Page<Schedule> findActiveSchedulesWithCrewPaged(@Param("guildId") String guildId, Pageable pageable);

    // Consultas por período otimizadas
    @Query("SELECT s FROM Schedule s WHERE s.endTime IS NOT NULL AND s.endTime < :threshold")
    List<Schedule> findByEndTimeBefore(@Param("threshold") Instant threshold);

    // Consultas por aeronave com cache
    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.crewMembers WHERE s.active = true AND s.guildId = :guildId AND s.aircraftType = :aircraftType")
    List<Schedule> findActiveSchedulesByAircraftType(@Param("guildId") String guildId, @Param("aircraftType") String aircraftType);

    // Consultas por usuário
    @Query("SELECT DISTINCT s FROM Schedule s LEFT JOIN FETCH s.crewMembers WHERE s.active = true AND s.guildId = :guildId AND EXISTS (SELECT 1 FROM s.crewMembers u WHERE u.discordId = :discordId)")
    List<Schedule> findActiveSchedulesByUser(@Param("guildId") String guildId, @Param("discordId") String discordId);

    // Operações em lote para melhor performance
    @Modifying
    @Transactional
    @Query("UPDATE Schedule s SET s.active = false WHERE s.endTime < :threshold")
    int deactivateExpiredSchedules(@Param("threshold") Instant threshold);

    @Modifying
    @Transactional
    @Query("DELETE FROM Schedule s WHERE s.active = false AND s.endTime < :threshold")
    int deleteExpiredSchedules(@Param("threshold") Instant threshold);

    // Consultas de estatísticas
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.active = true AND s.guildId = :guildId AND s.aircraftType = :aircraftType")
    long countActiveSchedulesByAircraftType(@Param("guildId") String guildId, @Param("aircraftType") String aircraftType);

    @Query("SELECT s.aircraftType, COUNT(s) FROM Schedule s WHERE s.active = true AND s.guildId = :guildId GROUP BY s.aircraftType")
    List<Object[]> getAircraftTypeStatistics(@Param("guildId") String guildId);

    // Consultas para limpeza de dados
    @Query("SELECT s FROM Schedule s WHERE s.active = false AND s.endTime < :threshold")
    List<Schedule> findExpiredInactiveSchedules(@Param("threshold") Instant threshold);
}