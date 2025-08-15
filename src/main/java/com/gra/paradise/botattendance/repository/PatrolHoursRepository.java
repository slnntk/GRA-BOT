package com.gra.paradise.botattendance.repository;

import com.gra.paradise.botattendance.model.PatrolHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PatrolHoursRepository extends JpaRepository<PatrolHours, Long> {
    
    List<PatrolHours> findByContestIdAndDiscordIdAndValidTrue(Long contestId, String discordId);
    
    List<PatrolHours> findByContestIdAndDiscordIdAndPatrolDateAndValidTrue(
        Long contestId, String discordId, LocalDate patrolDate);
    
    @Query("SELECT SUM(p.hours) FROM PatrolHours p WHERE p.contest.id = :contestId " +
           "AND p.discordId = :discordId AND p.patrolDate = :date AND p.valid = true")
    Double getTotalHoursForUserOnDate(@Param("contestId") Long contestId, 
                                      @Param("discordId") String discordId,
                                      @Param("date") LocalDate date);
    
    @Query("SELECT SUM(p.afternoonHours) FROM PatrolHours p WHERE p.contest.id = :contestId " +
           "AND p.discordId = :discordId AND p.valid = true")
    Double getTotalAfternoonHoursForUser(@Param("contestId") Long contestId, 
                                         @Param("discordId") String discordId);
    
    @Query("SELECT SUM(p.nightHours) FROM PatrolHours p WHERE p.contest.id = :contestId " +
           "AND p.discordId = :discordId AND p.valid = true")
    Double getTotalNightHoursForUser(@Param("contestId") Long contestId, 
                                     @Param("discordId") String discordId);
}