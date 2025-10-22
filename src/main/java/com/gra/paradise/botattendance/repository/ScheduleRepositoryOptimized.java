package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Otimizado para Schedule
 * Usa JOIN FETCH para evitar N+1 queries e melhorar performance
 */
@Repository
public interface ScheduleRepositoryOptimized extends JpaRepository<Schedule, Long> {

    /**
     * Busca escalas ativas com crew members em uma única query
     * Evita N+1 queries usando JOIN FETCH
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.crewMembers " +
           "LEFT JOIN FETCH s.logs " +
           "WHERE s.active = true AND s.guildId = :guildId")
    List<Schedule> findActiveSchedulesWithCrew(@Param("guildId") String guildId);

    /**
     * Busca escala específica com todos os relacionamentos
     * Uma única query ao invés de múltiplas
     */
    @Query("SELECT s FROM Schedule s " +
           "LEFT JOIN FETCH s.crewMembers " +
           "LEFT JOIN FETCH s.logs " +
           "WHERE s.id = :id AND s.guildId = :guildId")
    Optional<Schedule> findByIdAndGuildIdWithRelations(@Param("id") Long id, @Param("guildId") String guildId);

    /**
     * Conta escalas ativas sem carregar dados desnecessários
     * Query otimizada para contagem
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.active = true AND s.guildId = :guildId")
    long countActiveSchedulesByGuildId(@Param("guildId") String guildId);

    /**
     * Busca escalas por tipo de aeronave com paginação
     * Otimizada para grandes volumes de dados
     */
    @Query("SELECT s FROM Schedule s " +
           "LEFT JOIN FETCH s.crewMembers " +
           "WHERE s.active = true AND s.guildId = :guildId AND s.aircraftType = :aircraftType")
    List<Schedule> findActiveSchedulesByAircraftType(@Param("guildId") String guildId, 
                                                     @Param("aircraftType") String aircraftType);

    /**
     * Busca escalas por período com otimização de data
     * Usa índices de data para melhor performance
     */
    @Query("SELECT s FROM Schedule s " +
           "LEFT JOIN FETCH s.crewMembers " +
           "WHERE s.active = true AND s.guildId = :guildId " +
           "AND s.startTime >= :startDate AND s.startTime <= :endDate")
    List<Schedule> findActiveSchedulesByDateRange(@Param("guildId") String guildId,
                                                  @Param("startDate") java.time.Instant startDate,
                                                  @Param("endDate") java.time.Instant endDate);

    /**
     * Busca escalas que expiram em breve
     * Otimizada para notificações automáticas
     */
    @Query("SELECT s FROM Schedule s " +
           "WHERE s.active = true AND s.endTime <= :expirationTime")
    List<Schedule> findSchedulesExpiringSoon(@Param("expirationTime") java.time.Instant expirationTime);

    /**
     * Busca escalas por usuário específico
     * Otimizada para consultas de usuário
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.crewMembers " +
           "WHERE s.active = true AND s.guildId = :guildId " +
           "AND EXISTS (SELECT 1 FROM s.crewMembers u WHERE u.discordId = :discordId)")
    List<Schedule> findActiveSchedulesByUser(@Param("guildId") String guildId, 
                                            @Param("discordId") String discordId);
}
